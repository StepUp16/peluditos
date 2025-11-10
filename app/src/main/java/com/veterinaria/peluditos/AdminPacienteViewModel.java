package com.veterinaria.peluditos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.PacienteRepository;

import java.util.List;

public class AdminPacienteViewModel extends AndroidViewModel {

    private final PacienteRepository repository;
    private final LiveData<List<Paciente>> allPacientes;

    public AdminPacienteViewModel(@NonNull Application application) {
        super(application);
        repository = new PacienteRepository(application);
        allPacientes = repository.getAllPacientes();
    }

    public LiveData<List<Paciente>> getAllPacientes() {
        return allPacientes;
    }

    public LiveData<Paciente> getPaciente(String id) {
        return repository.getPaciente(id);
    }

    public void insert(Paciente paciente) {
        repository.insert(paciente);
    }

    public void update(Paciente paciente) {
        repository.update(paciente);
    }

    public void delete(Paciente paciente) {
        repository.delete(paciente);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
