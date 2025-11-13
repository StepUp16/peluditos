package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.veterinaria.peluditos.adapters.UsuarioAdminAdapter;
import com.veterinaria.peluditos.data.Usuario;

import java.util.Arrays;
import java.util.List;

public class AdminUsuarioListadoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private UsuarioAdminAdapter adapter;
    private SearchView searchView;
    private AdminUsuarioViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_listado);

        initViews();
        setupRecycler();
        setupListeners();

        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        observeUsuarios();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewUsuarios);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        searchView = findViewById(R.id.searchUsuarios);
    }

    private void setupRecycler() {
        adapter = new UsuarioAdminAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.setOnUsuarioActionListener(this::mostrarDialogoEliminar);
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        FloatingActionButton fabAddUser = findViewById(R.id.fabAddUser);

        btnBack.setOnClickListener(v -> finish());
        fabAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminUsuarioNuevo.class);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void observeUsuarios() {
        List<String> roles = Arrays.asList("administrador", "veterinario");
        viewModel.getUsuariosPorRoles(roles).observe(this, usuarios -> {
            adapter.setUsuarios(usuarios);
            actualizarEstadoVacio(usuarios);
        });
    }

    private void actualizarEstadoVacio(List<Usuario> usuarios) {
        boolean isEmpty = usuarios == null || usuarios.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void mostrarDialogoEliminar(@NonNull Usuario usuario) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete_patient, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        tvTitle.setText(R.string.confirm_eliminar_usuario_title);
        String nombreCompleto = (usuario.getNombre() != null ? usuario.getNombre() : "") + " " +
                (usuario.getApellido() != null ? usuario.getApellido() : "");
        String displayName = nombreCompleto.trim();
        if (displayName.isEmpty() && usuario.getEmail() != null) {
            displayName = usuario.getEmail();
        }
        tvMessage.setText(getString(R.string.confirm_eliminar_usuario_message, displayName));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                eliminarUsuario(usuario);
                dialog.dismiss();
            });
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.90f);
        }
        dialog.show();
    }

    private void eliminarUsuario(Usuario usuario) {
        try {
            viewModel.deleteUsuario(usuario, this);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_eliminar_usuario, Toast.LENGTH_SHORT).show();
        }
    }
}
