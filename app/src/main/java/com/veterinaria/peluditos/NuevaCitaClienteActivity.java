package com.veterinaria.peluditos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NuevaCitaClienteActivity extends AppCompatActivity {

    private Spinner spinnerPaciente;
    private TextView tvClienteActual;
    private EditText etFecha;
    private EditText etHora;
    private Spinner spinnerMotivo;
    private EditText etNotas;
    private Button btnCrearCita;

    private FirebaseAuth firebaseAuth;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminCitaViewModel citaViewModel;

    private final List<Paciente> todosLosPacientes = new ArrayList<>();
    private final List<Paciente> pacientesDelCliente = new ArrayList<>();
    private ArrayAdapter<String> pacienteAdapter;
    private ArrayAdapter<String> motivoAdapter;

    private final Calendar fechaSeleccionada = Calendar.getInstance();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private boolean fechaAsignada = false;
    private boolean horaAsignada = false;

    private String clienteId;
    private String clienteNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nueva_cita_cliente);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        firebaseAuth = FirebaseAuth.getInstance();
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);

        initViews();
        setupPacienteSpinner();

        btnCrearCita.setOnClickListener(v -> guardarCita());

        cargarClienteActual();

        pacienteViewModel.getAllPacientes().observe(this, lista -> {
            todosLosPacientes.clear();
            if (lista != null) {
                todosLosPacientes.addAll(lista);
            }
            actualizarPacientesDelCliente();
        });
    }

    private void initViews() {
        spinnerPaciente = findViewById(R.id.spinnerPaciente);
        tvClienteActual = findViewById(R.id.tvClienteActual);
        etFecha = findViewById(R.id.etFecha);
        etHora = findViewById(R.id.etHora);
        spinnerMotivo = findViewById(R.id.spinnerMotivo);
        etNotas = findViewById(R.id.etNotasAdicionales);
        btnCrearCita = findViewById(R.id.btnCrearCita);
        ImageButton btnPickFecha = findViewById(R.id.btnPickFecha);
        ImageButton btnPickHora = findViewById(R.id.btnPickHora);

        btnCrearCita.setEnabled(false);

        View.OnClickListener fechaListener = v -> mostrarSelectorFecha();
        etFecha.setOnClickListener(fechaListener);
        btnPickFecha.setOnClickListener(fechaListener);

        View.OnClickListener horaListener = v -> mostrarSelectorHora();
        etHora.setOnClickListener(horaListener);
        btnPickHora.setOnClickListener(horaListener);

        setupMotivoSpinner();
    }

    private void setupPacienteSpinner() {
        List<String> placeholder = new ArrayList<>();
        placeholder.add(getString(R.string.placeholder_select_paciente));
        pacienteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, placeholder);
        spinnerPaciente.setAdapter(pacienteAdapter);
        spinnerPaciente.setEnabled(false);
    }

    private void setupMotivoSpinner() {
        List<String> motivos = new ArrayList<>();
        motivos.add(getString(R.string.hint_motivo));
        motivos.add("Consulta general");
        motivos.add("Vacunación");
        motivos.add("Desparasitación");
        motivos.add("Control");
        motivos.add("Otro");
        motivoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, motivos);
        spinnerMotivo.setAdapter(motivoAdapter);
    }

    private void cargarClienteActual() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        clienteId = user.getUid();
        clienteNombre = obtenerNombreDesdeFirebase(user);
        mostrarClienteEnPantalla(clienteNombre);

        String email = user.getEmail();
        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    clienteId = usuario.getUid();
                    clienteNombre = construirNombreCompleto(usuario);
                    mostrarClienteEnPantalla(clienteNombre);
                    actualizarPacientesDelCliente();
                }
            });
        } else {
            usuarioViewModel.getUsuario(clienteId).observe(this, usuario -> {
                if (usuario != null) {
                    clienteNombre = construirNombreCompleto(usuario);
                    mostrarClienteEnPantalla(clienteNombre);
                    actualizarPacientesDelCliente();
                }
            });
        }

        actualizarPacientesDelCliente();
    }

    private void mostrarClienteEnPantalla(String nombre) {
        tvClienteActual.setText(getString(R.string.text_cliente_actual, nombre));
    }

    private void actualizarPacientesDelCliente() {
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.placeholder_select_paciente));
        pacientesDelCliente.clear();
        btnCrearCita.setText(R.string.btn_solicitar_cita);

        if (!TextUtils.isEmpty(clienteId)) {
            for (Paciente paciente : todosLosPacientes) {
                if (clienteId.equals(paciente.getClienteId())) {
                    pacientesDelCliente.add(paciente);
                    nombres.add(paciente.getNombre());
                }
            }
        }

        pacienteAdapter.clear();
        pacienteAdapter.addAll(nombres);
        pacienteAdapter.notifyDataSetChanged();
        spinnerPaciente.setSelection(0);

        boolean tieneMascotas = !pacientesDelCliente.isEmpty();
        spinnerPaciente.setEnabled(tieneMascotas);
        btnCrearCita.setEnabled(tieneMascotas);

        if (!TextUtils.isEmpty(clienteId) && !tieneMascotas) {
            Toast.makeText(this, R.string.error_cliente_sin_mascotas, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarSelectorFecha() {
        Calendar hoy = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    fechaSeleccionada.set(Calendar.YEAR, year);
                    fechaSeleccionada.set(Calendar.MONTH, month);
                    fechaSeleccionada.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    fechaAsignada = true;
                    etFecha.setText(formatoFecha.format(fechaSeleccionada.getTime()));
                },
                fechaSeleccionada.get(Calendar.YEAR),
                fechaSeleccionada.get(Calendar.MONTH),
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(hoy.getTimeInMillis());
        dialog.show();
    }

    private void mostrarSelectorHora() {
        Calendar referencia = Calendar.getInstance();
        int horaInicial = horaAsignada ? fechaSeleccionada.get(Calendar.HOUR_OF_DAY) : referencia.get(Calendar.HOUR_OF_DAY);
        int minutoInicial = horaAsignada ? fechaSeleccionada.get(Calendar.MINUTE) : referencia.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    fechaSeleccionada.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    fechaSeleccionada.set(Calendar.MINUTE, minute);
                    horaAsignada = true;
                    etHora.setText(formatoHora.format(fechaSeleccionada.getTime()));
                },
                horaInicial,
                minutoInicial,
                true
        );
        dialog.show();
    }

    private void guardarCita() {
        if (TextUtils.isEmpty(clienteId)) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_LONG).show();
            return;
        }

        Paciente pacienteSeleccionado = obtenerPacienteSeleccionado();
        if (pacienteSeleccionado == null) {
            Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
            return;
        }

        String fecha = etFecha.getText().toString().trim();
        if (TextUtils.isEmpty(fecha)) {
            etFecha.setError(getString(R.string.error_fecha));
            etFecha.requestFocus();
            return;
        }

        String hora = etHora.getText().toString().trim();
        if (TextUtils.isEmpty(hora)) {
            etHora.setError(getString(R.string.error_hora));
            etHora.requestFocus();
            return;
        }

        String motivo = obtenerMotivoSeleccionado();
        if (TextUtils.isEmpty(motivo) || motivo.equals(getString(R.string.hint_motivo))) {
            Toast.makeText(this, R.string.error_motivo, Toast.LENGTH_SHORT).show();
            return;
        }

        String notas = etNotas.getText().toString().trim();

        btnCrearCita.setEnabled(false);
        btnCrearCita.setText(R.string.btn_guardando);

        long timestamp = (fechaAsignada && horaAsignada)
                ? fechaSeleccionada.getTimeInMillis()
                : System.currentTimeMillis();

        String estadoInicial = getString(R.string.cita_estado_pendiente);

        Cita cita = new Cita(
                UUID.randomUUID().toString(),
                pacienteSeleccionado.getId(),
                pacienteSeleccionado.getNombre(),
                clienteId,
                clienteNombre,
                fecha,
                hora,
                motivo,
                notas,
                timestamp,
                estadoInicial,
                ""
        );

        citaViewModel.insert(cita);
        Toast.makeText(this, R.string.msg_cita_guardada, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String obtenerMotivoSeleccionado() {
        Object sel = spinnerMotivo != null ? spinnerMotivo.getSelectedItem() : null;
        return sel != null ? sel.toString().trim() : "";
    }

    private Paciente obtenerPacienteSeleccionado() {
        int position = spinnerPaciente.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= pacientesDelCliente.size()) {
            return null;
        }
        return pacientesDelCliente.get(position - 1);
    }

    private String construirNombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";

        if (!TextUtils.isEmpty(nombre) && !TextUtils.isEmpty(apellido)) {
            return nombre + " " + apellido;
        } else if (!TextUtils.isEmpty(nombre)) {
            return nombre;
        } else if (!TextUtils.isEmpty(apellido)) {
            return apellido;
        }
        return getString(R.string.text_cliente_sin_nombre);
    }

    private String obtenerNombreDesdeFirebase(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = user.getEmail();
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.text_cliente_sin_nombre);
        }
        return displayName;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
