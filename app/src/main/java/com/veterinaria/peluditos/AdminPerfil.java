package com.veterinaria.peluditos;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veterinaria.peluditos.data.Usuario;

public class AdminPerfil extends AppCompatActivity {
    private static final String TAG = "AdminPerfil";

    // UI Elements
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private Button btnViewUsers, btnEditarPerfil;
    private ImageButton btnBack;
    private LinearLayout llManageRoles, llAccessStats;
    private LinearLayout iconHome, iconCitas, iconPacientes, iconClientes, iconPerfil;

    // Firebase y otros componentes
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AdminUsuarioViewModel viewModel;
    private SessionManager sessionManager;

    // Datos del usuario actual
    private Usuario currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_perfil);

        // Inicializar componentes
        initComponents();
        initViews();
        setupListeners();

        // Cargar datos del usuario
        loadUserData();
    }

    private void initComponents() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        sessionManager = new SessionManager(this);
    }

    private void initViews() {
        // Información del usuario
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserRole = findViewById(R.id.tvUserRole);

        // Botones principales
        btnViewUsers = findViewById(R.id.btnViewUsers);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnBack = findViewById(R.id.btnBack);

        // Opciones del menú
        llManageRoles = findViewById(R.id.llManageRoles);
        llAccessStats = findViewById(R.id.llAccessStats);

        // Menú inferior
        iconHome = findViewById(R.id.iconHome).getParent() instanceof LinearLayout ?
                (LinearLayout) findViewById(R.id.iconHome).getParent() : null;
        iconCitas = findViewById(R.id.iconCitas).getParent() instanceof LinearLayout ?
                (LinearLayout) findViewById(R.id.iconCitas).getParent() : null;
        iconPacientes = findViewById(R.id.iconPacientes).getParent() instanceof LinearLayout ?
                (LinearLayout) findViewById(R.id.iconPacientes).getParent() : null;
        iconClientes = findViewById(R.id.iconClientes).getParent() instanceof LinearLayout ?
                (LinearLayout) findViewById(R.id.iconClientes).getParent() : null;
        iconPerfil = findViewById(R.id.iconPerfil).getParent() instanceof LinearLayout ?
                (LinearLayout) findViewById(R.id.iconPerfil).getParent() : null;
    }

    private void setupListeners() {
        // Botón de regreso
        btnBack.setOnClickListener(v -> finish());

        // Botón ver usuarios
        btnViewUsers.setOnClickListener(v -> {
            // Comentamos temporalmente hasta que exista la clase AdminUsuarios
            // Intent intent = new Intent(AdminPerfil.this, AdminUsuarios.class);
            // startActivity(intent);
            Toast.makeText(this, "Función de ver usuarios en desarrollo", Toast.LENGTH_SHORT).show();
        });

        // Botón editar perfil
        btnEditarPerfil.setOnClickListener(v -> {
            if (currentUser != null) {
                // Comentamos temporalmente hasta que exista la clase AdminEditarPerfil
                // Intent intent = new Intent(AdminPerfil.this, AdminEditarPerfil.class);
                // intent.putExtra("usuario_uid", currentUser.getUid());
                // startActivity(intent);
                Toast.makeText(this, "Función de editar perfil en desarrollo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: No se pudieron cargar los datos del usuario",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Gestionar roles
        llManageRoles.setOnClickListener(v -> {
            // Aquí puedes agregar la navegación a gestión de roles
            Toast.makeText(this, "Función de gestión de roles en desarrollo",
                    Toast.LENGTH_SHORT).show();
        });

        // Estadísticas
        llAccessStats.setOnClickListener(v -> {
            // Aquí puedes agregar la navegación a estadísticas
            Toast.makeText(this, "Función de estadísticas en desarrollo",
                    Toast.LENGTH_SHORT).show();
        });

        // Menú inferior
        setupBottomMenuListeners();
    }

    private void setupBottomMenuListeners() {
        if (iconHome != null) {
            iconHome.setOnClickListener(v -> {
                Intent intent = new Intent(AdminPerfil.this, admin_home.class);
                startActivity(intent);
            });
        }

        if (iconCitas != null) {
            iconCitas.setOnClickListener(v -> {
                // Navegar a citas
                Toast.makeText(this, "Navegando a Citas", Toast.LENGTH_SHORT).show();
            });
        }

        if (iconPacientes != null) {
            iconPacientes.setOnClickListener(v -> {
                // Navegar a pacientes
                Toast.makeText(this, "Navegando a Pacientes", Toast.LENGTH_SHORT).show();
            });
        }

        if (iconClientes != null) {
            iconClientes.setOnClickListener(v -> {
                // Navegar a clientes
                Toast.makeText(this, "Navegando a Clientes", Toast.LENGTH_SHORT).show();
            });
        }

        if (iconPerfil != null) {
            iconPerfil.setOnClickListener(v -> {
                // Ya estamos en perfil, no hacer nada o refrescar
                loadUserData();
            });
        }
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            String userEmail = firebaseUser.getEmail();
            Log.d(TAG, "Usuario Firebase encontrado: " + userEmail);

            if (isNetworkAvailable()) {
                // Cargar desde Firebase
                loadFromFirebase(firebaseUser.getUid(), userEmail);
            } else {
                // Cargar desde base de datos local
                loadFromLocalDatabase(userEmail);
            }
        } else {
            // Si no hay usuario de Firebase, intentar cargar desde local usando el email guardado
            String savedEmail = sessionManager.getSavedEmail();
            if (!savedEmail.isEmpty()) {
                loadFromLocalDatabase(savedEmail);
            } else {
                showError("No se pudo identificar al usuario. Por favor, inicie sesión nuevamente.");
                redirectToLogin();
            }
        }
    }

    private void loadFromFirebase(String uid, String userEmail) {
        Log.d(TAG, "Cargando datos desde Firebase para UID: " + uid);

        // Primero intentar buscar por UID
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Usuario usuario = createUsuarioFromDocument(documentSnapshot);
                        if (usuario != null) {
                            currentUser = usuario;
                            updateUI(usuario);
                            Log.d(TAG, "Datos cargados desde Firebase exitosamente por UID");
                            return;
                        }
                    }

                    // Si no se encuentra por UID, buscar por email
                    Log.d(TAG, "No encontrado por UID, buscando por email: " + userEmail);
                    searchUserByEmail(userEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar desde Firebase por UID: " + e.getMessage());
                    searchUserByEmail(userEmail);
                });
    }

    private void searchUserByEmail(String email) {
        db.collection("usuarios")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Usuario usuario = createUsuarioFromDocument(querySnapshot.getDocuments().get(0));
                        if (usuario != null) {
                            currentUser = usuario;
                            updateUI(usuario);
                            Log.d(TAG, "Datos cargados desde Firebase exitosamente por email");
                        } else {
                            Log.e(TAG, "Error al crear objeto Usuario desde documento Firebase");
                            loadFromLocalDatabase(email);
                        }
                    } else {
                        Log.w(TAG, "Usuario no encontrado en Firebase por email, intentando base local");
                        loadFromLocalDatabase(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al buscar por email en Firebase: " + e.getMessage());
                    loadFromLocalDatabase(email);
                });
    }

    private void loadFromLocalDatabase(String email) {
        Log.d(TAG, "Cargando datos desde base de datos local para email: " + email);

        // Usar el nuevo método más eficiente para buscar por email
        viewModel.getUsuarioByEmail(email).observe(this, usuario -> {
            if (usuario != null) {
                currentUser = usuario;
                updateUI(usuario);
                Log.d(TAG, "Datos cargados desde base local exitosamente");
            } else {
                Log.w(TAG, "Usuario no encontrado en base local con email: " + email);
                showError("No se encontraron datos del usuario. Contacte al administrador.");
            }
        });
    }

    private Usuario createUsuarioFromDocument(DocumentSnapshot document) {
        try {
            return new Usuario(
                    document.getId(),
                    document.getString("nombre"),
                    document.getString("apellido"),
                    document.getString("email"),
                    document.getString("telefono"),
                    document.getString("dui"),
                    document.getString("direccion"),
                    document.getString("rol")
            );
        } catch (Exception e) {
            Log.e(TAG, "Error al crear Usuario desde documento: " + e.getMessage());
            return null;
        }
    }

    private void updateUI(Usuario usuario) {
        if (usuario != null) {
            runOnUiThread(() -> {
                String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                tvUserName.setText(nombreCompleto);
                tvUserEmail.setText(usuario.getEmail());

                // Capitalizar la primera letra del rol
                String rol = usuario.getRol();
                if (rol != null && !rol.isEmpty()) {
                    rol = rol.substring(0, 1).toUpperCase() + rol.substring(1).toLowerCase();
                }
                tvUserRole.setText(rol);

                Log.d(TAG, "UI actualizada con datos del usuario: " + nombreCompleto);
            });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(AdminPerfil.this, message, Toast.LENGTH_LONG).show());
    }

    private void redirectToLogin() {
        sessionManager.logoutUser();
        Intent intent = new Intent(AdminPerfil.this, Login_Peluditos.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando regresamos a la actividad
        loadUserData();
    }
}