package com.veterinaria.peluditos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditarCitaClienteActivity extends AppCompatActivity {

    public static final String EXTRA_CITA_ID = "extra_cita_id";

    private Spinner spinnerPaciente;
    private Spinner spinnerCliente;
    private Spinner spinnerMotivo;
    private EditText etFecha;
    private EditText etHora;
    private EditText etNotas;
    private Button btnGuardar;
    private Button btnCancelar;
    private Button btnEliminar;

    private FirebaseAuth firebaseAuth;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminCitaViewModel citaViewModel;

    private final List<Paciente> todosLosPacientes = new ArrayList<>();
    private final List<Paciente> pacientesDelCliente = new ArrayList<>();
    private ArrayAdapter<String> pacienteAdapter;
    private ArrayAdapter<String> clienteAdapter;
    private ArrayAdapter<String> motivoAdapter;

    private final Calendar fechaSeleccionada = Calendar.getInstance();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private boolean fechaAsignada = false;
    private boolean horaAsignada = false;

    private String clienteId;
    private String clienteNombre;
    private String citaId;
    private Cita citaActual;
    private boolean ignoreMenuSelection = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_cita_cliente);

        firebaseAuth = FirebaseAuth.getInstance();
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);

        initViews();
        setupBottomMenu();
        setupSpinners();
        setupPickers();
        setupButtons();

        citaId = getIntent().getStringExtra(EXTRA_CITA_ID);
        if (TextUtils.isEmpty(citaId)) {
            Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cargarClienteActual();
        observarPacientes();
        cargarCita();
    }

    private void initViews() {
        spinnerPaciente = findViewById(R.id.spinnerPaciente);
        spinnerCliente = findViewById(R.id.spinnerCliente);
        spinnerMotivo = findViewById(R.id.spinnerMotivo);
        etFecha = findViewById(R.id.etFecha);
        etHora = findViewById(R.id.etHora);
        etNotas = findViewById(R.id.etNotasAdicionales);
        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnEliminar = findViewById(R.id.btnEliminarCita);
    }

    private void setupBottomMenu() {
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        menu.setOnItemSelectedListener(item -> {
            if (ignoreMenuSelection) {
                ignoreMenuSelection = false;
                return true;
            }
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, ClienteMainActivity.class));
            } else if (id == R.id.nav_mascotas) {
                startActivity(new Intent(this, ListadoPacientesClienteActivity.class));
            } else if (id == R.id.nav_citas) {
                startActivity(new Intent(this, ClienteCitaListadoActivity.class));
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilClienteActivity.class));
            } else {
                return false;
            }
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
            return true;
        });
        menu.setSelectedItemId(R.id.nav_citas);
    }

    private void setupSpinners() {
        // Pacientes
        List<String> placeholder = new ArrayList<>();
        placeholder.add(getString(R.string.placeholder_select_paciente));
        pacienteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, placeholder);
        spinnerPaciente.setAdapter(pacienteAdapter);
        spinnerPaciente.setEnabled(false);

        // Cliente (solo el actual)
        clienteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerCliente.setAdapter(clienteAdapter);
        spinnerCliente.setEnabled(false);

        // Motivo (lista simple + motivo actual)
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

    private void setupPickers() {
        View.OnClickListener fechaListener = v -> mostrarSelectorFecha();
        etFecha.setOnClickListener(fechaListener);
        ImageView iconFecha = findViewById(R.id.iconFecha);
        if (iconFecha != null) {
            iconFecha.setOnClickListener(fechaListener);
        }

        View.OnClickListener horaListener = v -> mostrarSelectorHora();
        etHora.setOnClickListener(horaListener);
        ImageView iconHora = findViewById(R.id.iconHora);
        if (iconHora != null) {
            iconHora.setOnClickListener(horaListener);
        }
    }

    private void setupButtons() {
        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCancelar.setOnClickListener(v -> finish());
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
        ImageView imgCierre = findViewById(R.id.imgCierre);
        if (imgCierre != null) {
            imgCierre.setOnClickListener(v -> finish());
        }
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
        String email = user.getEmail();
        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    clienteId = usuario.getUid();
                    clienteNombre = construirNombreCompleto(usuario);
                    poblarSpinnerCliente();
                    actualizarPacientesDelCliente();
                }
            });
        } else {
            usuarioViewModel.getUsuario(clienteId).observe(this, usuario -> {
                if (usuario != null) {
                    clienteNombre = construirNombreCompleto(usuario);
                    poblarSpinnerCliente();
                    actualizarPacientesDelCliente();
                }
            });
        }
        poblarSpinnerCliente();
    }

    private void poblarSpinnerCliente() {
        if (clienteAdapter == null) return;
        clienteAdapter.clear();
        if (!TextUtils.isEmpty(clienteNombre)) {
            clienteAdapter.add(clienteNombre);
        } else {
            clienteAdapter.add(getString(R.string.text_cliente_sin_nombre));
        }
        clienteAdapter.notifyDataSetChanged();
        spinnerCliente.setSelection(0);
    }

    private void observarPacientes() {
        pacienteViewModel.getAllPacientes().observe(this, lista -> {
            todosLosPacientes.clear();
            if (lista != null) {
                todosLosPacientes.addAll(lista);
            }
            actualizarPacientesDelCliente();
        });
    }

    private void actualizarPacientesDelCliente() {
        if (pacienteAdapter == null) return;
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.placeholder_select_paciente));
        pacientesDelCliente.clear();

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
        spinnerPaciente.setEnabled(!pacientesDelCliente.isEmpty());

        if (citaActual != null) {
            preseleccionarPaciente(citaActual.getPacienteId());
        }
    }

    private void cargarCita() {
        citaViewModel.getCita(citaId).observe(this, cita -> {
            if (cita == null) {
                Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            citaActual = cita;
            clienteId = cita.getClienteId();
            clienteNombre = cita.getClienteNombre();
            poblarSpinnerCliente();

            etFecha.setText(cita.getFecha());
            etHora.setText(cita.getHora());
            etNotas.setText(cita.getNotas());

            if (!TextUtils.isEmpty(cita.getFecha())) {
                try {
                    fechaSeleccionada.setTime(formatoFecha.parse(cita.getFecha()));
                    fechaAsignada = true;
                } catch (ParseException ignored) {
                }
            }
            if (!TextUtils.isEmpty(cita.getHora())) {
                try {
                    String[] partes = cita.getHora().split(":");
                    if (partes.length >= 2) {
                        fechaSeleccionada.set(Calendar.HOUR_OF_DAY, Integer.parseInt(partes[0]));
                        fechaSeleccionada.set(Calendar.MINUTE, Integer.parseInt(partes[1]));
                        horaAsignada = true;
                    }
                } catch (Exception ignored) {
                }
            }

            preseleccionarPaciente(cita.getPacienteId());
            preseleccionarMotivo(cita.getMotivo());
        });
    }

    private void preseleccionarPaciente(String pacienteId) {
        if (TextUtils.isEmpty(pacienteId) || pacientesDelCliente.isEmpty()) {
            return;
        }
        for (int i = 0; i < pacientesDelCliente.size(); i++) {
            if (pacienteId.equals(pacientesDelCliente.get(i).getId())) {
                spinnerPaciente.setSelection(i + 1);
                break;
            }
        }
    }

    private void preseleccionarMotivo(String motivo) {
        if (TextUtils.isEmpty(motivo) || spinnerMotivo == null || motivoAdapter == null) {
            return;
        }
        int index = motivoAdapter.getPosition(motivo);
        if (index >= 0) {
            spinnerMotivo.setSelection(index);
        } else {
            motivoAdapter.add(motivo);
            motivoAdapter.notifyDataSetChanged();
            spinnerMotivo.setSelection(motivoAdapter.getPosition(motivo));
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

    private void guardarCambios() {
        if (citaActual == null) {
            Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_LONG).show();
            return;
        }
        Paciente paciente = obtenerPacienteSeleccionado();
        if (paciente == null) {
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

        btnGuardar.setEnabled(false);
        btnGuardar.setText(R.string.btn_guardando);

        long timestamp = (fechaAsignada && horaAsignada)
                ? fechaSeleccionada.getTimeInMillis()
                : citaActual.getFechaHoraTimestamp();

        citaActual.setPacienteId(paciente.getId());
        citaActual.setPacienteNombre(paciente.getNombre());
        citaActual.setClienteId(clienteId);
        citaActual.setClienteNombre(clienteNombre);
        citaActual.setFecha(fecha);
        citaActual.setHora(hora);
        citaActual.setMotivo(motivo);
        citaActual.setNotas(notas);
        citaActual.setFechaHoraTimestamp(timestamp);

        citaViewModel.update(citaActual);
        Toast.makeText(this, R.string.msg_cita_actualizada, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarEliminacion() {
        if (citaActual == null) return;

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_confirm_delete_patient, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        if (tvTitle != null) {
            tvTitle.setText(R.string.confirm_eliminar_cita_title);
        }
        if (tvMessage != null) {
            tvMessage.setText(R.string.confirm_eliminar_cita_message);
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.90f);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            citaViewModel.delete(citaActual);
            Toast.makeText(this, R.string.msg_cita_eliminada, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private Paciente obtenerPacienteSeleccionado() {
        int position = spinnerPaciente.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= pacientesDelCliente.size()) {
            return null;
        }
        return pacientesDelCliente.get(position - 1);
    }

    private String obtenerMotivoSeleccionado() {
        Object sel = spinnerMotivo.getSelectedItem();
        return sel != null ? sel.toString().trim() : "";
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
