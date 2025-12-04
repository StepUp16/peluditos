package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.adapters.AlertaClienteAdapter;
import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.Usuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClienteMainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminCitaViewModel citaViewModel;
    private ShapeableImageView imgAvatarCliente;
    private MaterialCardView cardProximaCita;
    private TextView txtFechaCita;
    private TextView txtMotivoCita;
    private TextView txtNombreDoctor;
    private TextView tvProximasVacio;
    private RecyclerView rvAlertas;
    private TextView tvAlertasVacio;
    private MaterialButton btnVerAlertas;
    private AlertaClienteAdapter alertaAdapter;
    private final List<Cita> cacheCitas = new ArrayList<>();
    private String clienteId;
    private final SimpleDateFormat fechaDisplayFormat =
            new SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault());
    private final SimpleDateFormat horaDisplayFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_cliente);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);
        imgAvatarCliente = findViewById(R.id.imgAvatarCliente);

        Toast.makeText(this, R.string.toast_cliente_bienvenida, Toast.LENGTH_LONG).show();
        configurarSaludo();
        configurarAccionesPrincipales();
        configurarResumenCitas();
        configurarMenuInferior();
    }

    private void configurarSaludo() {
        TextView txtMensaje = findViewById(R.id.txtMensajeBenvenida);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            txtMensaje.setText(R.string.text_saludo_generico);
            actualizarAvatar(null);
            mostrarEstadoProximas(false);
            return;
        }

        clienteId = currentUser.getUid();
        String nombreFallback = obtenerNombreDesdeFirebase(currentUser);
        txtMensaje.setText(getString(R.string.text_saludo_cliente, nombreFallback));
        actualizarAvatar(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        actualizarTarjetaProximaCita();

        String email = currentUser.getEmail();
        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    txtMensaje.setText(getString(R.string.text_saludo_cliente,
                            construirNombreCompleto(usuario)));
                    actualizarAvatar(usuario.getFotoUrl());
                    clienteId = usuario.getUid();
                    actualizarTarjetaProximaCita();
                }
            });
        } else {
            usuarioViewModel.getUsuario(currentUser.getUid()).observe(this, usuario -> {
                if (usuario != null) {
                    txtMensaje.setText(getString(R.string.text_saludo_cliente,
                            construirNombreCompleto(usuario)));
                    actualizarAvatar(usuario.getFotoUrl());
                    clienteId = usuario.getUid();
                    actualizarTarjetaProximaCita();
                }
            });
        }
    }

    private void configurarAccionesPrincipales() {
        Button btnAgregarMascota = findViewById(R.id.btnAgregarMascota);
        Button btnSolicitarCita = findViewById(R.id.btnSolicitarCita);

        btnAgregarMascota.setOnClickListener(v -> abrirNuevoPaciente());
        btnSolicitarCita.setOnClickListener(v -> abrirNuevaCita());
    }

    private void configurarResumenCitas() {
        cardProximaCita = findViewById(R.id.cardProximaCita);
        txtFechaCita = findViewById(R.id.txtFechaCita);
        txtMotivoCita = findViewById(R.id.txtMotivoCita);
        txtNombreDoctor = findViewById(R.id.txtNombreDoctor);
        tvProximasVacio = findViewById(R.id.tvProximasVacio);
        rvAlertas = findViewById(R.id.rvAlertas);
        tvAlertasVacio = findViewById(R.id.tvAlertasVacio);
        btnVerAlertas = findViewById(R.id.btnVerAlertas);

        if (cardProximaCita != null) {
            cardProximaCita.setOnClickListener(v -> abrirListadoCitas());
        }
        if (btnVerAlertas != null) {
            btnVerAlertas.setOnClickListener(v -> abrirListadoCitas());
        }

        if (rvAlertas != null) {
            rvAlertas.setLayoutManager(new LinearLayoutManager(this));
            alertaAdapter = new AlertaClienteAdapter();
            rvAlertas.setAdapter(alertaAdapter);
        }

        if (citaViewModel != null) {
            citaViewModel.getAllCitas().observe(this, citas -> {
                cacheCitas.clear();
                if (citas != null) {
                    cacheCitas.addAll(citas);
                }
                actualizarTarjetaProximaCita();
                actualizarAlertasRecientes();
            });
        }
    }

    private void configurarMenuInferior() {
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        menu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_mascotas) {
                abrirListadoPacientes();
                return true;
            } else if (itemId == R.id.nav_citas) {
                abrirListadoCitas();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                abrirPerfil();
                return true;
            }
            return false;
        });
        menu.setSelectedItemId(R.id.nav_home);
    }

    private void abrirNuevoPaciente() {
        Intent intent = new Intent(this, NuevoPacienteClienteActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void abrirListadoPacientes() {
        Intent intent = new Intent(this, ListadoPacientesClienteActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void abrirListadoCitas() {
        Intent intent = new Intent(this, ClienteCitaListadoActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void abrirNuevaCita() {
        Intent intent = new Intent(this, NuevaCitaClienteActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void abrirPerfil() {
        Intent intent = new Intent(this, PerfilClienteActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void cerrarSesion(View view) {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_superior, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.mapa_google) {
            Intent intent = new Intent(this, maps_google.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_logout) {
            mAuth.signOut();
            sessionManager.logoutUser();

            Intent intent = new Intent(ClienteMainActivity.this, Login_Peluditos.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (itemId == R.id.perfil) {
            abrirPerfil();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void actualizarTarjetaProximaCita() {
        if (cardProximaCita == null) {
            return;
        }
        if (TextUtils.isEmpty(clienteId)) {
            mostrarEstadoProximas(false);
            actualizarAlertasRecientes();
            return;
        }

        Cita proxima = obtenerProximaCita();
        if (proxima == null) {
            mostrarEstadoProximas(false);
            actualizarAlertasRecientes();
            return;
        }

        mostrarEstadoProximas(true);
        long timestamp = proxima.getFechaHoraTimestamp();
        String fechaTexto = proxima.getFecha();
        String horaTexto = proxima.getHora();
        if (timestamp > 0) {
            Date date = new Date(timestamp);
            fechaTexto = fechaDisplayFormat.format(date);
            horaTexto = horaDisplayFormat.format(date);
        }
        if (TextUtils.isEmpty(fechaTexto)) {
            fechaTexto = "--";
        }
        if (horaTexto == null) {
            horaTexto = "";
        }
        if (txtFechaCita != null) {
            if (TextUtils.isEmpty(horaTexto)) {
                txtFechaCita.setText(fechaTexto);
            } else {
                txtFechaCita.setText(getString(R.string.cita_cliente_fecha_hora, fechaTexto, horaTexto));
            }
        }

        if (txtMotivoCita != null) {
            String motivo = TextUtils.isEmpty(proxima.getMotivo())
                    ? getString(R.string.cita_cliente_sin_motivo)
                    : proxima.getMotivo();
            String paciente = proxima.getPacienteNombre();
            if (!TextUtils.isEmpty(paciente)) {
                motivo = getString(R.string.text_cliente_home_motivo_paciente, motivo, paciente);
            }
            txtMotivoCita.setText(motivo);
        }

        if (txtNombreDoctor != null) {
            String estado = TextUtils.isEmpty(proxima.getEstado())
                    ? getString(R.string.cita_estado_pendiente)
                    : proxima.getEstado();
            txtNombreDoctor.setText(getString(R.string.text_cliente_home_estado, estado));
        }
    }

    private void actualizarAlertasRecientes() {
        if (alertaAdapter == null) {
            mostrarEstadoAlertas(false);
            return;
        }
        if (TextUtils.isEmpty(clienteId)) {
            alertaAdapter.setAlertas(new ArrayList<>());
            mostrarEstadoAlertas(false);
            return;
        }

        List<Cita> propias = new ArrayList<>();
        for (Cita cita : cacheCitas) {
            if (clienteId.equals(cita.getClienteId())) {
                propias.add(cita);
            }
        }
        if (propias.isEmpty()) {
            alertaAdapter.setAlertas(new ArrayList<>());
            mostrarEstadoAlertas(false);
            return;
        }
        Collections.sort(propias, (c1, c2) ->
                Long.compare(c2.getTimestampModificacion(), c1.getTimestampModificacion()));

        List<AlertaClienteAdapter.AlertaItem> items = new ArrayList<>();
        for (int i = 0; i < propias.size() && items.size() < 3; i++) {
            Cita cita = propias.get(i);
            String estado = TextUtils.isEmpty(cita.getEstado())
                    ? getString(R.string.cita_estado_pendiente)
                    : cita.getEstado();
            String paciente = !TextUtils.isEmpty(cita.getPacienteNombre())
                    ? cita.getPacienteNombre()
                    : getString(R.string.text_cliente_mascota_sin_nombre);
            String titulo = getString(R.string.text_cliente_home_estado, estado) + " - " + paciente;
            String motivo = TextUtils.isEmpty(cita.getMotivo())
                    ? getString(R.string.cita_cliente_sin_motivo)
                    : cita.getMotivo();
            String descripcion = getString(R.string.text_cliente_home_motivo_paciente, motivo, paciente);
            long ts = cita.getTimestampModificacion();
            if (ts <= 0) {
                ts = cita.getFechaHoraTimestamp();
            }
            items.add(new AlertaClienteAdapter.AlertaItem(
                    titulo,
                    descripcion,
                    ts,
                    obtenerIconoAlerta(estado)
            ));
        }
        alertaAdapter.setAlertas(items);
        mostrarEstadoAlertas(!items.isEmpty());
    }

    private Cita obtenerProximaCita() {
        if (cacheCitas.isEmpty() || TextUtils.isEmpty(clienteId)) {
            return null;
        }
        List<Cita> propias = new ArrayList<>();
        for (Cita cita : cacheCitas) {
            if (clienteId.equals(cita.getClienteId())) {
                propias.add(cita);
            }
        }
        if (propias.isEmpty()) {
            return null;
        }

        long ahora = System.currentTimeMillis();
        Cita candidata = null;
        for (Cita cita : propias) {
            long ts = cita.getFechaHoraTimestamp();
            if (ts <= 0) {
                continue;
            }
            if (ts >= ahora && (candidata == null || ts < candidata.getFechaHoraTimestamp())) {
                candidata = cita;
            }
        }
        if (candidata == null) {
            Collections.sort(propias, (c1, c2) ->
                    Long.compare(c1.getFechaHoraTimestamp(), c2.getFechaHoraTimestamp()));
            candidata = propias.get(0);
        }
        return candidata;
    }

    private void mostrarEstadoProximas(boolean hayCita) {
        if (cardProximaCita != null) {
            cardProximaCita.setVisibility(hayCita ? View.VISIBLE : View.GONE);
        }
        if (tvProximasVacio != null) {
            tvProximasVacio.setVisibility(hayCita ? View.GONE : View.VISIBLE);
        }
    }

    private void mostrarEstadoAlertas(boolean hayAlertas) {
        if (rvAlertas != null) {
            rvAlertas.setVisibility(hayAlertas ? View.VISIBLE : View.GONE);
        }
        if (tvAlertasVacio != null) {
            tvAlertasVacio.setVisibility(hayAlertas ? View.GONE : View.VISIBLE);
        }
    }

    private int obtenerIconoAlerta(String estado) {
        if (estado == null) {
            return R.drawable.icono_alarm;
        }
        switch (estado.toLowerCase(Locale.getDefault())) {
            case "confirmada":
            case "completada":
                return R.drawable.icono_citas;
            case "pospuesta":
            case "cancelada":
                return R.drawable.icono_notificacion;
            default:
                return R.drawable.icono_alarm;
        }
    }

    private void actualizarAvatar(String fotoUrl) {
        if (imgAvatarCliente == null) {
            return;
        }

        // 1. EL TRUCO DEL PLACEHOLDER:
        // Le decimos a Glide: "Mientras procesas, NO borres lo que ya tiene la imagen".
        android.graphics.drawable.Drawable imagenActual = imgAvatarCliente.getDrawable();

        // Limpieza defensiva solo si no hay imagen previa
        if (imagenActual == null) {
            imgAvatarCliente.setImageResource(R.drawable.icono_perfil);
        }

        if (TextUtils.isEmpty(fotoUrl)) {
            imgAvatarCliente.setImageResource(R.drawable.icono_perfil);
            return;
        }

        if (fotoUrl.startsWith("http")) {
            // Legacy URL (broken/paid) - Show placeholder immediately
            imgAvatarCliente.setImageResource(R.drawable.icono_perfil);
        } else {
            try {
                byte[] imageByteArray = android.util.Base64.decode(fotoUrl, android.util.Base64.DEFAULT);
                Glide.with(this)
                        .asBitmap()
                        .load(imageByteArray)
                        .placeholder(imagenActual) // Mantiene la imagen vieja mientras carga la nueva
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache decoded image
                        .dontAnimate()
                        .centerCrop()
                        .into(imgAvatarCliente);
            } catch (IllegalArgumentException e) {
                imgAvatarCliente.setImageResource(R.drawable.icono_perfil);
            }
        }
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
}
