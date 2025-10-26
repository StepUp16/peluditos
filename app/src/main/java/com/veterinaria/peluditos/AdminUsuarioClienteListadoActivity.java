package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AdminUsuarioClienteListadoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_cliente_listado);

        // Configurar el botón flotante para agregar cliente
        FloatingActionButton fabAddClient = findViewById(R.id.fabAddClient);
        fabAddClient.setOnClickListener(view -> {
            // Iniciar la actividad AdminUsuarioNuevo
            Intent intent = new Intent(AdminUsuarioClienteListadoActivity.this, AdminUsuarioNuevo.class);
            startActivity(intent);
            // Cerrar esta actividad
            finish();
        });

        // Configurar el botón de retroceso
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Aquí puedes agregar la lógica para cargar los clientes en el RecyclerView
    }
}
