package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class admin_home extends AppCompatActivity {

    private Button btnVerClientes;
    private Button btnVerPacientes;
    private Button btnCrearCita; // Nuevo botón para crear citas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_home);

        // Lógica del botón Ver Clientes
        btnVerClientes = findViewById(R.id.btnVerClientes);
        btnVerClientes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(admin_home.this, AdminUsuarioClienteListadoActivity.class);
                startActivity(intent);
            }
        });

        // Lógica del botón Ver Pacientes
        btnVerPacientes = findViewById(R.id.btnVerPacientes);
        btnVerPacientes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(admin_home.this, AdminPacienteListadoActivity.class);
                startActivity(intent);
            }
        });

        // Lógica del botón Crear Cita
        btnCrearCita = findViewById(R.id.btnCrearCita);
        btnCrearCita.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(admin_home.this, admin_cita_nueva.class);
                startActivity(intent);
            }
        });
    }
}
