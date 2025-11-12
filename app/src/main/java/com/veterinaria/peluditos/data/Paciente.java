package com.veterinaria.peluditos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pacientes")
public class Paciente {

    @PrimaryKey
    @NonNull
    private String id;
    private String nombre;
    private String especie;
    private String raza;
    private int edad;
    private double peso;
    private String sexo;
    private String clienteId;
    private String clienteNombre;
    private String fotoUrl;
    private boolean sincronizado;
    private boolean eliminado;
    private long timestampModificacion;

    public Paciente(@NonNull String id,
                    String nombre,
                    String especie,
                    String raza,
                    int edad,
                    double peso,
                    String sexo,
                    String clienteId,
                    String clienteNombre,
                    String fotoUrl) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.edad = edad;
        this.peso = peso;
        this.sexo = sexo;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.fotoUrl = fotoUrl;
        this.sincronizado = false;
        this.eliminado = false;
        this.timestampModificacion = System.currentTimeMillis();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getRaza() {
        return raza;
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public boolean isSincronizado() {
        return sincronizado;
    }

    public void setSincronizado(boolean sincronizado) {
        this.sincronizado = sincronizado;
    }

    public long getTimestampModificacion() {
        return timestampModificacion;
    }

    public void setTimestampModificacion(long timestampModificacion) {
        this.timestampModificacion = timestampModificacion;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}
