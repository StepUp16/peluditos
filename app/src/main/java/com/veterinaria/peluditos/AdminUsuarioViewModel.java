package com.veterinaria.peluditos;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.data.UsuarioRepository;

import java.util.List;

public class AdminUsuarioViewModel extends AndroidViewModel {
    private UsuarioRepository repository;
    private LiveData<List<Usuario>> allUsuarios;

    public AdminUsuarioViewModel(Application application) {
        super(application);
        repository = new UsuarioRepository(application);
        allUsuarios = repository.getAllUsuarios();
    }

    public void insert(Usuario usuario) {
        repository.insert(usuario);
    }

    public LiveData<List<Usuario>> getAllUsuarios() {
        return allUsuarios;
    }

    public LiveData<Usuario> getUsuario(String uid) {
        return repository.getUsuario(uid);
    }
}
