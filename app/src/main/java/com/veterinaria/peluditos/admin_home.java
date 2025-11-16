package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.veterinaria.peluditos.data.Cita;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class admin_home extends AppCompatActivity {

    private Button btnVerClientes;
    private Button btnVerPacientes;
    private Button btnCrearCita;

    // Elementos del menú inferior
    private LinearLayout iconHome, iconCitas, iconPacientes, iconClientes, iconPerfil;
    private LinearLayout layoutCitas, layoutNotificaciones;
    private AdminCitaViewModel citaViewModel;
    private LayoutInflater inflater;

    private final SimpleDateFormat fechaFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat horaFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // 1. Declarar FirebaseAuth y SessionManager
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        // 2. Inicializar FirebaseAuth y SessionManager
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        inflater = LayoutInflater.from(this);

        // Inicializar vistas
        initViews();
        setupListeners();
        setupDashboardSections();
    }

    private void initViews() {
        // Botones principales
        btnVerClientes = findViewById(R.id.btnVerClientes);
        btnVerPacientes = findViewById(R.id.btnVerPacientes);
        btnCrearCita = findViewById(R.id.btnCrearCita);

        layoutCitas = findViewById(R.id.layoutCitas);
        layoutNotificaciones = findViewById(R.id.layoutNotificaciones);

        // Menú inferior - obtener los LinearLayouts padre de cada icono
        iconHome = (LinearLayout) findViewById(R.id.iconHome).getParent();
        iconCitas = (LinearLayout) findViewById(R.id.iconCitas).getParent();
        iconPacientes = (LinearLayout) findViewById(R.id.iconPacientes).getParent();
        iconClientes = (LinearLayout) findViewById(R.id.iconClientes).getParent();
        iconPerfil = (LinearLayout) findViewById(R.id.iconPerfil).getParent();
    }

    private void setupListeners() {
        // Lógica de los botones de acceso rápido
        btnVerClientes.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminUsuarioClienteListadoActivity.class);
            startActivity(intent);
        });

        btnVerPacientes.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminPacienteListadoActivity.class);
            startActivity(intent);
        });

        btnCrearCita.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, admin_cita_nueva.class);
            startActivity(intent);
        });

        // Configurar listeners del menú inferior
        setupBottomMenuListeners();
    }

    private void setupDashboardSections() {
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);
        citaViewModel.getAllCitas().observe(this, citas -> {
            renderProximasCitas(citas);
            renderNotificaciones(citas);
        });
    }

    private void setupBottomMenuListeners() {
        iconHome.setOnClickListener(v -> {
            // Ya estamos en Home, no hacer nada o refrescar
        });

        iconCitas.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, admin_cita_listado.class);
            startActivity(intent);
        });

        iconPacientes.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminPacienteListadoActivity.class);
            startActivity(intent);
        });

        iconClientes.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminUsuarioClienteListadoActivity.class);
            startActivity(intent);
        });

        iconPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminPerfil.class);
            startActivity(intent);
        });
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
            // Navegación al mapa (sin cerrar esta pantalla para una mejor UX)
            Intent intent = new Intent(this, maps_google.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_logout) {
            // 3. Lógica de cierre de sesión completa
            mAuth.signOut();
            sessionManager.logoutUser();

            Intent intent = new Intent(admin_home.this, Login_Peluditos.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }else if (itemId == R.id.perfil) {
            // Navegación al perfil (sin cerrar esta pantalla para una mejor UX)
            Intent intent = new Intent(this, AdminPerfil.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void renderProximasCitas(List<Cita> citas) {
        if (layoutCitas == null) {
            return;
        }
        layoutCitas.removeAllViews();
        List<Cita> proximas = new ArrayList<>();
        if (citas != null) {
            long ahora = System.currentTimeMillis();
            for (Cita cita : citas) {
                long ts = cita.getFechaHoraTimestamp();
                if (ts > 0 && ts >= ahora) {
                    proximas.add(cita);
                }
            }
        }
        if (proximas.isEmpty()) {
            addEmptyState(layoutCitas, R.string.text_admin_home_citas_vacio);
            return;
        }
        Collections.sort(proximas, (c1, c2) ->
                Long.compare(c1.getFechaHoraTimestamp(), c2.getFechaHoraTimestamp()));
        int limite = Math.min(3, proximas.size());
        for (int i = 0; i < limite; i++) {
            Cita cita = proximas.get(i);
            String paciente = !TextUtils.isEmpty(cita.getPacienteNombre())
                    ? cita.getPacienteNombre()
                    : getString(R.string.text_cliente_mascota_sin_nombre);
            String cliente = !TextUtils.isEmpty(cita.getClienteNombre())
                    ? cita.getClienteNombre()
                    : getString(R.string.text_cliente_sin_nombre);
            String titulo = paciente + " • " + cliente;
            String fechaHora = buildFechaHoraTexto(cita);
            String estado = TextUtils.isEmpty(cita.getEstado())
                    ? getString(R.string.cita_estado_pendiente)
                    : cita.getEstado();
            String extra = getString(R.string.text_cliente_home_estado, estado);
            View card = inflater.inflate(R.layout.item_admin_home_entry, layoutCitas, false);
            bindEntryCard(card, R.drawable.icono_citas, titulo, fechaHora, extra);
            layoutCitas.addView(card);
        }
    }

    private void renderNotificaciones(List<Cita> citas) {
        if (layoutNotificaciones == null) {
            return;
        }
        layoutNotificaciones.removeAllViews();
        if (citas == null || citas.isEmpty()) {
            addEmptyState(layoutNotificaciones, R.string.text_admin_home_notificaciones_vacio);
            return;
        }
        List<Cita> recientes = new ArrayList<>(citas);
        Collections.sort(recientes, (c1, c2) -> {
            long t1 = c1.getTimestampModificacion() > 0
                    ? c1.getTimestampModificacion() : c1.getFechaHoraTimestamp();
            long t2 = c2.getTimestampModificacion() > 0
                    ? c2.getTimestampModificacion() : c2.getFechaHoraTimestamp();
            return Long.compare(t2, t1);
        });
        int limite = Math.min(3, recientes.size());
        for (int i = 0; i < limite; i++) {
            Cita cita = recientes.get(i);
            String paciente = !TextUtils.isEmpty(cita.getPacienteNombre())
                    ? cita.getPacienteNombre()
                    : getString(R.string.text_cliente_mascota_sin_nombre);
            String estado = TextUtils.isEmpty(cita.getEstado())
                    ? getString(R.string.cita_estado_pendiente)
                    : cita.getEstado();
            String titulo = estado + " • " + paciente;
            String fechaHora = buildFechaHoraTexto(cita);
            String motivo = TextUtils.isEmpty(cita.getMotivo())
                    ? getString(R.string.cita_cliente_sin_motivo)
                    : cita.getMotivo();
            View card = inflater.inflate(R.layout.item_admin_home_entry, layoutNotificaciones, false);
            bindEntryCard(card, R.drawable.icono_notificacion, titulo, fechaHora, motivo);
            layoutNotificaciones.addView(card);
        }
    }

    private void addEmptyState(LinearLayout container, int stringRes) {
        TextView tv = new TextView(this);
        tv.setText(stringRes);
        tv.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tv.setPadding(8, 8, 8, 8);
        container.addView(tv);
    }

    private String buildFechaHoraTexto(Cita cita) {
        long timestamp = cita.getFechaHoraTimestamp();
        String fechaTexto = cita.getFecha();
        String horaTexto = cita.getHora();
        if (timestamp > 0) {
            Date date = new Date(timestamp);
            fechaTexto = fechaFormat.format(date);
            horaTexto = horaFormat.format(date);
        }
        if (TextUtils.isEmpty(fechaTexto)) {
            fechaTexto = "--";
        }
        if (TextUtils.isEmpty(horaTexto)) {
            return fechaTexto;
        }
        return fechaTexto + " • " + horaTexto;
    }

    private void bindEntryCard(View card, int iconRes, String titulo, String subtitulo, String extra) {
        ImageView icon = card.findViewById(R.id.imgEntryIcon);
        TextView tvTitulo = card.findViewById(R.id.tvEntryTitle);
        TextView tvSubtitulo = card.findViewById(R.id.tvEntrySubtitle);
        TextView tvExtra = card.findViewById(R.id.tvEntryExtra);
        icon.setImageResource(iconRes);
        tvTitulo.setText(titulo);
        tvSubtitulo.setText(subtitulo);
        tvExtra.setText(extra);
    }
}
