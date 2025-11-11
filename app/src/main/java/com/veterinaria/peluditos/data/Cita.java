package com.veterinaria.peluditos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "citas")
public class Cita {

    @PrimaryKey
    @NonNull
    private String id;
    private String pacienteId;
    private String pacienteNombre;
    private String clienteId;
    private String clienteNombre;
    private String fecha;
    private String hora;
    private String motivo;
    private String notas;
    private long fechaHoraTimestamp;
    private boolean sincronizado;
    private long timestampModificacion;
    private String estado;

    public Cita(@NonNull String id,
                String pacienteId,
                String pacienteNombre,
                String clienteId,
                String clienteNombre,
                String fecha,
                String hora,
                String motivo,
                String notas,
                long fechaHoraTimestamp,
                String estado) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.pacienteNombre = pacienteNombre;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.fecha = fecha;
        this.hora = hora;
        this.motivo = motivo;
        this.notas = notas;
        this.fechaHoraTimestamp = fechaHoraTimestamp;
        this.estado = estado;
        this.sincronizado = false;
        this.timestampModificacion = System.currentTimeMillis();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public void setPacienteNombre(String pacienteNombre) {
        this.pacienteNombre = pacienteNombre;
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

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public long getFechaHoraTimestamp() {
        return fechaHoraTimestamp;
    }

    public void setFechaHoraTimestamp(long fechaHoraTimestamp) {
        this.fechaHoraTimestamp = fechaHoraTimestamp;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
