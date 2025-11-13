package com.veterinaria.peluditos;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.imageview.ShapeableImageView;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.util.NetworkUtils;
import com.veterinaria.peluditos.util.PacientePhotoManager;

public class AdminPerfil extends AppCompatActivity {
    private static final String TAG = "AdminPerfil";
    private static final int REQUEST_CODE_EDITAR_PERFIL = 1001;

    // UI Elements
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private Button btnViewUsers, btnEditarPerfil;
    private ImageButton btnBack;
    private LinearLayout llManageRoles, llAccessStats;
    private LinearLayout iconHome, iconCitas, iconPacientes, iconClientes, iconPerfil;
    private ShapeableImageView ivUserProfile;
    private ImageButton btnChangePhoto;
    private ProgressBar photoProgress;

    // Firebase y otros componentes
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AdminUsuarioViewModel viewModel;
    private SessionManager sessionManager;
    private ActivityResultLauncher<String> photoPickerLauncher;
    private PacientePhotoManager photoManager;

    // Datos del usuario actual
    private Usuario currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_perfil);

        photoManager = new PacientePhotoManager("usuarios");
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        subirFotoPerfil(uri);
                    }
                }
        );

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
        ivUserProfile = findViewById(R.id.ivUserProfile);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        photoProgress = findViewById(R.id.photoProgress);

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

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> seleccionarNuevaFoto());
        }

        // Botón ver usuarios
        btnViewUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminUsuarioListadoActivity.class);
            startActivity(intent);
        });

        // Botón editar perfil
        btnEditarPerfil.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent = new Intent(AdminPerfil.this, AdminUsuarioEditar.class);
                intent.putExtra("USUARIO_ID", currentUser.getUid());
                intent.putExtra("ES_PERFIL_PROPIO", true); // Flag para indicar que es edición de perfil propio
                startActivityForResult(intent, REQUEST_CODE_EDITAR_PERFIL);
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
                Intent intent = new Intent(this, admin_home.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconCitas != null) {
            iconCitas.setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_cita_listado.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconPacientes != null) {
            iconPacientes.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPacienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconClientes != null) {
            iconClientes.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminUsuarioClienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconPerfil != null) {
            iconPerfil.setOnClickListener(v -> {
                // ya estamos en perfil; refrescar datos
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
                    document.getString("rol"),
                    document.getString("fotoUrl")
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
                loadUserPhoto(usuario.getFotoUrl());

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDITAR_PERFIL && resultCode == RESULT_OK) {
            loadUserData();
        }
    }

    private void seleccionarNuevaFoto() {
        if (currentUser == null || currentUser.getUid() == null || currentUser.getUid().isEmpty()) {
            Toast.makeText(this, R.string.error_usuario_no_cargado, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.error_red_requerida, Toast.LENGTH_SHORT).show();
            return;
        }
        if (photoPickerLauncher != null) {
            photoPickerLauncher.launch("image/*");
        }
    }

    private void subirFotoPerfil(Uri uri) {
        if (photoManager == null || currentUser == null || currentUser.getUid() == null) {
            return;
        }
        mostrarCargaFoto(true);
        Toast.makeText(this, R.string.msg_foto_subiendo, Toast.LENGTH_SHORT).show();
        photoManager.uploadPhoto(getApplicationContext(), uri, currentUser.getUid(),
                new PacientePhotoManager.UploadCallback() {
                    @Override
                    public void onSuccess(@NonNull String downloadUrl) {
                        currentUser.setFotoUrl(downloadUrl);
                        viewModel.update(currentUser);
                        loadUserPhoto(downloadUrl);
                        mostrarCargaFoto(false);
                        Toast.makeText(AdminPerfil.this,
                                R.string.msg_foto_actualizada,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        mostrarCargaFoto(false);
                        Toast.makeText(AdminPerfil.this,
                                R.string.error_subir_foto,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarCargaFoto(boolean mostrando) {
        if (photoProgress != null) {
            photoProgress.setVisibility(mostrando ? View.VISIBLE : View.GONE);
        }
        if (btnChangePhoto != null) {
            btnChangePhoto.setEnabled(!mostrando);
            btnChangePhoto.setAlpha(mostrando ? 0.4f : 1f);
        }
    }

    private void loadUserPhoto(String fotoUrl) {
        if (ivUserProfile == null) {
            return;
        }
        if (TextUtils.isEmpty(fotoUrl)) {
            ivUserProfile.setImageResource(R.drawable.user_sofia);
            return;
        }
        Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.user_sofia)
                .error(R.drawable.user_sofia)
                .into(ivUserProfile);
    }

    private void startActivityWithAnimation(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoManager != null) {
            photoManager.dispose();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
