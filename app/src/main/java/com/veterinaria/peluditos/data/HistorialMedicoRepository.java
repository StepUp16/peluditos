package com.veterinaria.peluditos.data;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class HistorialMedicoRepository {

    private static final String TAG = "HistorialMedicoRepo";
    private static final String COLLECTION = "historial_medico";

    private final HistorialMedicoDao historialMedicoDao;
    private final LiveData<List<HistorialMedico>> todosLosHistoriales;
    private final FirebaseFirestore firestore;
    private final Executor executor = AppDatabase.databaseWriteExecutor;

    public HistorialMedicoRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        historialMedicoDao = db.historialMedicoDao();
        todosLosHistoriales = historialMedicoDao.getAllHistorial();
        firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<HistorialMedico>> getTodosLosHistoriales() {
        return todosLosHistoriales;
    }

    public LiveData<List<HistorialMedico>> getHistorialPorPaciente(String pacienteId) {
        if (TextUtils.isEmpty(pacienteId)) {
            return todosLosHistoriales;
        }
        return historialMedicoDao.getHistorialPorPaciente(pacienteId);
    }

    public void insert(HistorialMedico historialMedico) {
        historialMedico.setTimestampModificacion(System.currentTimeMillis());
        historialMedico.setSincronizado(false);

        executor.execute(() -> historialMedicoDao.insert(historialMedico));

        Map<String, Object> data = new HashMap<>();
        data.put("id", historialMedico.getId());
        data.put("pacienteId", historialMedico.getPacienteId());
        data.put("pacienteNombre", historialMedico.getPacienteNombre());
        data.put("fechaConsulta", historialMedico.getFechaConsulta());
        data.put("horaConsulta", historialMedico.getHoraConsulta());
        data.put("motivoConsulta", historialMedico.getMotivoConsulta());
        data.put("diagnostico", historialMedico.getDiagnostico());
        data.put("tratamiento", historialMedico.getTratamiento());
        data.put("medicacion", historialMedico.getMedicacion());
        data.put("notasAdicionales", historialMedico.getNotasAdicionales());
        data.put("timestampRegistro", historialMedico.getTimestampRegistro());
        data.put("timestampModificacion", historialMedico.getTimestampModificacion());

        firestore.collection(COLLECTION)
                .document(historialMedico.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> executor.execute(() ->
                        historialMedicoDao.actualizarSincronizacion(historialMedico.getId(), true)))
                .addOnFailureListener(e -> Log.w(TAG, "Error al sincronizar historial médico", e));
    }
}
