package com.veterinaria.peluditos;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegistrarActivity extends AppCompatActivity {

    private EditText edtNombre, edtApellido, edtEmail, edtTelefono, edtContraseña, edtConfirmarContraseña, edtDUI;
    private Button btnRegistrar;
    private TextView txtYaTengoCuenta;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Patrón para validar contraseña alfanumérica de mínimo 8 caracteres
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    // Patrón para validar DUI (9 dígitos)
    private static final Pattern DUI_PATTERN =
            Pattern.compile("^[0-9]{9}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vincular elementos de la UI
        edtNombre = findViewById(R.id.edtNombre);
        edtApellido = findViewById(R.id.edtApellido);
        edtDUI = findViewById(R.id.edtDUI);
        edtEmail = findViewById(R.id.edtEmail);
        edtTelefono = findViewById(R.id.edtTelefono);
        edtContraseña = findViewById(R.id.edtContraseña);
        edtConfirmarContraseña = findViewById(R.id.edtConfirmarContraseña);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        txtYaTengoCuenta = findViewById(R.id.txtYaTengoCuenta);

        // Configurar eventos de click
        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });

        txtYaTengoCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Volver al login
                Intent intent = new Intent(RegistrarActivity.this, Login_Peluditos.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registrarUsuario() {
        String nombre = edtNombre.getText().toString().trim();
        String apellido = edtApellido.getText().toString().trim();
        String dui = edtDUI.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String telefono = edtTelefono.getText().toString().trim();
        String contraseña = edtContraseña.getText().toString().trim();
        String confirmarContraseña = edtConfirmarContraseña.getText().toString().trim();

        // Validar campos vacíos
        if (TextUtils.isEmpty(nombre)) {
            edtNombre.setError("El nombre es requerido");
            edtNombre.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(apellido)) {
            edtApellido.setError("El apellido es requerido");
            edtApellido.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(dui)) {
            edtDUI.setError("El DUI es requerido");
            edtDUI.requestFocus();
            return;
        }

        if (!DUI_PATTERN.matcher(dui).matches()) {
            edtDUI.setError("El DUI debe tener 9 dígitos");
            edtDUI.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("El correo electrónico es requerido");
            edtEmail.requestFocus();
            return;
        }

        // Validar formato de correo
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Ingrese un correo electrónico válido");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(telefono)) {
            edtTelefono.setError("El teléfono es requerido");
            edtTelefono.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(contraseña)) {
            edtContraseña.setError("La contraseña es requerida");
            edtContraseña.requestFocus();
            return;
        }

        // Validar contraseña (mínimo 8 caracteres, alfanumérica)
        if (!PASSWORD_PATTERN.matcher(contraseña).matches()) {
            edtContraseña.setError("La contraseña debe tener mínimo 8 caracteres y contener letras y números");
            edtContraseña.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmarContraseña)) {
            edtConfirmarContraseña.setError("Confirme su contraseña");
            edtConfirmarContraseña.requestFocus();
            return;
        }

        // Validar que las contraseñas coincidan
        if (!contraseña.equals(confirmarContraseña)) {
            edtConfirmarContraseña.setError("Las contraseñas no coinciden");
            edtConfirmarContraseña.requestFocus();
            return;
        }

        // Crear cuenta en Firebase Auth
        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Registrando...");

        mAuth.createUserWithEmailAndPassword(email, contraseña)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Registro exitoso, guardar datos adicionales en Firestore
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                guardarDatosUsuario(user.getUid(), nombre, apellido, dui, email, telefono);
                            }
                        } else {
                            // Error en el registro
                            btnRegistrar.setEnabled(true);
                            btnRegistrar.setText("Registrarme");
                            Toast.makeText(RegistrarActivity.this,
                                    "Error en el registro: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void guardarDatosUsuario(String uid, String nombre, String apellido, String dui, String email, String telefono) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("apellido", apellido);
        usuario.put("dui", dui);
        usuario.put("email", email);
        usuario.put("telefono", telefono);
        usuario.put("rol", "cliente"); // Por defecto los usuarios registrados son clientes
        usuario.put("fechaRegistro", System.currentTimeMillis());

        db.collection("usuarios").document(dui)
                .set(usuario)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        btnRegistrar.setEnabled(true);
                        btnRegistrar.setText("Registrarme");

                        if (task.isSuccessful()) {
                            Toast.makeText(RegistrarActivity.this,
                                    "Registro exitoso. Bienvenido!", Toast.LENGTH_SHORT).show();

                            // Ir a la pantalla principal del cliente
                            Intent intent = new Intent(RegistrarActivity.this, ClienteMainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegistrarActivity.this,
                                    "Error al guardar datos: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}