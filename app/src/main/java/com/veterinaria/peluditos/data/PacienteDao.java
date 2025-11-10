package com.veterinaria.peluditos.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PacienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Paciente paciente);

    @Query("SELECT * FROM pacientes WHERE eliminado = 0 ORDER BY timestampModificacion DESC")
    LiveData<List<Paciente>> getAllPacientes();

    @Query("SELECT * FROM pacientes WHERE id = :id LIMIT 1")
    Paciente getPaciente(String id);

    @Query("SELECT * FROM pacientes WHERE id = :id LIMIT 1")
    LiveData<Paciente> getPacienteLive(String id);

    @Query("SELECT * FROM pacientes WHERE sincronizado = 0 ORDER BY timestampModificacion DESC")
    List<Paciente> getPacientesPendientes();

    @Query("UPDATE pacientes SET sincronizado = :sincronizado WHERE id = :id")
    void actualizarSincronizacion(String id, boolean sincronizado);

    @Query("DELETE FROM pacientes WHERE id = :id")
    void deleteById(String id);
}
