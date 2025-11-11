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

import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class admin_cita_nueva extends AppCompatActivity {

    public static final String EXTRA_CITA_ID = "extra_cita_id";

    private Spinner spinnerPaciente;
    private Spinner spinnerCliente;
    private Spinner spinnerEstado;
    private EditText etFecha;
    private EditText etHora;
    private EditText etMotivo;
    private EditText etNotas;
    private Button btnCrearCita;

    private final List<Paciente> pacientes = new ArrayList<>();
    private final List<Paciente> pacientesFiltrados = new ArrayList<>();
    private final List<Usuario> clientes = new ArrayList<>();
    private ArrayAdapter<String> pacienteAdapter;
    private ArrayAdapter<String> clienteAdapter;

    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminCitaViewModel citaViewModel;

    private final Calendar fechaSeleccionada = Calendar.getInstance();
    private boolean fechaAsignada = false;
    private boolean horaAsignada = false;
    private String clienteSeleccionadoId;
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_cita_nueva);

        initViews();
        setupSpinners();
        setupEstadoSpinner();
        setupPickers();

        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);

        observePacientes();
        observeClientes();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnCrearCita.setOnClickListener(v -> guardarCita());
    }

    private void initViews() {
        spinnerPaciente = findViewById(R.id.spinnerPaciente);
        spinnerCliente = findViewById(R.id.spinnerCliente);
        spinnerEstado = findViewById(R.id.spinnerEstado);
        etFecha = findViewById(R.id.etFecha);
        etHora = findViewById(R.id.etHora);
        etMotivo = findViewById(R.id.etMotivo);
        etNotas = findViewById(R.id.etNotasAdicionales);
        btnCrearCita = findViewById(R.id.btnCrearCita);

        etFecha.setInputType(InputType.TYPE_NULL);
        etHora.setInputType(InputType.TYPE_NULL);
    }

    private void setupSpinners() {
        List<String> placeholderPaciente = new ArrayList<>();
        placeholderPaciente.add(getString(R.string.placeholder_select_paciente));
        pacienteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                placeholderPaciente
        );
        spinnerPaciente.setAdapter(pacienteAdapter);
        spinnerPaciente.setEnabled(false);

        List<String> placeholderCliente = new ArrayList<>();
        placeholderCliente.add(getString(R.string.placeholder_select_cliente));
        clienteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                placeholderCliente
        );
        spinnerCliente.setAdapter(clienteAdapter);
        spinnerCliente.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position - 1 < clientes.size()) {
                    clienteSeleccionadoId = clientes.get(position - 1).getUid();
                } else {
                    clienteSeleccionadoId = null;
                }
                actualizarPacientesParaCliente(clienteSeleccionadoId);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                clienteSeleccionadoId = null;
                actualizarPacientesParaCliente(null);
            }
        });
    }

    private void setupEstadoSpinner() {
        List<String> estados = new ArrayList<>();
        estados.add(getString(R.string.placeholder_select_estado));
        estados.addAll(Arrays.asList(getResources().getStringArray(R.array.cita_estados_array)));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                estados
        );
        spinnerEstado.setAdapter(adapter);
        spinnerEstado.setSelection(0);
    }

    private void setupPickers() {
        View.OnClickListener fechaClickListener = v -> mostrarDatePicker();
        etFecha.setOnClickListener(fechaClickListener);
        ImageView iconFecha = findViewById(R.id.iconFecha);
        iconFecha.setOnClickListener(fechaClickListener);

        View.OnClickListener horaClickListener = v -> mostrarTimePicker();
        etHora.setOnClickListener(horaClickListener);
        ImageView iconHora = findViewById(R.id.iconHora);
        iconHora.setOnClickListener(horaClickListener);
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
            etFecha.setText(formatoFecha.format(fechaSeleccionada.getTime()));
        }
        if (horaAsignada) {
            etHora.setText(formatoHora.format(fechaSeleccionada.getTime()));
        }
    }

    private void observePacientes() {
        pacienteViewModel.getAllPacientes().observe(this, pacientesLista -> {
            pacientes.clear();
            if (pacientesLista != null) {
                pacientes.addAll(pacientesLista);
            }
            actualizarPacientesParaCliente(clienteSeleccionadoId);
        });
    }

    private void observeClientes() {
        usuarioViewModel.getAllUsuarios().observe(this, usuarios -> {
            clientes.clear();
            List<String> nombres = new ArrayList<>();
            nombres.add(getString(R.string.placeholder_select_cliente));

            if (usuarios != null) {
                clientes.addAll(usuarios);
                for (Usuario usuario : usuarios) {
                    nombres.add(usuario.getNombre() + " " + usuario.getApellido());
                }
            }

            clienteAdapter.clear();
            clienteAdapter.addAll(nombres);
            clienteAdapter.notifyDataSetChanged();
        });
    }

    private void guardarCita() {
        Paciente pacienteSeleccionado = obtenerPacienteSeleccionado();
        Usuario clienteSeleccionado = obtenerClienteSeleccionado();
        String fecha = etFecha.getText().toString().trim();
        String hora = etHora.getText().toString().trim();
        String motivo = etMotivo.getText().toString().trim();
        String notas = etNotas.getText().toString().trim();
        String estado = spinnerEstado.getSelectedItem() != null
                ? spinnerEstado.getSelectedItem().toString()
                : getString(R.string.cita_estado_pendiente);

        if (!validarCampos(pacienteSeleccionado, clienteSeleccionado, fecha, hora, motivo, estado)) {
            return;
        }

        btnCrearCita.setEnabled(false);
        btnCrearCita.setText(R.string.btn_guardando);

        long fechaHoraTimestamp = fechaAsignada && horaAsignada
                ? fechaSeleccionada.getTimeInMillis()
                : System.currentTimeMillis();

        String pacienteNombre = pacienteSeleccionado != null ? pacienteSeleccionado.getNombre() : "";
        String pacienteId = pacienteSeleccionado != null ? pacienteSeleccionado.getId() : "";
        String clienteUid = clienteSeleccionado != null ? clienteSeleccionado.getUid() : "";
        String clienteNombreCompleto = clienteSeleccionado != null
                ? clienteSeleccionado.getNombre() + " " + clienteSeleccionado.getApellido()
                : "";

        Cita cita = new Cita(
                UUID.randomUUID().toString(),
                pacienteId,
                pacienteNombre,
                clienteUid,
                clienteNombreCompleto,
                fecha,
                hora,
                motivo,
                notas,
                fechaHoraTimestamp,
                estado,
                ""
        );
        citaViewModel.insert(cita);
        Toast.makeText(this, R.string.msg_cita_guardada, Toast.LENGTH_SHORT).show();

        finish();
    }

    private Paciente obtenerPacienteSeleccionado() {
        int position = spinnerPaciente.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= pacientesFiltrados.size()) {
            return null;
        }
        return pacientesFiltrados.get(position - 1);
    }

    private Usuario obtenerClienteSeleccionado() {
        int position = spinnerCliente.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= clientes.size()) {
            return null;
        }
        return clientes.get(position - 1);
    }

    private boolean validarCampos(Paciente paciente,
                                  Usuario cliente,
                                  String fecha,
                                  String hora,
                                  String motivo,
                                  String estado) {
        if (paciente == null) {
            Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cliente == null) {
            Toast.makeText(this, R.string.error_cliente, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(fecha)) {
            etFecha.setError(getString(R.string.error_fecha));
            etFecha.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(hora)) {
            etHora.setError(getString(R.string.error_hora));
            etHora.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(motivo)) {
            etMotivo.setError(getString(R.string.error_motivo));
            etMotivo.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(estado) || estado.equals(getString(R.string.placeholder_select_estado))) {
            Toast.makeText(this, R.string.error_estado_cita, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void actualizarPacientesParaCliente(String clienteId) {
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.placeholder_select_paciente));
        pacientesFiltrados.clear();

        if (!TextUtils.isEmpty(clienteId)) {
            for (Paciente paciente : pacientes) {
                if (clienteId.equals(paciente.getClienteId())) {
                    pacientesFiltrados.add(paciente);
                    nombres.add(paciente.getNombre());
                }
            }
        }

        spinnerPaciente.setEnabled(!pacientesFiltrados.isEmpty());

        pacienteAdapter.clear();
        pacienteAdapter.addAll(nombres);
        pacienteAdapter.notifyDataSetChanged();
    }
}
