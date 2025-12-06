package com.veterinaria.peluditos.data;

import android.app.Application;
import android.content.Context;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CitaRepository {

    private static final String TAG = "CitaRepository";
    private static final String COLLECTION = "citas";
    private static final String ESTADO_DEFAULT = "Pendiente";

    private final CitaDao citaDao;
    private final LiveData<List<Cita>> allCitas;
    private final FirebaseFirestore firestore;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private boolean isOnline = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ListenerRegistration firestoreListener;

    public CitaRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        citaDao = db.citaDao();
        allCitas = citaDao.getAllCitas();
        firestore = FirebaseFirestore.getInstance();
        setupNetworkCallback(application);
        checkInitialNetworkState();
    }

    public LiveData<List<Cita>> getAllCitas() {
        return allCitas;
    }

    public LiveData<Cita> getCita(String id) {
        return citaDao.getCita(id);
    }

    public void insert(Cita cita) {
        cita.setTimestampModificacion(System.currentTimeMillis());
        cita.setPendienteEliminacion(false);
        if (TextUtils.isEmpty(cita.getEstado())) {
            cita.setEstado(ESTADO_DEFAULT);
        }
        if (cita.getNotaEstado() == null) {
            cita.setNotaEstado("");
        }
        if (!isOnline || isSyncing.get()) {
            cita.setSincronizado(false);
        } else {
            sincronizarConFirestore(cita, success -> {
                if (!success) {
                    markAsPending(cita);
                }
            });
        }

        AppDatabase.databaseWriteExecutor.execute(() -> citaDao.insert(cita));
    }

    public void update(Cita cita) {
        cita.setTimestampModificacion(System.currentTimeMillis());
        cita.setPendienteEliminacion(false);
        if (TextUtils.isEmpty(cita.getEstado())) {
            cita.setEstado(ESTADO_DEFAULT);
        }
        if (cita.getNotaEstado() == null) {
            cita.setNotaEstado("");
        }
        if (!isOnline || isSyncing.get()) {
            cita.setSincronizado(false);
        } else {
            sincronizarConFirestore(cita, success -> {
                if (!success) {
                    markAsPending(cita);
                }
            });
        }

        AppDatabase.databaseWriteExecutor.execute(() -> citaDao.insert(cita));
    }

    private void markAsPending(Cita cita) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            cita.setSincronizado(false);
            citaDao.insert(cita);
        });
    }

    private void setupNetworkCallback(Application application) {
        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                isOnline = true;
                sincronizarCambiosOffline();
                startFirestoreListener();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                isOnline = false;
                stopFirestoreListener();
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private void checkInitialNetworkState() {
        if (connectivityManager == null) {
            return;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        isOnline = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if (isOnline) {
            startFirestoreListener();
        }
    }

    private void sincronizarCambiosOffline() {
        if (!isOnline || isSyncing.get()) {
            return;
        }

        isSyncing.set(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Cita> pendientes = citaDao.getPendientes();
            if (pendientes.isEmpty()) {
                isSyncing.set(false);
                startFirestoreListener();
                return;
            }

            final int total = pendientes.size();
            final int[] completados = {0};

            for (Cita cita : pendientes) {
                if (cita.isPendienteEliminacion()) {
                    eliminarEnFirestore(cita, success -> {
                        if (success) {
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    citaDao.deleteById(cita.getId()));
                        }
                        synchronized (completados) {
                            completados[0]++;
                            if (completados[0] >= total) {
                                isSyncing.set(false);
                                startFirestoreListener();
                            }
                        }
                    });
                } else {
                    sincronizarConFirestore(cita, success -> {
                        if (success) {
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    citaDao.actualizarSincronizacion(cita.getId(), true));
                        }
                        synchronized (completados) {
                            completados[0]++;
                            if (completados[0] >= total) {
                                isSyncing.set(false);
                                startFirestoreListener();
                            }
                        }
                    });
                }
            }
        });
    }

    public void updateEstado(Cita cita, String nuevoEstado) {
        if (cita == null || TextUtils.isEmpty(nuevoEstado)) return;
        cita.setEstado(nuevoEstado);
        update(cita);
    }

    public void delete(Cita cita) {
        if (cita == null || TextUtils.isEmpty(cita.getId())) {
            return;
        }

        cita.setPendienteEliminacion(true);
        cita.setSincronizado(false);
        cita.setTimestampModificacion(System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> citaDao.insert(cita));

        if (isOnline && !isSyncing.get()) {
            eliminarEnFirestore(cita, success -> {
                if (success) {
                    AppDatabase.databaseWriteExecutor.execute(() ->
                            citaDao.deleteById(cita.getId()));
                }
            });
        } else {
            Log.w(TAG, "Eliminación offline: se sincronizará cuando vuelva la conexión");
        }
    }

    private void sincronizarConFirestore(Cita cita, SyncCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("pacienteId", cita.getPacienteId());
        data.put("pacienteNombre", cita.getPacienteNombre());
        data.put("clienteId", cita.getClienteId());
        data.put("clienteNombre", cita.getClienteNombre());
        data.put("fecha", cita.getFecha());
        data.put("hora", cita.getHora());
        data.put("motivo", cita.getMotivo());
        data.put("notas", cita.getNotas());
        data.put("fechaHoraTimestamp", cita.getFechaHoraTimestamp());
        data.put("timestampModificacion", cita.getTimestampModificacion());
        data.put("estado", cita.getEstado());
        data.put("notaEstado", cita.getNotaEstado());
        data.put("pendienteEliminacion", cita.isPendienteEliminacion());

        firestore.collection(COLLECTION)
                .document(cita.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cita sincronizada: " + cita.getId());
                    AppDatabase.databaseWriteExecutor.execute(() ->
                            citaDao.actualizarSincronizacion(cita.getId(), true));
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al sincronizar cita", e);
                    callback.onComplete(false);
                });
    }

    private void startFirestoreListener() {
        if (!isOnline || firestoreListener != null || isSyncing.get()) {
            return;
        }

        firestoreListener = firestore.collection(COLLECTION)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        if (error != null) {
                            Log.e(TAG, "Error escuchando citas", error);
                        }
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            try {
                                switch (change.getType()) {
                                    case REMOVED:
                                        citaDao.deleteById(change.getDocument().getId());
                                        break;
                                    case ADDED:
                                    case MODIFIED:
                                        Cita citaRemota = createCitaFromDocument(change.getDocument());
                                        if (citaRemota.isPendienteEliminacion()) {
                                            citaDao.deleteById(citaRemota.getId());
                                        } else {
                                            citaRemota.setSincronizado(true);
                                            citaRemota.setPendienteEliminacion(false);
                                            citaDao.insert(citaRemota);
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando cita remota", e);
                            }
                        }
                    });
                });
    }

    private Cita createCitaFromDocument(QueryDocumentSnapshot document) {
        String id = document.getId();
        String pacienteId = document.getString("pacienteId");
        String pacienteNombre = document.getString("pacienteNombre");
        String clienteId = document.getString("clienteId");
        String clienteNombre = document.getString("clienteNombre");
        String fecha = document.getString("fecha");
        String hora = document.getString("hora");
        String motivo = document.getString("motivo");
        String notas = document.getString("notas");
        Long fechaHoraTimestamp = document.getLong("fechaHoraTimestamp");
        Long timestampModificacion = document.getLong("timestampModificacion");
        String estado = document.getString("estado");
        String notaEstado = document.getString("notaEstado");
        Boolean pendienteEliminacion = document.getBoolean("pendienteEliminacion");

        Cita cita = new Cita(
                id,
                pacienteId != null ? pacienteId : "",
                pacienteNombre != null ? pacienteNombre : "",
                clienteId != null ? clienteId : "",
                clienteNombre != null ? clienteNombre : "",
                fecha != null ? fecha : "",
                hora != null ? hora : "",
                motivo != null ? motivo : "",
                notas != null ? notas : "",
                fechaHoraTimestamp != null ? fechaHoraTimestamp : System.currentTimeMillis(),
                TextUtils.isEmpty(estado) ? ESTADO_DEFAULT : estado,
                notaEstado != null ? notaEstado : ""
        );
        if (timestampModificacion != null) {
            cita.setTimestampModificacion(timestampModificacion);
        }
        cita.setSincronizado(true);
        cita.setPendienteEliminacion(pendienteEliminacion != null && pendienteEliminacion);
        return cita;
    }

    private void stopFirestoreListener() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    public void cleanup() {
        stopFirestoreListener();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "Error al desregistrar network callback", e);
            }
        }
    }

    private interface SyncCallback {
        void onComplete(boolean success);
    }

    private void eliminarEnFirestore(Cita cita, SyncCallback callback) {
        firestore.collection(COLLECTION)
                .document(cita.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cita eliminada en Firestore: " + cita.getId());
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando cita en Firestore", e);
                    callback.onComplete(false);
                });
    }
}
