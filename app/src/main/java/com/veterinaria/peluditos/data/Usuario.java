package com.veterinaria.peluditos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey
    @NonNull
    private String uid;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String dui;
    private String direccion;
    private String rol;
    private boolean sincronizado = true;
    private long timestampModificacion = System.currentTimeMillis();

    public Usuario(@NonNull String uid, String nombre, String apellido, String email,
                  String telefono, String dui, String direccion, String rol) {
        this.uid = uid;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.dui = dui;
        this.direccion = direccion;
        this.rol = rol;
        this.sincronizado = true;
        this.timestampModificacion = System.currentTimeMillis();
    }

    @NonNull
    public String getUid() { return uid; }
    public void setUid(@NonNull String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = dui; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isSincronizado() { return sincronizado; }
    public void setSincronizado(boolean sincronizado) { this.sincronizado = sincronizado; }

    public long getTimestampModificacion() { return timestampModificacion; }
    public void setTimestampModificacion(long timestampModificacion) { this.timestampModificacion = timestampModificacion; }
}
