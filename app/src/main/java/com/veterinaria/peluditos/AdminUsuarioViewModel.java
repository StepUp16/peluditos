package com.veterinaria.peluditos;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veterinaria.peluditos.data.AppDatabase;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.data.UsuarioDao;

import java.util.List;

public class AdminUsuarioViewModel extends AndroidViewModel {
    private UsuarioDao usuarioDao;
    private LiveData<List<Usuario>> allUsuarios;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public AdminUsuarioViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        usuarioDao = db.usuarioDao();
        allUsuarios = usuarioDao.getAllUsuarios();
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public void insert(Usuario usuario) {
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

    public void sincronizarUsuariosLocales(Context context) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Usuario> usuariosLocales = usuarioDao.getUsuariosLocales();
            if (!usuariosLocales.isEmpty()) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> Toast.makeText(context,
                        "Encontrados " + usuariosLocales.size() + " usuarios para sincronizar",
                        Toast.LENGTH_SHORT).show());

                for (Usuario usuario : usuariosLocales) {
                    sincronizarUsuario(usuario, context);
                }
            }
        });
    }

    private void sincronizarUsuario(Usuario usuario, Context context) {
        String email = usuario.getEmail();
        String dui = usuario.getDui().replace("-", "").trim();
        String password = "Temporal123@"; // Contraseña temporal que cumple con los requisitos

        // Primero verificamos si el usuario ya existe en Firestore
        firestore.collection("usuarios")
                .document(dui)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Si no existe, creamos el usuario en Firebase Auth
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener(authResult -> {
                                    String viejoUid = usuario.getUid();
                                    usuario.setUid(dui); // Usar el DUI como ID definitivo

                                    // Guardar en Firestore
                                    firestore.collection("usuarios")
                                            .document(dui)
                                            .set(usuario)
                                            .addOnSuccessListener(aVoid -> {
                                                // Actualizar el ID en Room
                                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                                    usuarioDao.actualizarUid(viejoUid, dui);
                                                });
                                                Handler handler = new Handler(Looper.getMainLooper());
                                                handler.post(() -> Toast.makeText(context,
                                                        "Usuario sincronizado exitosamente: " + usuario.getNombre(),
                                                        Toast.LENGTH_SHORT).show());
                                            })
                                            .addOnFailureListener(e -> mostrarError(context,
                                                    "Error al sincronizar con Firestore: " + e.getMessage()));
                                })
                                .addOnFailureListener(e -> mostrarError(context,
                                        "Error al crear usuario en Firebase Auth: " + e.getMessage()));
                    } else {
                        mostrarError(context, "El usuario con DUI " + usuario.getDui() +
                                " ya existe en Firebase");
                    }
                })
                .addOnFailureListener(e -> mostrarError(context,
                        "Error al verificar usuario en Firestore: " + e.getMessage()));
    }

    private void mostrarError(Context context, String mensaje) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show());
    }

    public void deleteUsuario(Usuario usuario, Context context) {
        // Si el usuario no es local (fue creado con Firebase Auth)
        if (!usuario.getUid().startsWith("local_")) {
            // Primero eliminamos el usuario de Firebase Auth
            FirebaseAuth.getInstance().signInWithEmailAndPassword(usuario.getEmail(), "temporal123")
                    .addOnSuccessListener(authResult -> {
                        // Una vez autenticados, eliminamos el usuario
                        authResult.getUser().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Después de eliminar de Auth, eliminamos de Firestore
                                    firestore.collection("usuarios")
                                            .document(usuario.getUid())
                                            .delete()
                                            .addOnSuccessListener(void1 -> {
                                                // Finalmente eliminamos de Room
                                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                                    usuarioDao.deleteUsuario(usuario.getUid());
                                                });
                                                Toast.makeText(context, "Usuario eliminado completamente",
                                                        Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context,
                                                        "Error al eliminar de Firestore: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context,
                                            "Error al eliminar usuario de Firebase Auth: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        // Si falla la autenticación, solo eliminamos de Firestore y Room
                        firestore.collection("usuarios")
                                .document(usuario.getUid())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    AppDatabase.databaseWriteExecutor.execute(() -> {
                                        usuarioDao.deleteUsuario(usuario.getUid());
                                    });
                                    Toast.makeText(context, "Usuario eliminado de Firestore y base local",
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
        } else {
            // Si es un usuario local, solo eliminamos de Room
            AppDatabase.databaseWriteExecutor.execute(() -> {
                usuarioDao.deleteUsuario(usuario.getUid());
            });
            Toast.makeText(context, "Usuario local eliminado", Toast.LENGTH_SHORT).show();
        }
    }
}
