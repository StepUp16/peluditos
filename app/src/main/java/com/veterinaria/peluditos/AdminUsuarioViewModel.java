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
import com.veterinaria.peluditos.data.UsuarioRepository;

import java.util.List;

public class AdminUsuarioViewModel extends AndroidViewModel {
    private UsuarioRepository usuarioRepository;
    private UsuarioDao usuarioDao;
    private LiveData<List<Usuario>> allUsuarios;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public AdminUsuarioViewModel(Application application) {
        super(application);
        usuarioRepository = new UsuarioRepository(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        usuarioDao = db.usuarioDao();
        allUsuarios = usuarioRepository.getAllUsuarios();
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public void insert(Usuario usuario) {
        usuarioRepository.insert(usuario);
    }

    public void update(Usuario usuario) {
        usuarioRepository.update(usuario);
    }

    public LiveData<List<Usuario>> getAllUsuarios() {
        return allUsuarios;
    }

    public LiveData<Usuario> getUsuario(String uid) {
        return usuarioRepository.getUsuario(uid);
    }

    public LiveData<Usuario> getUsuarioByEmail(String email) {
        return usuarioRepository.getUsuarioByEmail(email);
    }

    public Usuario getUsuarioDirecto(String uid) {
        return usuarioDao.getUsuarioSincrono(uid);
    }

    public void sincronizarUsuariosLocales(Context context) {
        // El nuevo repositorio maneja automáticamente la sincronización
        // Esta función queda para compatibilidad pero ya no es necesaria
        Toast.makeText(context, "La sincronización se maneja automáticamente", Toast.LENGTH_SHORT).show();
    }

    public void deleteUsuario(Usuario usuario, Context context) {
        // Si el usuario no es local (fue creado con Firebase Auth)
        if (!usuario.getUid().startsWith("local_")) {
            // Eliminamos de Firestore
            firestore.collection("usuarios")
                    .document(usuario.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Después eliminamos de Room
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
        } else {
            // Si es un usuario local, solo eliminamos de Room
            AppDatabase.databaseWriteExecutor.execute(() -> {
                usuarioDao.deleteUsuario(usuario.getUid());
            });
            Toast.makeText(context, "Usuario local eliminado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (usuarioRepository != null) {
            usuarioRepository.cleanup();
        }
    }
}
