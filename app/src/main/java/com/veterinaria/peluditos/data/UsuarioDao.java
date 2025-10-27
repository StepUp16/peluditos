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

    @Query("SELECT * FROM usuarios")
    LiveData<List<Usuario>> getAllUsuarios();

    @Query("SELECT * FROM usuarios WHERE rol = 'cliente'")
    LiveData<List<Usuario>> getAllClientes();

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    LiveData<Usuario> getUsuario(String uid);

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    Usuario getUsuarioSincrono(String uid);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    LiveData<Usuario> getUsuarioByEmail(String email);

    @Query("DELETE FROM usuarios WHERE uid = :uid")
    void deleteUsuario(String uid);

    // Obtener usuarios no sincronizados (offline changes)
    @Query("SELECT * FROM usuarios WHERE sincronizado = 0 ORDER BY timestampModificacion DESC")
    List<Usuario> getUsuariosLocales();

    // Actualizar estado de sincronización
    @Query("UPDATE usuarios SET sincronizado = :sincronizado WHERE uid = :uid")
    void actualizarSincronizacion(String uid, boolean sincronizado);

    // Actualizar el ID de un usuario local con su nuevo ID de Firebase
    @Query("UPDATE usuarios SET uid = :nuevoUid WHERE uid = :viejoUid")
    void actualizarUid(String viejoUid, String nuevoUid);

    // Marcar un usuario como modificado offline
    @Query("UPDATE usuarios SET sincronizado = 0, timestampModificacion = :timestamp WHERE uid = :uid")
    void marcarComoModificadoOffline(String uid, long timestamp);

    // Verificar si hay cambios pendientes de sincronización
    @Query("SELECT COUNT(*) FROM usuarios WHERE sincronizado = 0")
    int getCantidadCambiosPendientes();

    // Limpiar usuarios locales duplicados
    @Query("DELETE FROM usuarios WHERE uid LIKE 'local_%' AND EXISTS (SELECT 1 FROM usuarios u2 WHERE u2.dui = usuarios.dui AND u2.uid NOT LIKE 'local_%')")
    void limpiarUsuariosLocalesDuplicados();
}
