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
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class Login_Peluditos extends AppCompatActivity {

    // 1. Declaramos los elementos de la UI y las instancias de Firebase
    private EditText edtEmail, edtContraseña;
    private Button btnIniciarSesion;
    private Chip chipRecordarme;
    private TextView txtRegistrarme;
    private TextView txtOlvidarContraseña;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    // Patrón para validar contraseña alfanumérica de mínimo 8 caracteres
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El nombre del layout es "login.xml", así que usamos R.layout.login
        setContentView(R.layout.login);

        // 2. Inicializamos las instancias de Firebase y SessionManager
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // 3. Vinculamos los elementos de la UI con el código usando los IDs de tu XML
        edtEmail = findViewById(R.id.edtEmail);
        edtContraseña = findViewById(R.id.edtContraseña);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        chipRecordarme = findViewById(R.id.chipRecordarme);
        txtRegistrarme = findViewById(R.id.txtRegistrarme);
        txtOlvidarContraseña = findViewById(R.id.txtOlvidarContraseña);

        // 4. Verificar si ya hay una sesión activa
        verificarSesionActiva();

        // 5. Cargar datos guardados si "Recordarme" estaba activado
        cargarDatosRecordados();

        // 6. Configurar eventos de click
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String correo = edtEmail.getText().toString().trim();
                String contrasena = edtContraseña.getText().toString().trim();

                // Validar campos
                if (!validarCampos(correo, contrasena)) {
                    return;
                }

                // Llamamos a la función para iniciar sesión
                iniciarSesion(correo, contrasena);
            }
        });

        // 7. Configurar navegación al registro
        txtRegistrarme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login_Peluditos.this, RegistrarActivity.class);
                startActivity(intent);
            }
        });

        if (txtOlvidarContraseña != null) {
            txtOlvidarContraseña.setOnClickListener(v -> {
                Intent intent = new Intent(Login_Peluditos.this, AdminRecuperarContrasenaActivity.class);
                startActivity(intent);
            });
        }
    }

    private void verificarSesionActiva() {
        if (sessionManager.isLoggedIn()) {
            String userRole = sessionManager.getUserRole();
            navegarSegunRol(userRole);
        }
    }

    private void cargarDatosRecordados() {
        if (sessionManager.isRememberMeEnabled()) {
            String emailGuardado = sessionManager.getSavedEmail();
            String passwordGuardada = sessionManager.getSavedPassword();
            edtEmail.setText(emailGuardado);
            edtContraseña.setText(passwordGuardada);
            chipRecordarme.setChecked(true);
        }
    }

    private boolean validarCampos(String correo, String contrasena) {
        // Validar que los campos no estén vacíos
        if (TextUtils.isEmpty(correo)) {
            edtEmail.setError("El correo electrónico es requerido");
            edtEmail.requestFocus();
            return false;
        }

        // Validar formato de correo
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            edtEmail.setError("Ingrese un correo electrónico válido");
            edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(contrasena)) {
            edtContraseña.setError("La contraseña es requerida");
            edtContraseña.requestFocus();
            return false;
        }

        // Validar contraseña (mínimo 8 caracteres, alfanumérica)
        if (!PASSWORD_PATTERN.matcher(contrasena).matches()) {
            edtContraseña.setError("La contraseña debe tener mínimo 8 caracteres y contener letras y números");
            edtContraseña.requestFocus();
            return false;
        }

        return true;
    }

    private void iniciarSesion(String correo, String contrasena) {
        // Deshabilitar botón durante el proceso
        btnIniciarSesion.setEnabled(false);
        btnIniciarSesion.setText("Iniciando...");

        // 5. Usamos Firebase Authentication para verificar el usuario
        mAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnIniciarSesion.setEnabled(true);
                        btnIniciarSesion.setText("Iniciar Sesión");

                        if (task.isSuccessful()) {
                            // Si la autenticación es exitosa, obtenemos el usuario
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Y ahora, verificamos su rol en Firestore
                            if (user != null) {
                                // Guardar datos si "Recordarme" está activado
                                sessionManager.saveRememberMe(chipRecordarme.isChecked(), correo, contrasena);
                                verificarRol(user.getUid());
                            }
                        } else {
                            // Si falla, mostramos un error
                            Toast.makeText(Login_Peluditos.this, "Error en la autenticación: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void verificarRol(String uid) {
        // Primero buscamos el usuario por email para obtener su DUI
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(Login_Peluditos.this, "Error: No se pudo obtener la información del usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("usuarios")
                .whereEqualTo("email", user.getEmail())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Tomamos el primer documento que coincida con el email
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String rol = document.getString("rol");

                        // Guardar sesión activa
                        sessionManager.createLoginSession(rol);

                        // Navegar según el rol
                        navegarSegunRol(rol);
                    } else {
                        Toast.makeText(Login_Peluditos.this,
                            "No se encontraron datos para este usuario.",
                            Toast.LENGTH_SHORT).show();
                        // Cerrar sesión de Firebase Auth ya que no encontramos los datos en Firestore
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Login_Peluditos.this,
                        "Error al obtener datos: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void navegarSegunRol(String rol) {
        if ("administrador".equals(rol)) {
            // Si el rol es "administrador", vamos al flujo de admin
            Toast.makeText(Login_Peluditos.this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Login_Peluditos.this, admin_home.class);
            startActivity(intent);
            finish(); // Cierra la actividad de login
        } else if ("cliente".equals(rol)) {
            // Si el rol es "cliente", vamos al flujo de cliente
            Toast.makeText(Login_Peluditos.this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Login_Peluditos.this, ClienteMainActivity.class);
            startActivity(intent);
            finish(); // Cierra la actividad de login
        } else {
            // Por si acaso hay un rol no definido
            Toast.makeText(Login_Peluditos.this, "Rol no reconocido.", Toast.LENGTH_SHORT).show();
        }
    }
}
