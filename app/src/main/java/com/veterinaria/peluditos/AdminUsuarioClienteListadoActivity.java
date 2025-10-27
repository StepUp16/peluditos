package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.veterinaria.peluditos.adapters.ClienteAdapter;

public class AdminUsuarioClienteListadoActivity extends AppCompatActivity {
    private ClienteAdapter adapter;
    private AdminUsuarioViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_cliente_listado);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);

        // Configurar RecyclerView y su adaptador
        setupRecyclerView();

        // Configurar el botón flotante para agregar cliente
        FloatingActionButton fabAddClient = findViewById(R.id.fabAddClient);
        fabAddClient.setOnClickListener(view -> {
            Intent intent = new Intent(AdminUsuarioClienteListadoActivity.this, AdminUsuarioNuevo.class);
            startActivity(intent);
        });

        // Configurar el botón de retroceso
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Observar cambios en la base de datos local
        // El repositorio maneja automáticamente la sincronización con Firestore
        observeLocalUsers();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewClientes);
        adapter = new ClienteAdapter();

        // Configurar el listener para eliminar usuarios
        adapter.setOnDeleteClickListener(usuario -> {
            // Mostrar diálogo de confirmación
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Eliminar Usuario")
                    .setMessage("¿Estás seguro que deseas eliminar a " + usuario.getNombre() + " " + usuario.getApellido() + "?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        // Eliminar usuario
                        viewModel.deleteUsuario(usuario, this);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Configurar el listener para editar usuarios
        adapter.setOnEditClickListener(usuario -> {
            Intent intent = new Intent(this, AdminUsuarioEditar.class);
            intent.putExtra("USUARIO_ID", usuario.getUid());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private void observeLocalUsers() {
        viewModel.getAllUsuarios().observe(this, usuarios -> {
            if (usuarios != null) {
                adapter.setUsuarios(usuarios);
            }
        });
    }
}
