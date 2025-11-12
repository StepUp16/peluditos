package com.veterinaria.peluditos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.veterinaria.peluditos.data.HistorialMedico;
import com.veterinaria.peluditos.data.Paciente;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class admin_historial_medico_nuevo extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";
    public static final String EXTRA_PACIENTE_NOMBRE = "extra_paciente_nombre";

    private Spinner spinnerPaciente;
    private EditText etFechaConsulta;
    private EditText etHoraConsulta;
    private EditText etMotivoConsulta;
    private EditText etDiagnostico;
    private EditText etTratamiento;
    private EditText etMedicacion;
    private EditText etNotasAdicionales;
    private Button btnGuardarHistorial;
    private ImageView iconFecha;
    private ImageView iconHora;

    private AdminPacienteViewModel pacienteViewModel;
    private AdminHistorialMedicoViewModel historialMedicoViewModel;

    private final List<Paciente> pacientes = new ArrayList<>();
    private ArrayAdapter<String> pacienteAdapter;

    private final Calendar fechaSeleccionada = Calendar.getInstance();
    private boolean fechaAsignada = false;
    private boolean horaAsignada = false;
    private String pacienteIdPreseleccionado;

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_historial_medico_nuevo);

        pacienteIdPreseleccionado = getIntent() != null
                ? getIntent().getStringExtra(EXTRA_PACIENTE_ID)
                : null;

        initViews();
        setupSpinner();
        setupPickers();

        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        historialMedicoViewModel = new ViewModelProvider(this).get(AdminHistorialMedicoViewModel.class);

        observePacientes();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnGuardarHistorial.setOnClickListener(v -> guardarHistorial());
    }

    private void initViews() {
        spinnerPaciente = findViewById(R.id.spinnerPaciente);
        etFechaConsulta = findViewById(R.id.etFechaConsulta);
        etHoraConsulta = findViewById(R.id.etHoraConsulta);
        etMotivoConsulta = findViewById(R.id.etMotivoConsulta);
        etDiagnostico = findViewById(R.id.etDiagnostico);
        etTratamiento = findViewById(R.id.etTratamiento);
        etMedicacion = findViewById(R.id.etMedicacion);
        etNotasAdicionales = findViewById(R.id.etNotasAdicionales);
        btnGuardarHistorial = findViewById(R.id.btnGuardarHistorial);
        iconFecha = findViewById(R.id.iconFecha);
        iconHora = findViewById(R.id.iconHora);

        etFechaConsulta.setInputType(InputType.TYPE_NULL);
        etHoraConsulta.setInputType(InputType.TYPE_NULL);
    }

    private void setupSpinner() {
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.hint_seleccionar_paciente));
        pacienteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nombres
        );
        spinnerPaciente.setAdapter(pacienteAdapter);
    }

    private void setupPickers() {
        View.OnClickListener fechaListener = v -> mostrarDatePicker();
        etFechaConsulta.setOnClickListener(fechaListener);
        iconFecha.setOnClickListener(fechaListener);

        View.OnClickListener horaListener = v -> mostrarTimePicker();
        etHoraConsulta.setOnClickListener(horaListener);
        iconHora.setOnClickListener(horaListener);
    }

    private void observePacientes() {
        pacienteViewModel.getAllPacientes().observe(this, pacientesLista -> {
            pacientes.clear();
            List<String> nombres = new ArrayList<>();
            nombres.add(getString(R.string.hint_seleccionar_paciente));
            if (pacientesLista != null) {
                pacientes.addAll(pacientesLista);
                for (Paciente paciente : pacientesLista) {
                    nombres.add(paciente.getNombre());
                }
            }
            pacienteAdapter.clear();
            pacienteAdapter.addAll(nombres);
            pacienteAdapter.notifyDataSetChanged();
            preseleccionarPaciente();
        });
    }

    private void preseleccionarPaciente() {
        if (TextUtils.isEmpty(pacienteIdPreseleccionado)) {
            return;
        }
        for (int i = 0; i < pacientes.size(); i++) {
            if (pacienteIdPreseleccionado.equals(pacientes.get(i).getId())) {
                spinnerPaciente.setSelection(i + 1);
                break;
            }
        }
    }

    private void mostrarDatePicker() {
        final Calendar actual = Calendar.getInstance();
        int year = fechaAsignada ? fechaSeleccionada.get(Calendar.YEAR) : actual.get(Calendar.YEAR);
        int month = fechaAsignada ? fechaSeleccionada.get(Calendar.MONTH) : actual.get(Calendar.MONTH);
        int day = fechaAsignada ? fechaSeleccionada.get(Calendar.DAY_OF_MONTH) : actual.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            fechaSeleccionada.set(Calendar.YEAR, y);
            fechaSeleccionada.set(Calendar.MONTH, m);
            fechaSeleccionada.set(Calendar.DAY_OF_MONTH, d);
            fechaAsignada = true;
            actualizarCamposFechaHora();
        }, year, month, day);
        dialog.show();
    }

    private void mostrarTimePicker() {
        final Calendar actual = Calendar.getInstance();
        int hour = horaAsignada ? fechaSeleccionada.get(Calendar.HOUR_OF_DAY) : actual.get(Calendar.HOUR_OF_DAY);
        int minute = horaAsignada ? fechaSeleccionada.get(Calendar.MINUTE) : actual.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            fechaSeleccionada.set(Calendar.HOUR_OF_DAY, h);
            fechaSeleccionada.set(Calendar.MINUTE, m);
            horaAsignada = true;
            actualizarCamposFechaHora();
        }, hour, minute, true);
        dialog.show();
    }

    private void actualizarCamposFechaHora() {
        if (fechaAsignada) {
            etFechaConsulta.setText(formatoFecha.format(fechaSeleccionada.getTime()));
        }
        if (horaAsignada) {
            etHoraConsulta.setText(formatoHora.format(fechaSeleccionada.getTime()));
        }
    }

    private void guardarHistorial() {
        Paciente pacienteSeleccionado = obtenerPacienteSeleccionado();
        String fecha = etFechaConsulta.getText().toString().trim();
        String hora = etHoraConsulta.getText().toString().trim();
        String motivo = etMotivoConsulta.getText().toString().trim();
        String diagnostico = etDiagnostico.getText().toString().trim();
        String tratamiento = etTratamiento.getText().toString().trim();
        String medicacion = etMedicacion.getText().toString().trim();
        String notas = etNotasAdicionales.getText().toString().trim();

        if (!validarCampos(pacienteSeleccionado, fecha, hora, motivo, diagnostico, tratamiento, medicacion)) {
            return;
        }

        btnGuardarHistorial.setEnabled(false);

        long timestampRegistro = fechaAsignada && horaAsignada
                ? fechaSeleccionada.getTimeInMillis()
                : System.currentTimeMillis();

        HistorialMedico historial = new HistorialMedico(
                UUID.randomUUID().toString(),
                pacienteSeleccionado != null ? pacienteSeleccionado.getId() : "",
                pacienteSeleccionado != null ? pacienteSeleccionado.getNombre() : "",
                fecha,
                hora,
                motivo,
                diagnostico,
                tratamiento,
                medicacion,
                notas,
                timestampRegistro
        );

        historialMedicoViewModel.insert(historial);
        Toast.makeText(this, R.string.msg_historial_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private Paciente obtenerPacienteSeleccionado() {
        int position = spinnerPaciente.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= pacientes.size()) {
            return null;
        }
        return pacientes.get(position - 1);
    }

    private boolean validarCampos(Paciente paciente,
                                  String fecha,
                                  String hora,
                                  String motivo,
                                  String diagnostico,
                                  String tratamiento,
                                  String medicacion) {
        if (paciente == null) {
            Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(fecha)) {
            etFechaConsulta.setError(getString(R.string.error_fecha));
            etFechaConsulta.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(hora)) {
            etHoraConsulta.setError(getString(R.string.error_hora));
            etHoraConsulta.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(motivo)) {
            etMotivoConsulta.setError(getString(R.string.error_motivo));
            etMotivoConsulta.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(diagnostico)) {
            etDiagnostico.setError(getString(R.string.error_diagnostico));
            etDiagnostico.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(tratamiento)) {
            etTratamiento.setError(getString(R.string.error_tratamiento));
            etTratamiento.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(medicacion)) {
            etMedicacion.setError(getString(R.string.error_medicacion));
            etMedicacion.requestFocus();
            return false;
        }

        return true;
    }
}
