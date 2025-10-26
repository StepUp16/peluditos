package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.GoogleMap;

public class admin_home extends AppCompatActivity {

    private Button btnVerClientes;
    private Button btnVerPacientes;
    private Button btnCrearCita;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_home);

        Toolbar toolbar = findViewById(R.id.toolbar); 
        setSupportActionBar(toolbar);

        // Lógica de los botones de acceso rápido
        btnVerClientes = findViewById(R.id.btnVerClientes);
        btnVerClientes.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminUsuarioClienteListadoActivity.class);
            startActivity(intent);
        });

        btnVerPacientes = findViewById(R.id.btnVerPacientes);
        btnVerPacientes.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, AdminPacienteListadoActivity.class);
            startActivity(intent);
        });

        btnCrearCita = findViewById(R.id.btnCrearCita);
        btnCrearCita.setOnClickListener(v -> {
            Intent intent = new Intent(admin_home.this, admin_cita_nueva.class);
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
            // CORREGIDO: Iniciar la actividad correcta (maps_google.class) y cerrar la actual
            Intent intent = new Intent(this, maps_google.class);
            startActivity(intent);
            finish(); // Cierra admin_home
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
