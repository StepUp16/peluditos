package com.veterinaria.peluditos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veterinaria.peluditos.data.HistorialMedico;
import com.veterinaria.peluditos.data.HistorialMedicoRepository;

import java.util.List;

public class AdminHistorialMedicoViewModel extends AndroidViewModel {

    private final HistorialMedicoRepository repository;

    public AdminHistorialMedicoViewModel(@NonNull Application application) {
        super(application);
        repository = new HistorialMedicoRepository(application);
    }

    public LiveData<List<HistorialMedico>> getTodos() {
        return repository.getTodosLosHistoriales();
    }

    public LiveData<List<HistorialMedico>> getPorPaciente(String pacienteId) {
        return repository.getHistorialPorPaciente(pacienteId);
    }

    public void insert(HistorialMedico historialMedico) {
        repository.insert(historialMedico);
    }
}
