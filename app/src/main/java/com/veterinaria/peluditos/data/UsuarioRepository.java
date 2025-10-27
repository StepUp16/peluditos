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
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class UsuarioRepository {
    private static final String TAG = "UsuarioRepository";

    private UsuarioDao usuarioDao;
    private FirebaseFirestore firestore;
    private LiveData<List<Usuario>> allUsuarios;
    private boolean isOnline = false;
    private AtomicBoolean isSyncing = new AtomicBoolean(false);
    private ListenerRegistration firestoreListener;
    private ConnectivityManager.NetworkCallback networkCallback;

    public UsuarioRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        usuarioDao = db.usuarioDao();
        firestore = FirebaseFirestore.getInstance();
        allUsuarios = usuarioDao.getAllUsuarios();
        setupNetworkCallback(application);
    }

    private void setupNetworkCallback(Application application) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
            application.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    isOnline = true;
                    Log.d(TAG, "Conexión disponible - iniciando sincronización");
                    sincronizarCambiosOffline();
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    isOnline = false;
                    Log.d(TAG, "Conexión perdida - modo offline");
                    // Detener el listener de Firestore cuando se pierde la conexión
                    stopFirestoreListener();
                }
            };

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    /**
     * Sincroniza los cambios offline con Firestore antes de activar los listeners remotos
     */
    private void sincronizarCambiosOffline() {
        if (isSyncing.get()) {
            Log.d(TAG, "Sincronización ya en proceso");
            return;
        }

        isSyncing.set(true);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Limpiar usuarios locales duplicados antes de sincronizar
                usuarioDao.limpiarUsuariosLocalesDuplicados();

                List<Usuario> usuariosLocales = usuarioDao.getUsuariosLocales();
                Log.d(TAG, "Usuarios pendientes de sincronización: " + usuariosLocales.size());

                if (usuariosLocales.isEmpty()) {
                    // Si no hay cambios pendientes, activar listener de Firestore con retraso
                    Log.d(TAG, "No hay cambios pendientes - activando listener de Firestore con retraso");
                    isSyncing.set(false);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        startFirestoreListener();
                    }, 2000); // 2 segundos de retraso
                    return;
                }

                // Sincronizar cada usuario pendiente
                final int[] pendingSync = {usuariosLocales.size()};

                for (Usuario usuario : usuariosLocales) {
                    sincronizarUsuarioConFirestore(usuario, success -> {
                        synchronized (pendingSync) {
                            pendingSync[0]--;
                            if (pendingSync[0] == 0) {
                                // Todos los usuarios han sido sincronizados
                                Log.d(TAG, "Sincronización completada - activando listener de Firestore con retraso");
                                isSyncing.set(false);
                                // Añadir un pequeño retraso para evitar interferir con pantallas de edición
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    startFirestoreListener();
                                }, 2000); // 2 segundos de retraso
                            }
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error durante la sincronización", e);
                isSyncing.set(false);
            }
        });
    }

    private void sincronizarUsuarioConFirestore(Usuario usuario, SyncCallback callback) {
        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("nombre", usuario.getNombre());
        usuarioMap.put("apellido", usuario.getApellido());
        usuarioMap.put("email", usuario.getEmail());
        usuarioMap.put("telefono", usuario.getTelefono());
        usuarioMap.put("dui", usuario.getDui());
        usuarioMap.put("direccion", usuario.getDireccion());
        usuarioMap.put("rol", usuario.getRol());
        usuarioMap.put("timestampModificacion", usuario.getTimestampModificacion());

        String documentId = usuario.getUid().startsWith("local_") ?
            usuario.getDui().replace("-", "").trim() : usuario.getUid();

        firestore.collection("usuarios")
                .document(documentId)
                .set(usuarioMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario sincronizado exitosamente: " + usuario.getUid());
                    // Actualizar en la base de datos local
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            if (usuario.getUid().startsWith("local_")) {
                                // Verificar si ya existe un usuario con el UID definitivo
                                Usuario usuarioExistente = usuarioDao.getUsuarioSincrono(documentId);
                                if (usuarioExistente != null) {
                                    // Si ya existe, eliminar el usuario local y actualizar el existente
                                    usuarioDao.deleteUsuario(usuario.getUid());
                                    usuarioExistente.setNombre(usuario.getNombre());
                                    usuarioExistente.setApellido(usuario.getApellido());
                                    usuarioExistente.setEmail(usuario.getEmail());
                                    usuarioExistente.setTelefono(usuario.getTelefono());
                                    usuarioExistente.setDireccion(usuario.getDireccion());
                                    usuarioExistente.setRol(usuario.getRol());
                                    usuarioExistente.setTimestampModificacion(usuario.getTimestampModificacion());
                                    usuarioExistente.setSincronizado(true);
                                    usuarioDao.insert(usuarioExistente);
                                } else {
                                    // Si no existe, actualizar el UID local con el definitivo
                                    usuarioDao.actualizarUid(usuario.getUid(), documentId);
                                }
                            }
                            // Marcar como sincronizado
                            usuarioDao.actualizarSincronizacion(documentId, true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error al actualizar usuario local después de sincronización", e);
                        }
                    });
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al sincronizar usuario: " + usuario.getUid(), e);
                    callback.onComplete(false);
                });
    }

    private void startFirestoreListener() {
        if (firestoreListener != null) {
            return; // Ya está escuchando
        }

        Log.d(TAG, "Iniciando listener de Firestore");
        firestoreListener = firestore.collection("usuarios")
                .whereEqualTo("rol", "cliente")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error en el listener de Firestore", error);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !isSyncing.get()) {
                        Log.d(TAG, "Datos recibidos de Firestore - actualizando base de datos local");
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                try {
                                    Usuario usuarioRemoto = createUsuarioFromDocument(document);

                                    // Verificar si hay una versión local más reciente
                                    Usuario usuarioLocal = usuarioDao.getUsuarioSincrono(usuarioRemoto.getUid());

                                    boolean debeActualizar = true;
                                    if (usuarioLocal != null) {
                                        // Si el usuario local no está sincronizado, no sobrescribir
                                        if (!usuarioLocal.isSincronizado()) {
                                            Log.d(TAG, "Usuario local no sincronizado - manteniendo versión local: " + usuarioLocal.getUid());
                                            debeActualizar = false;
                                        }
                                        // Si el usuario local es más reciente, no sobrescribir
                                        else if (usuarioLocal.getTimestampModificacion() > usuarioRemoto.getTimestampModificacion()) {
                                            Log.d(TAG, "Usuario local es más reciente - manteniendo versión local: " + usuarioLocal.getUid());
                                            debeActualizar = false;
                                        }
                                    }

                                    if (debeActualizar) {
                                        usuarioRemoto.setSincronizado(true);
                                        usuarioDao.insert(usuarioRemoto);
                                        Log.d(TAG, "Usuario actualizado desde Firestore: " + usuarioRemoto.getUid());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error al procesar documento de Firestore", e);
                                }
                            }
                        });
                    }
                });
    }

    private void stopFirestoreListener() {
        if (firestoreListener != null) {
            Log.d(TAG, "Deteniendo listener de Firestore");
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private Usuario createUsuarioFromDocument(com.google.firebase.firestore.QueryDocumentSnapshot document) {
        String uid = document.getId();
        String nombre = document.getString("nombre");
        String apellido = document.getString("apellido");
        String email = document.getString("email");
        String telefono = document.getString("telefono");
        String dui = document.getString("dui");
        String direccion = document.getString("direccion");
        String rol = document.getString("rol");

        Usuario usuario = new Usuario(uid, nombre, apellido, email, telefono, dui, direccion, rol);
        usuario.setSincronizado(true);

        // Obtener timestamp si existe
        Long timestamp = document.getLong("timestampModificacion");
        if (timestamp != null) {
            usuario.setTimestampModificacion(timestamp);
        }

        return usuario;
    }

    public void insert(Usuario usuario) {
        if (!isOnline) {
            // Modo offline: generar ID local único y marcar como no sincronizado
            if (!usuario.getUid().startsWith("local_")) {
                // Verificar si ya existe un usuario con este DUI
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    Usuario existente = usuarioDao.getUsuarioSincrono(usuario.getDui().replace("-", "").trim());
                    if (existente != null) {
                        // Si ya existe, actualizar en lugar de crear nuevo
                        existente.setNombre(usuario.getNombre());
                        existente.setApellido(usuario.getApellido());
                        existente.setEmail(usuario.getEmail());
                        existente.setTelefono(usuario.getTelefono());
                        existente.setDireccion(usuario.getDireccion());
                        existente.setRol(usuario.getRol());
                        existente.setSincronizado(false);
                        existente.setTimestampModificacion(System.currentTimeMillis());
                        usuarioDao.insert(existente);
                    } else {
                        // Crear nuevo usuario local
                        usuario.setUid("local_" + System.currentTimeMillis());
                        usuario.setSincronizado(false);
                        usuario.setTimestampModificacion(System.currentTimeMillis());
                        usuarioDao.insert(usuario);
                    }
                });
                return;
            }
            usuario.setSincronizado(false);
            usuario.setTimestampModificacion(System.currentTimeMillis());
            Log.d(TAG, "Insertando usuario en modo offline: " + usuario.getUid());
        } else if (!isSyncing.get()) {
            // Modo online y no sincronizando: intentar subir a Firestore inmediatamente
            sincronizarUsuarioConFirestore(usuario, success -> {
                if (!success) {
                    // Si falla, marcar para sincronización posterior
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        usuario.setSincronizado(false);
                        usuarioDao.insert(usuario);
                    });
                }
            });
        }

        // Insertar/actualizar en Room
        AppDatabase.databaseWriteExecutor.execute(() -> {
            usuarioDao.insert(usuario);
        });
    }

    public void update(Usuario usuario) {
        usuario.setTimestampModificacion(System.currentTimeMillis());

        if (!isOnline || isSyncing.get()) {
            // Marcar como no sincronizado para posterior sync
            usuario.setSincronizado(false);
            Log.d(TAG, "Marcando usuario como modificado offline: " + usuario.getUid());
        } else {
            // Intentar sincronizar inmediatamente
            sincronizarUsuarioConFirestore(usuario, success -> {
                if (!success) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        usuarioDao.marcarComoModificadoOffline(usuario.getUid(), usuario.getTimestampModificacion());
                    });
                }
            });
        }

        // Actualizar en Room
        AppDatabase.databaseWriteExecutor.execute(() -> {
            usuarioDao.insert(usuario);
        });
    }

    public LiveData<List<Usuario>> getAllUsuarios() {
        return allUsuarios;
    }

    public LiveData<Usuario> getUsuario(String uid) {
        return usuarioDao.getUsuario(uid);
    }

    public LiveData<Usuario> getUsuarioByEmail(String email) {
        return usuarioDao.getUsuarioByEmail(email);
    }

    public void cleanup() {
        if (networkCallback != null) {
            // Cleanup será manejado por la Activity/ViewModel
        }
        stopFirestoreListener();
    }

    // Interface para callbacks de sincronización
    private interface SyncCallback {
        void onComplete(boolean success);
    }
}
