package com.veterinaria.peluditos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class admin_cita_editar extends AppCompatActivity {

    public static final String EXTRA_CITA_ID = admin_cita_nueva.EXTRA_CITA_ID;

    private Spinner spinnerPaciente;
    private Spinner spinnerCliente;
    private Spinner spinnerEstado;
    private EditText etFecha;
    private EditText etHora;
    private EditText etMotivo;
    private TextInputEditText etNotas;
    private Button btnGuardarCambios;
    private Button btnCancelar;
    private TextView btnEliminarCita;
    private ImageButton btnBack;
    private ImageView iconFecha;
    private ImageView iconHora;

    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminCitaViewModel citaViewModel;

    private final List<Paciente> pacientes = new ArrayList<>();
    private final List<Paciente> pacientesFiltrados = new ArrayList<>();
    private final List<Usuario> clientes = new ArrayList<>();
    private ArrayAdapter<String> pacienteAdapter;
    private ArrayAdapter<String> clienteAdapter;

    private final Calendar fechaSeleccionada = Calendar.getInstance();
    private boolean fechaAsignada = false;
    private boolean horaAsignada = false;

    private String citaId;
    private Cita citaActual;
    private String clienteSeleccionadoId;
    private String clienteIdPendiente;
    private String pacienteIdPendiente;
    private boolean clientePreseleccionado = false;
    private boolean pacientePreseleccionado = false;

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_cita_editar);

        citaId = getIntent() != null ? getIntent().getStringExtra(EXTRA_CITA_ID) : null;
        if (TextUtils.isEmpty(citaId)) {
            Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinners();
        setupEstadoSpinner();
        setupPickers();
        setupBottomMenu();

        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);

        observePacientes();
        observeClientes();
        cargarCitaSeleccionada();

        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
        btnCancelar.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
        btnEliminarCita.setOnClickListener(v -> confirmarEliminarCita());
    }

    private void initViews() {
        spinnerPaciente = findViewById(R.id.spinnerPaciente);
        spinnerCliente = findViewById(R.id.spinnerCliente);
        spinnerEstado = findViewById(R.id.spinnerEstado);
        etFecha = findViewById(R.id.etFecha);
        etHora = findViewById(R.id.etHora);
        etMotivo = findViewById(R.id.etMotivo);
        etNotas = findViewById(R.id.etNotas);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnEliminarCita = findViewById(R.id.btnEliminarCita);
        btnBack = findViewById(R.id.btnBack);
        iconFecha = findViewById(R.id.iconFecha);
        iconHora = findViewById(R.id.iconHora);

        etFecha.setInputType(InputType.TYPE_NULL);
        etHora.setInputType(InputType.TYPE_NULL);
        btnGuardarCambios.setEnabled(false);
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
        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                estados
        );
        spinnerEstado.setAdapter(estadoAdapter);
    }

    private void setupPickers() {
        View.OnClickListener fechaListener = v -> mostrarDatePicker();
        etFecha.setOnClickListener(fechaListener);
        iconFecha.setOnClickListener(fechaListener);

        View.OnClickListener horaListener = v -> mostrarTimePicker();
        etHora.setOnClickListener(horaListener);
        iconHora.setOnClickListener(horaListener);
    }

    private void setupBottomMenu() {
        ImageView iconHome = findViewById(R.id.iconHome);
        ImageView iconCitas = findViewById(R.id.iconCitas);
        ImageView iconPacientes = findViewById(R.id.iconPacientes);
        ImageView iconClientes = findViewById(R.id.iconClientes);
        ImageView iconPerfil = findViewById(R.id.iconPerfil);

        if (iconHome != null && iconHome.getParent() instanceof View) {
            View homeView = (View) iconHome.getParent();
            homeView.setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_home.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconCitas != null && iconCitas.getParent() instanceof View) {
            View citasView = (View) iconCitas.getParent();
            citasView.setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_cita_listado.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconPacientes != null && iconPacientes.getParent() instanceof View) {
            View pacientesView = (View) iconPacientes.getParent();
            pacientesView.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPacienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconClientes != null && iconClientes.getParent() instanceof View) {
            View clientesView = (View) iconClientes.getParent();
            clientesView.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminUsuarioClienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconPerfil != null && iconPerfil.getParent() instanceof View) {
            View perfilView = (View) iconPerfil.getParent();
            perfilView.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPerfil.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }
    }

    private void startActivityWithAnimation(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void observePacientes() {
        pacienteViewModel.getAllPacientes().observe(this, pacientesLista -> {
            pacientes.clear();
            if (pacientesLista != null) {
                pacientes.addAll(pacientesLista);
            }
            actualizarPacientesParaCliente(clienteSeleccionadoId);
            seleccionarPacientePendiente();
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
            seleccionarClientePendiente();
        });
    }

    private void cargarCitaSeleccionada() {
        citaViewModel.getCita(citaId).observe(this, cita -> {
            if (cita == null) {
                Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            citaActual = cita;
            clienteIdPendiente = cita.getClienteId();
            pacienteIdPendiente = cita.getPacienteId();
            clienteSeleccionadoId = cita.getClienteId();
            llenarCamposConCita(cita);
            btnGuardarCambios.setEnabled(true);
        });
    }

    private void llenarCamposConCita(Cita cita) {
        etMotivo.setText(cita.getMotivo());
        etNotas.setText(cita.getNotas());
        etFecha.setText(cita.getFecha());
        etHora.setText(cita.getHora());

        long timestamp = cita.getFechaHoraTimestamp();
        if (timestamp > 0) {
            fechaSeleccionada.setTimeInMillis(timestamp);
            fechaAsignada = true;
            horaAsignada = true;
        } else {
            fechaAsignada = !TextUtils.isEmpty(cita.getFecha());
            horaAsignada = !TextUtils.isEmpty(cita.getHora());
        }
        actualizarCamposFechaHora();
        seleccionarEstado(cita.getEstado());
        seleccionarClientePendiente();
        seleccionarPacientePendiente();
    }

    private void seleccionarEstado(String estado) {
        if (spinnerEstado.getAdapter() == null || TextUtils.isEmpty(estado)) {
            return;
        }
        String[] estados = getResources().getStringArray(R.array.cita_estados_array);
        for (int i = 0; i < estados.length; i++) {
            if (estado.equalsIgnoreCase(estados[i])) {
                spinnerEstado.setSelection(i + 1);
                break;
            }
        }
    }

    private void seleccionarClientePendiente() {
        if (clientePreseleccionado || TextUtils.isEmpty(clienteIdPendiente) || clientes.isEmpty()) {
            return;
        }
        for (int i = 0; i < clientes.size(); i++) {
            if (clienteIdPendiente.equals(clientes.get(i).getUid())) {
                spinnerCliente.setSelection(i + 1);
                clientePreseleccionado = true;
                break;
            }
        }
    }

    private void seleccionarPacientePendiente() {
        if (pacientePreseleccionado || TextUtils.isEmpty(pacienteIdPendiente) || pacientesFiltrados.isEmpty()) {
            return;
        }
        for (int i = 0; i < pacientesFiltrados.size(); i++) {
            if (pacienteIdPendiente.equals(pacientesFiltrados.get(i).getId())) {
                spinnerPaciente.setSelection(i + 1);
                pacientePreseleccionado = true;
                break;
            }
        }
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
        seleccionarPacientePendiente();
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

    private void guardarCambios() {
        if (citaActual == null) {
            Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_SHORT).show();
            return;
        }

        Paciente pacienteSeleccionado = obtenerPacienteSeleccionado();
        Usuario clienteSeleccionado = obtenerClienteSeleccionado();
        String fecha = etFecha.getText().toString().trim();
        String hora = etHora.getText().toString().trim();
        String motivo = etMotivo.getText().toString().trim();
        String notas = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";
        String estado = spinnerEstado.getSelectedItem() != null
                ? spinnerEstado.getSelectedItem().toString()
                : getString(R.string.cita_estado_pendiente);

        if (!validarCampos(pacienteSeleccionado, clienteSeleccionado, fecha, hora, motivo, estado)) {
            return;
        }

        btnGuardarCambios.setEnabled(false);

        long fechaHoraTimestamp = fechaAsignada && horaAsignada
                ? fechaSeleccionada.getTimeInMillis()
                : citaActual.getFechaHoraTimestamp();

        citaActual.setPacienteId(pacienteSeleccionado != null ? pacienteSeleccionado.getId() : "");
        citaActual.setPacienteNombre(pacienteSeleccionado != null ? pacienteSeleccionado.getNombre() : "");
        citaActual.setClienteId(clienteSeleccionado != null ? clienteSeleccionado.getUid() : "");
        citaActual.setClienteNombre(clienteSeleccionado != null
                ? clienteSeleccionado.getNombre() + " " + clienteSeleccionado.getApellido()
                : "");
        citaActual.setFecha(fecha);
        citaActual.setHora(hora);
        citaActual.setMotivo(motivo);
        citaActual.setNotas(notas);
        citaActual.setEstado(estado);
        citaActual.setFechaHoraTimestamp(fechaHoraTimestamp);

        citaViewModel.update(citaActual);
        Toast.makeText(this, R.string.msg_cita_actualizada, Toast.LENGTH_SHORT).show();
        finish();
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

    private void confirmarEliminarCita() {
        if (citaActual == null) {
            Toast.makeText(this, R.string.error_cita_no_encontrada, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_confirm_delete_patient, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        tvTitle.setText(R.string.confirm_eliminar_cita_title);
        tvMessage.setText(R.string.confirm_eliminar_cita_message);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            eliminarCita();
            if (isNetworkAvailable()) {
                Toast.makeText(this, R.string.msg_cita_eliminada, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.msg_cita_eliminada_pendiente, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void eliminarCita() {
        citaViewModel.delete(citaActual);
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
