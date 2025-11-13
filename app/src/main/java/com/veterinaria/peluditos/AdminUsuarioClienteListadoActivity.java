package com.veterinaria.peluditos;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.veterinaria.peluditos.adapters.ClienteAdapter;
import com.veterinaria.peluditos.data.Usuario;

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
        adapter.setOnDeleteClickListener(usuario -> mostrarDialogoEliminar(usuario));

        // Configurar el listener para editar usuarios
        adapter.setOnEditClickListener(usuario -> {
            Intent intent = new Intent(this, AdminUsuarioEditar.class);
            intent.putExtra("USUARIO_ID", usuario.getUid());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private void mostrarDialogoEliminar(Usuario usuario) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_confirm_delete_patient, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        tvTitle.setText(R.string.confirm_eliminar_usuario_title);
        tvMessage.setText(getString(
                R.string.confirm_eliminar_usuario_message,
                (usuario.getNombre() + " " + usuario.getApellido()).trim()));

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
            viewModel.deleteUsuario(usuario, this);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void observeLocalUsers() {
        viewModel.getAllUsuarios().observe(this, usuarios -> {
            if (usuarios != null) {
                adapter.setUsuarios(usuarios);
            }
        });
    }
}
