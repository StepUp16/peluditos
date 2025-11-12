package com.veterinaria.peluditos.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistorialMedicoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistorialMedico historial);

    @Query("SELECT * FROM historial_medico ORDER BY timestampRegistro DESC")
    LiveData<List<HistorialMedico>> getAllHistorial();

    @Query("SELECT * FROM historial_medico WHERE pacienteId = :pacienteId ORDER BY timestampRegistro DESC")
    LiveData<List<HistorialMedico>> getHistorialPorPaciente(String pacienteId);

    @Query("SELECT * FROM historial_medico WHERE sincronizado = 0")
    List<HistorialMedico> getPendientes();

    @Query("UPDATE historial_medico SET sincronizado = :sincronizado WHERE id = :id")
    void actualizarSincronizacion(String id, boolean sincronizado);
}
