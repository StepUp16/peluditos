package com.veterinaria.peluditos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.CitaRepository;

import java.util.List;

public class AdminCitaViewModel extends AndroidViewModel {

    private final CitaRepository repository;
    private final LiveData<List<Cita>> allCitas;

    public AdminCitaViewModel(@NonNull Application application) {
        super(application);
        repository = new CitaRepository(application);
        allCitas = repository.getAllCitas();
    }

    public LiveData<List<Cita>> getAllCitas() {
        return allCitas;
    }

    public LiveData<Cita> getCita(String id) {
        return repository.getCita(id);
    }

    public void insert(Cita cita) {
        repository.insert(cita);
    }

    public void update(Cita cita) {
        repository.update(cita);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
