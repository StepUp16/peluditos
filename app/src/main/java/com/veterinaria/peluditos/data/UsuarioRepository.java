package com.veterinaria.peluditos.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuarioRepository {
    private UsuarioDao usuarioDao;
    private FirebaseFirestore firestore;
    private LiveData<List<Usuario>> allUsuarios;

    public UsuarioRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        usuarioDao = db.usuarioDao();
        firestore = FirebaseFirestore.getInstance();
        allUsuarios = usuarioDao.getAllUsuarios();
    }

    public void insert(Usuario usuario) {
        // Insertar en Room
        AppDatabase.databaseWriteExecutor.execute(() -> {
            usuarioDao.insert(usuario);
        });

        // Insertar en Firestore
        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("nombre", usuario.getNombre());
        usuarioMap.put("apellido", usuario.getApellido());
        usuarioMap.put("email", usuario.getEmail());
        usuarioMap.put("telefono", usuario.getTelefono());
        usuarioMap.put("dui", usuario.getDui());
        usuarioMap.put("direccion", usuario.getDireccion());
        usuarioMap.put("rol", usuario.getRol());

        firestore.collection("usuarios")
                .document(usuario.getUid())
                .set(usuarioMap, SetOptions.merge())
                .addOnFailureListener(e -> {
                    // Manejar el error si es necesario
                });
    }

    public LiveData<List<Usuario>> getAllUsuarios() {
        return allUsuarios;
    }

    public LiveData<Usuario> getUsuario(String uid) {
        return usuarioDao.getUsuario(uid);
    }
}
