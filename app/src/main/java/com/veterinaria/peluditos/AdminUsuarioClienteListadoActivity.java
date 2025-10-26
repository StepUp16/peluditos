package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.veterinaria.peluditos.adapters.ClienteAdapter;
import com.veterinaria.peluditos.data.Usuario;

import java.util.ArrayList;
import java.util.List;

public class AdminUsuarioClienteListadoActivity extends AppCompatActivity {
    private ClienteAdapter adapter;
    private AdminUsuarioViewModel viewModel;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_cliente_listado);

        // Inicializar ViewModel y Firestore
        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        firestore = FirebaseFirestore.getInstance();

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
        observeLocalUsers();

        // Cargar usuarios desde Firestore
        loadFirestoreUsers();
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
                        // Eliminar usuario de todas las bases de datos
                        viewModel.deleteUsuario(usuario, this);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
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

    private void loadFirestoreUsers() {
        firestore.collection("usuarios")
                .whereEqualTo("rol", "cliente")  // Filtrar solo usuarios con rol "cliente"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Usuario> usuariosFirestore = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String uid = document.getId();
                        String nombre = document.getString("nombre");
                        String apellido = document.getString("apellido");
                        String email = document.getString("email");
                        String telefono = document.getString("telefono");
                        String dui = document.getString("dui");
                        String direccion = document.getString("direccion");
                        String rol = document.getString("rol");

                        Usuario usuario = new Usuario(uid, nombre, apellido, email,
                                                   telefono, dui, direccion, rol);
                        usuariosFirestore.add(usuario);
                    }
                    // Actualizar la base de datos local con los datos de Firestore
                    for (Usuario usuario : usuariosFirestore) {
                        viewModel.insert(usuario);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar usuarios: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando la actividad vuelve a primer plano
        loadFirestoreUsers();
    }
}
