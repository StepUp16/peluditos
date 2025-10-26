package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.veterinaria.peluditos.data.Usuario;

public class AdminUsuarioEditar extends AppCompatActivity {
    private EditText etNombre, etApellido, etCorreo, etTelefono, etDui, etDireccion;
    private Spinner spinnerRol;
    private Button btnGuardar;
    private ImageButton btnBack;
    private AdminUsuarioViewModel viewModel;
    private FirebaseFirestore firestore;
    private String usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_editar);

        // Inicializar ViewModel y Firestore
        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        firestore = FirebaseFirestore.getInstance();

        // Inicializar vistas
        initViews();
        setupSpinner();

        // Obtener el ID del usuario a editar
        usuarioId = getIntent().getStringExtra("USUARIO_ID");
        if (usuarioId != null) {
            cargarDatosUsuario();
        }

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etCorreo = findViewById(R.id.etCorreo);
        etTelefono = findViewById(R.id.etTelefono);
        etDui = findViewById(R.id.etDui);
        etDireccion = findViewById(R.id.etDireccion);
        spinnerRol = findViewById(R.id.spinnerRol);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosUsuario() {
        viewModel.getUsuario(usuarioId).observe(this, usuario -> {
            if (usuario != null) {
                etNombre.setText(usuario.getNombre());
                etApellido.setText(usuario.getApellido());
                etCorreo.setText(usuario.getEmail());
                etTelefono.setText(usuario.getTelefono());
                etDui.setText(usuario.getDui());
                etDireccion.setText(usuario.getDireccion());

                // Establecer el rol en el spinner
                ArrayAdapter adapter = (ArrayAdapter) spinnerRol.getAdapter();
                int position = adapter.getPosition(usuario.getRol());
                spinnerRol.setSelection(position);
            }
        });
    }

    private void guardarCambios() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String dui = etDui.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String rol = spinnerRol.getSelectedItem().toString();

        // Validaciones
        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() ||
            telefono.isEmpty() || dui.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuarioActualizado = new Usuario(
                usuarioId, nombre, apellido, email,
                telefono, dui, direccion, rol
        );

        // Actualizar en Firestore
        firestore.collection("usuarios")
                .document(usuarioId)
                .set(usuarioActualizado)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar en Room
                    viewModel.insert(usuarioActualizado);
                    Toast.makeText(AdminUsuarioEditar.this,
                            "Usuario actualizado exitosamente",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminUsuarioEditar.this,
                            "Error al actualizar usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
