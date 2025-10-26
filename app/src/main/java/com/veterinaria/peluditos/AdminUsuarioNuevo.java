package com.veterinaria.peluditos;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.veterinaria.peluditos.data.Usuario;

public class AdminUsuarioNuevo extends AppCompatActivity {
    private EditText etNombre, etApellido, etCorreo, etTelefono, etDui, etDireccion, etPassword, etConfirmPassword;
    private Spinner spinnerRol;
    private Button btnCrearUsuario;
    private ImageButton btnBack;
    private AdminUsuarioViewModel viewModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_nuevo);

        // Inicializar ViewModel y FirebaseAuth
        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        // Inicializar vistas
        initViews();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etCorreo = findViewById(R.id.etCorreo);
        etTelefono = findViewById(R.id.etTelefono);
        etDui = findViewById(R.id.etDui);
        etDireccion = findViewById(R.id.etDireccion);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRol = findViewById(R.id.spinnerRol);
        btnCrearUsuario = findViewById(R.id.btnCrearUsuario);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCrearUsuario.setOnClickListener(v -> crearUsuario());
        btnBack.setOnClickListener(v -> finish());
    }

    private void crearUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String dui = etDui.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String rol = spinnerRol.getSelectedItem().toString();

        // Validaciones
        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || telefono.isEmpty() ||
            dui.isEmpty() || direccion.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar el botón mientras se procesa
        btnCrearUsuario.setEnabled(false);

        // Crear usuario en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        // Crear objeto Usuario
                        Usuario nuevoUsuario = new Usuario(
                                uid, nombre, apellido, email,
                                telefono, dui, direccion, rol
                        );

                        // Guardar en Room y Firestore
                        viewModel.insert(nuevoUsuario);

                        Toast.makeText(AdminUsuarioNuevo.this,
                                "Usuario creado exitosamente", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AdminUsuarioNuevo.this,
                                "Error al crear usuario: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        btnCrearUsuario.setEnabled(true);
                    }
                });
    }
}
