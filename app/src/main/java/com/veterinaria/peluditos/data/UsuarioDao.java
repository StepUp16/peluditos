package com.veterinaria.peluditos.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE rol = 'cliente'")
    LiveData<List<Usuario>> getAllUsuarios();

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    LiveData<Usuario> getUsuario(String uid);

    @Query("DELETE FROM usuarios WHERE uid = :uid")
    void deleteUsuario(String uid);

    // Obtener usuarios locales pendientes de sincronización
    @Query("SELECT * FROM usuarios WHERE uid LIKE 'local_%'")
    List<Usuario> getUsuariosLocales();

    // Actualizar el ID de un usuario local con su nuevo ID de Firebase
    @Query("UPDATE usuarios SET uid = :nuevoUid WHERE uid = :viejoUid")
    void actualizarUid(String viejoUid, String nuevoUid);
}
