package com.veterinaria.peluditos;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veterinaria.peluditos.data.Usuario;

import java.util.UUID;

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

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private boolean isValidPassword(String password) {
        // Al menos 8 caracteres, al menos una letra y un número
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
        return password.matches(passwordPattern);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void guardarUsuarioLocal(String nombre, String apellido, String email,
                                     String telefono, String dui, String direccion, String rol) {
        // Usar el DUI como ID
        String localUid = dui.replace("-", "").trim();

        // Crear objeto Usuario
        Usuario nuevoUsuario = new Usuario(
                localUid, nombre, apellido, email,
                telefono, dui, direccion, rol
        );

        // Guardar solo en Room
        viewModel.insert(nuevoUsuario);

        Toast.makeText(AdminUsuarioNuevo.this,
                "Usuario guardado localmente. Se sincronizará cuando haya conexión",
                Toast.LENGTH_LONG).show();
        finish();
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

        // Validación de longitud para nombre
        if (nombre.length() < 7) {
            etNombre.setError("El nombre debe tener al menos 7 caracteres");
            etNombre.requestFocus();
            return;
        }

        // Validación de longitud para apellido
        if (apellido.length() < 7) {
            etApellido.setError("El apellido debe tener al menos 7 caracteres");
            etApellido.requestFocus();
            return;
        }

        // Validación de formato de correo electrónico
        if (!isValidEmail(email)) {
            etCorreo.setError("Ingrese un correo electrónico válido");
            etCorreo.requestFocus();
            return;
        }

        // Validación de contraseña
        if (!isValidPassword(password)) {
            etPassword.setError("La contraseña debe tener al menos 8 caracteres y contener letras y números");
            etPassword.requestFocus();
            return;
        }

        // Validación de coincidencia de contraseñas
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return;
        }

        // Validación específica para DUI
        if (dui.length() < 9) {
            etDui.setError("El DUI debe tener al menos 9 dígitos");
            etDui.requestFocus();
            return;
        }

        // Deshabilitar el botón mientras se procesa
        btnCrearUsuario.setEnabled(false);

        if (isNetworkAvailable()) {
            // Usar el DUI como ID del documento
            String docId = dui.replace("-", "").trim();

            // Verificar si ya existe un usuario con ese DUI
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(docId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(AdminUsuarioNuevo.this,
                                    "Ya existe un usuario registrado con este DUI",
                                    Toast.LENGTH_LONG).show();
                            btnCrearUsuario.setEnabled(true);
                        } else {
                            // Si no existe, proceder con la creación
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this, task -> {
                                        if (task.isSuccessful()) {
                                            // Crear objeto Usuario con el DUI como ID
                                            Usuario nuevoUsuario = new Usuario(
                                                    docId, nombre, apellido, email,
                                                    telefono, dui, direccion, rol
                                            );

                                            // Guardar en Firestore primero
                                            FirebaseFirestore.getInstance()
                                                    .collection("usuarios")
                                                    .document(docId)
                                                    .set(nuevoUsuario)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Una vez guardado en Firestore, guardar en Room
                                                        viewModel.insert(nuevoUsuario);
                                                        Toast.makeText(AdminUsuarioNuevo.this,
                                                                "Usuario creado exitosamente",
                                                                Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(AdminUsuarioNuevo.this,
                                                                "Error al guardar en Firestore: " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                        btnCrearUsuario.setEnabled(true);
                                                    });
                                        } else {
                                            Toast.makeText(AdminUsuarioNuevo.this,
                                                    "Error al crear usuario en Firebase: " + task.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                            btnCrearUsuario.setEnabled(true);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AdminUsuarioNuevo.this,
                                "Error al verificar DUI: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        btnCrearUsuario.setEnabled(true);
                    });
        } else {
            // Si no hay conexión, guardar solo localmente
            guardarUsuarioLocal(nombre, apellido, email, telefono, dui, direccion, rol);
            btnCrearUsuario.setEnabled(true);
        }
    }
}
