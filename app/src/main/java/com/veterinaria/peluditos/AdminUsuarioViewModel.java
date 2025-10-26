package com.veterinaria.peluditos;

import android.app.Application;
import android.content.Context;
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

    public AdminUsuarioViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        usuarioDao = db.usuarioDao();
        allUsuarios = usuarioDao.getAllUsuarios();
        firestore = FirebaseFirestore.getInstance();
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
