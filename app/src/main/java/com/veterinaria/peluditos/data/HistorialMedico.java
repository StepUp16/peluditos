package com.veterinaria.peluditos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "historial_medico")
public class HistorialMedico {

    @PrimaryKey
    @NonNull
    private String id;
    private String pacienteId;
    private String pacienteNombre;
    private String fechaConsulta;
    private String horaConsulta;
    private String motivoConsulta;
    private String diagnostico;
    private String tratamiento;
    private String medicacion;
    private String notasAdicionales;
    private long timestampRegistro;
    private long timestampModificacion;
    private boolean sincronizado;

    public HistorialMedico(@NonNull String id,
                           String pacienteId,
                           String pacienteNombre,
                           String fechaConsulta,
                           String horaConsulta,
                           String motivoConsulta,
                           String diagnostico,
                           String tratamiento,
                           String medicacion,
                           String notasAdicionales,
                           long timestampRegistro) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.pacienteNombre = pacienteNombre;
        this.fechaConsulta = fechaConsulta;
        this.horaConsulta = horaConsulta;
        this.motivoConsulta = motivoConsulta;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.medicacion = medicacion;
        this.notasAdicionales = notasAdicionales;
        this.timestampRegistro = timestampRegistro;
        this.timestampModificacion = System.currentTimeMillis();
        this.sincronizado = false;
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

    public String getFechaConsulta() {
        return fechaConsulta;
    }

    public void setFechaConsulta(String fechaConsulta) {
        this.fechaConsulta = fechaConsulta;
    }

    public String getHoraConsulta() {
        return horaConsulta;
    }

    public void setHoraConsulta(String horaConsulta) {
        this.horaConsulta = horaConsulta;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String motivoConsulta) {
        this.motivoConsulta = motivoConsulta;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getMedicacion() {
        return medicacion;
    }

    public void setMedicacion(String medicacion) {
        this.medicacion = medicacion;
    }

    public String getNotasAdicionales() {
        return notasAdicionales;
    }

    public void setNotasAdicionales(String notasAdicionales) {
        this.notasAdicionales = notasAdicionales;
    }

    public long getTimestampRegistro() {
        return timestampRegistro;
    }

    public void setTimestampRegistro(long timestampRegistro) {
        this.timestampRegistro = timestampRegistro;
    }

    public long getTimestampModificacion() {
        return timestampModificacion;
    }

    public void setTimestampModificacion(long timestampModificacion) {
        this.timestampModificacion = timestampModificacion;
    }

    public boolean isSincronizado() {
        return sincronizado;
    }

    public void setSincronizado(boolean sincronizado) {
        this.sincronizado = sincronizado;
    }
}
