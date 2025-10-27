package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;

public class admin_home extends AppCompatActivity {

    private Button btnVerClientes;
    private Button btnVerPacientes;
    private Button btnCrearCita;

    // Elementos del menú inferior
    private LinearLayout iconHome, iconCitas, iconPacientes, iconClientes, iconPerfil;

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

        // Inicializar vistas
        initViews();
        setupListeners();
    }

    private void initViews() {
        // Botones principales
        btnVerClientes = findViewById(R.id.btnVerClientes);
        btnVerPacientes = findViewById(R.id.btnVerPacientes);
        btnCrearCita = findViewById(R.id.btnCrearCita);

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

    private void setupBottomMenuListeners() {
        iconHome.setOnClickListener(v -> {
            // Ya estamos en Home, no hacer nada o refrescar
        });

        iconCitas.setOnClickListener(v -> {
            // Navegar a listado de citas (cuando esté implementado)
            // Intent intent = new Intent(admin_home.this, AdminCitaListadoActivity.class);
            // startActivity(intent);
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
}
