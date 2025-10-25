package com.veterinaria.peluditos;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login_Peluditos extends AppCompatActivity {

    // 1. Declaramos los elementos de la UI y las instancias de Firebase
    private EditText edtEmail, edtContraseña;
    private Button btnIniciarSesion;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El nombre del layout es "login.xml", así que usamos R.layout.login
        setContentView(R.layout.login);

        // 2. Inicializamos las instancias de Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. Vinculamos los elementos de la UI con el código usando los IDs de tu XML
        edtEmail = findViewById(R.id.edtEmail);
        edtContraseña = findViewById(R.id.edtContraseña);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);

        // 4. Creamos el evento click para el botón de login
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String correo = edtEmail.getText().toString().trim();
                String contrasena = edtContraseña.getText().toString().trim();

                // Validamos que los campos no estén vacíos
                if (correo.isEmpty() || contrasena.isEmpty()) {
                    Toast.makeText(Login_Peluditos.this, "Por favor, ingrese correo y contraseña", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Llamamos a la función para iniciar sesión
                iniciarSesion(correo, contrasena);
            }
        });
    }

    private void iniciarSesion(String correo, String contrasena) {
        // 5. Usamos Firebase Authentication para verificar el usuario
        mAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Si la autenticación es exitosa, obtenemos el usuario
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Y ahora, verificamos su rol en Firestore
                            if (user != null) {
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
        // 6. Buscamos en la colección "usuarios" el documento con el UID del usuario logueado
        db.collection("usuarios").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Si el documento existe, leemos el campo "rol"
                                String rol = document.getString("rol");

                                if ("administrador".equals(rol)) {
                                    // Si el rol es "administrador", vamos al flujo de admin
                                    Toast.makeText(Login_Peluditos.this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show();
                                    // --- CAMBIO REALIZADO AQUÍ ---
                                    // Ahora abrimos admin_home, que tiene la lógica correcta.
                                    Intent intent = new Intent(Login_Peluditos.this, admin_home.class);
                                    startActivity(intent);
                                    finish(); // Cierra la actividad de login
                                } else if ("cliente".equals(rol)) {
                                    // Si el rol es "cliente", vamos al flujo de cliente
                                    Toast.makeText(Login_Peluditos.this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show();
                                    // Reemplaza ClienteMainActivity.class con el nombre de tu actividad principal de cliente
                                    Intent intent = new Intent(Login_Peluditos.this, ClienteMainActivity.class);
                                    startActivity(intent);
                                    finish(); // Cierra la actividad de login
                                } else {
                                    // Por si acaso hay un rol no definido
                                    Toast.makeText(Login_Peluditos.this, "Rol no reconocido.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Esto no debería pasar si el admin crea bien los usuarios
                                Toast.makeText(Login_Peluditos.this, "No se encontraron datos para este usuario.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Error al buscar en Firestore
                            Toast.makeText(Login_Peluditos.this, "Error al obtener datos: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
