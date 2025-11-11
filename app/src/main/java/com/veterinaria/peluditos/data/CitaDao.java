package com.veterinaria.peluditos.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CitaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Cita cita);

    @Query("SELECT * FROM citas WHERE pendienteEliminacion = 0 ORDER BY fechaHoraTimestamp ASC")
    LiveData<List<Cita>> getAllCitas();

    @Query("SELECT * FROM citas WHERE id = :id LIMIT 1")
    LiveData<Cita> getCita(String id);

    @Query("SELECT * FROM citas WHERE sincronizado = 0 OR pendienteEliminacion = 1")
    List<Cita> getPendientes();

    @Query("UPDATE citas SET sincronizado = :sincronizado WHERE id = :id")
    void actualizarSincronizacion(String id, boolean sincronizado);

    @Query("DELETE FROM citas WHERE id = :id")
    void deleteById(String id);
}
