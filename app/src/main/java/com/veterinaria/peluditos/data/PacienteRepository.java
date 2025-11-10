package com.veterinaria.peluditos.data;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PacienteRepository {

    private static final String TAG = "PacienteRepository";
    private static final String COLLECTION = "pacientes";

    private final PacienteDao pacienteDao;
    private final LiveData<List<Paciente>> allPacientes;
    private final FirebaseFirestore firestore;

    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private boolean isOnline = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ListenerRegistration firestoreListener;

    public PacienteRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        pacienteDao = db.pacienteDao();
        allPacientes = pacienteDao.getAllPacientes();
        firestore = FirebaseFirestore.getInstance();
        setupNetworkCallback(application);
        checkInitialNetworkState();
    }

    public LiveData<List<Paciente>> getAllPacientes() {
        return allPacientes;
    }

    public LiveData<Paciente> getPaciente(String id) {
        return pacienteDao.getPacienteLive(id);
    }

    public void insert(Paciente paciente) {
        long now = System.currentTimeMillis();
        paciente.setTimestampModificacion(now);
        paciente.setEliminado(false);

        if (!isOnline || isSyncing.get()) {
            paciente.setSincronizado(false);
        } else {
            sincronizarConFirestore(paciente, success -> {
                if (!success) {
                    marcarComoPendiente(paciente);
                }
            });
        }

        AppDatabase.databaseWriteExecutor.execute(() -> pacienteDao.insert(paciente));
    }

    public void update(Paciente paciente) {
        long now = System.currentTimeMillis();
        paciente.setTimestampModificacion(now);
        paciente.setEliminado(false);

        if (!isOnline || isSyncing.get()) {
            paciente.setSincronizado(false);
        } else {
            sincronizarConFirestore(paciente, success -> {
                if (!success) {
                    marcarComoPendiente(paciente);
                }
            });
        }

        AppDatabase.databaseWriteExecutor.execute(() -> pacienteDao.insert(paciente));
    }

    public void delete(Paciente paciente) {
        if (paciente == null || paciente.getId() == null) {
            return;
        }

        if (isOnline && !isSyncing.get()) {
            firestore.collection(COLLECTION)
                    .document(paciente.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Paciente eliminado en Firestore: " + paciente.getId());
                        deleteLocal(paciente.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al eliminar paciente en Firestore", e);
                        marcarEliminadoLocal(paciente);
                    });
        } else {
            marcarEliminadoLocal(paciente);
        }
    }

    private void marcarEliminadoLocal(Paciente paciente) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            paciente.setEliminado(true);
            paciente.setSincronizado(false);
            paciente.setTimestampModificacion(System.currentTimeMillis());
            pacienteDao.insert(paciente);
        });
    }

    private void marcarComoPendiente(Paciente paciente) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            paciente.setSincronizado(false);
            pacienteDao.insert(paciente);
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
                Log.d(TAG, "Conexión disponible - sincronizando pacientes");
                sincronizarCambiosOffline();
                startFirestoreListener();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                isOnline = false;
                Log.d(TAG, "Conexión perdida - modo offline para pacientes");
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
            List<Paciente> pendientes = pacienteDao.getPacientesPendientes();
            if (pendientes.isEmpty()) {
                isSyncing.set(false);
                startFirestoreListener();
                return;
            }

            final int total = pendientes.size();
            final int[] completados = {0};

            for (Paciente paciente : pendientes) {
                sincronizarConFirestore(paciente, success -> {
                    if (success) {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                pacienteDao.actualizarSincronizacion(paciente.getId(), true));
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
        });
    }

    private void sincronizarConFirestore(Paciente paciente, SyncCallback callback) {
        if (paciente.isEliminado()) {
            firestore.collection(COLLECTION)
                    .document(paciente.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Paciente eliminado (sync): " + paciente.getId());
                        deleteLocal(paciente.getId());
                        callback.onComplete(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al eliminar paciente durante sincronización", e);
                        callback.onComplete(false);
                    });
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nombre", paciente.getNombre());
        data.put("especie", paciente.getEspecie());
        data.put("raza", paciente.getRaza());
        data.put("edad", paciente.getEdad());
        data.put("peso", paciente.getPeso());
        data.put("sexo", paciente.getSexo());
        data.put("clienteId", paciente.getClienteId());
        data.put("clienteNombre", paciente.getClienteNombre());
        data.put("timestampModificacion", paciente.getTimestampModificacion());
        data.put("eliminado", false);

        firestore.collection(COLLECTION)
                .document(paciente.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Paciente sincronizado: " + paciente.getId());
                    AppDatabase.databaseWriteExecutor.execute(() ->
                            pacienteDao.actualizarSincronizacion(paciente.getId(), true));
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al sincronizar paciente", e);
                    callback.onComplete(false);
                });
    }

    private void deleteLocal(String pacienteId) {
        AppDatabase.databaseWriteExecutor.execute(() -> pacienteDao.deleteById(pacienteId));
    }

    private void startFirestoreListener() {
        if (!isOnline || firestoreListener != null || isSyncing.get()) {
            return;
        }

        firestoreListener = firestore.collection(COLLECTION)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        if (error != null) {
                            Log.e(TAG, "Error escuchando pacientes", error);
                        }
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (QueryDocumentSnapshot document : snapshots) {
                            try {
                                Paciente remoto = createPacienteFromDocument(document);
                                Paciente local = pacienteDao.getPaciente(remoto.getId());

                                boolean actualizar = true;
                                if (remoto.isEliminado()) {
                                    pacienteDao.deleteById(remoto.getId());
                                    continue;
                                }

                                if (local != null) {
                                    if (!local.isSincronizado()) {
                                        actualizar = false;
                                    } else if (local.getTimestampModificacion() > remoto.getTimestampModificacion()) {
                                        actualizar = false;
                                    }
                                }

                                if (actualizar) {
                                    remoto.setSincronizado(true);
                                    pacienteDao.insert(remoto);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando paciente remoto", e);
                            }
                        }
                    });
                });
    }

    private void stopFirestoreListener() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private Paciente createPacienteFromDocument(QueryDocumentSnapshot document) {
        String id = document.getId();
        String nombre = document.getString("nombre");
        String especie = document.getString("especie");
        String raza = document.getString("raza");
        Long edadLong = document.getLong("edad");
        Long timestamp = document.getLong("timestampModificacion");
        Double pesoDouble = document.getDouble("peso");
        String sexo = document.getString("sexo");
        String clienteId = document.getString("clienteId");
        String clienteNombre = document.getString("clienteNombre");
        Boolean eliminado = document.getBoolean("eliminado");

        int edad = edadLong != null ? edadLong.intValue() : 0;
        double peso = pesoDouble != null ? pesoDouble : 0d;

        Paciente paciente = new Paciente(
                id,
                nombre != null ? nombre : "",
                especie != null ? especie : "",
                raza != null ? raza : "",
                edad,
                peso,
                sexo != null ? sexo : "",
                clienteId != null ? clienteId : "",
                clienteNombre != null ? clienteNombre : ""
        );
        paciente.setSincronizado(true);
        if (timestamp != null) {
            paciente.setTimestampModificacion(timestamp);
        }
        if (eliminado != null) {
            paciente.setEliminado(eliminado);
        }
        return paciente;
    }

    public void cleanup() {
        stopFirestoreListener();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "Error al desregistrar callback de red", e);
            }
        }
    }

    private interface SyncCallback {
        void onComplete(boolean success);
    }
}
