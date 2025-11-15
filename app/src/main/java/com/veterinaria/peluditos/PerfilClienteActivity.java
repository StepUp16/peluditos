package com.veterinaria.peluditos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.veterinaria.peluditos.adapters.PacienteClienteAdapter;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.util.NetworkUtils;
import com.veterinaria.peluditos.util.PacientePhotoManager;

import java.util.ArrayList;
import java.util.List;

public class PerfilClienteActivity extends AppCompatActivity {

    private ShapeableImageView ivUserProfile;
    private ImageButton btnCambiarFoto;
    private CircularProgressIndicator photoProgress;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhone;
    private TextView tvMascotasEmpty;
    private RecyclerView recyclerViewMascotas;
    private LinearLayout llChangePassword;
    private LinearLayout llNotifications;
    private Button btnLogout;

    private FirebaseAuth firebaseAuth;
    private SessionManager sessionManager;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminPacienteViewModel pacienteViewModel;
    private PacientePhotoManager photoManager;
    private PacienteClienteAdapter mascotasAdapter;
    private ActivityResultLauncher<String> photoPickerLauncher;

    private final List<Paciente> cachePacientes = new ArrayList<>();
    private Usuario usuarioActual;
    private String clienteId;
    private boolean ignoreMenuSelection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil_cliente);

        firebaseAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        photoManager = new PacientePhotoManager("usuarios");

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        subirFotoPerfil(uri);
                    }
                }
        );

        initViews();
        configurarMenuInferior();
        configurarRecycler();

        cargarDatosUsuario();
        observarPacientes();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        photoProgress = findViewById(R.id.photoProgress);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvMascotasEmpty = findViewById(R.id.tvMascotasEmpty);
        recyclerViewMascotas = findViewById(R.id.recyclerViewMascotas);
        llChangePassword = findViewById(R.id.llChangePassword);
        llNotifications = findViewById(R.id.llNotifications);
        btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> finish());
        btnCambiarFoto.setOnClickListener(v -> seleccionarNuevaFoto());
        llChangePassword.setOnClickListener(v -> mostrarDialogoCambiarContrasena());
        llNotifications.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_feature_en_construccion, Toast.LENGTH_SHORT).show());
        btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    private void mostrarDialogoCambiarContrasena() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.error_red_requerida, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputLayout tilActual = dialogView.findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNueva = dialogView.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirmacion = dialogView.findViewById(R.id.tilConfirmPassword);
        TextInputEditText etActual = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNueva = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmacion = dialogView.findViewById(R.id.etConfirmPassword);
        LinearProgressIndicator indicator = dialogView.findViewById(R.id.changePasswordProgress);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_dialog_change_password)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_actualizar_contrasena, null)
                .setNegativeButton(R.string.btn_cancelar, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String actual = etActual.getText() != null ? etActual.getText().toString().trim() : "";
                String nueva = etNueva.getText() != null ? etNueva.getText().toString().trim() : "";
                String confirmar = etConfirmacion.getText() != null ? etConfirmacion.getText().toString().trim() : "";

                if (!validarCamposCambioPassword(actual, nueva, confirmar, tilActual, tilNueva, tilConfirmacion)) {
                    return;
                }

                if (TextUtils.isEmpty(currentUser.getEmail())) {
                    Toast.makeText(this, R.string.error_password_usuario_sin_correo, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }

                actualizarContrasena(currentUser, actual, nueva, dialog, indicator, positiveButton, tilActual);
            });
        });

        dialog.show();
    }

    private boolean validarCamposCambioPassword(String actual,
                                                String nueva,
                                                String confirmar,
                                                TextInputLayout tilActual,
                                                TextInputLayout tilNueva,
                                                TextInputLayout tilConfirmacion) {
        boolean esValido = true;

        tilActual.setError(null);
        tilNueva.setError(null);
        tilConfirmacion.setError(null);

        if (TextUtils.isEmpty(actual)) {
            tilActual.setError(getString(R.string.error_password_actual_requerida));
            esValido = false;
        }

        if (TextUtils.isEmpty(nueva)) {
            tilNueva.setError(getString(R.string.error_password_nueva_requerida));
            esValido = false;
        } else if (nueva.length() < 6) {
            tilNueva.setError(getString(R.string.error_password_min_length));
            esValido = false;
        } else if (nueva.equals(actual)) {
            tilNueva.setError(getString(R.string.error_password_iguales));
            esValido = false;
        }

        if (TextUtils.isEmpty(confirmar)) {
            tilConfirmacion.setError(getString(R.string.error_password_confirmacion_requerida));
            esValido = false;
        } else if (!nueva.equals(confirmar)) {
            tilConfirmacion.setError(getString(R.string.error_passwords_no_coinciden));
            esValido = false;
        }

        return esValido;
    }

    private void actualizarContrasena(FirebaseUser user,
                                      String actual,
                                      String nueva,
                                      AlertDialog dialog,
                                      LinearProgressIndicator indicator,
                                      Button positiveButton,
                                      TextInputLayout tilActual) {
        toggleDialogProgress(true, indicator, positiveButton);

        String email = user.getEmail();
        if (TextUtils.isEmpty(email)) {
            toggleDialogProgress(false, indicator, positiveButton);
            Toast.makeText(this, R.string.error_password_usuario_sin_correo, Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, actual);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(nueva).addOnCompleteListener(updateTask -> {
                    toggleDialogProgress(false, indicator, positiveButton);
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, R.string.msg_password_actualizada_logout, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        cerrarSesion();
                    } else {
                        Toast.makeText(this, R.string.error_password_actualizar, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                toggleDialogProgress(false, indicator, positiveButton);
                tilActual.setError(getString(R.string.error_password_actual_incorrecta));
            }
        });
    }

    private void toggleDialogProgress(boolean mostrando,
                                      LinearProgressIndicator indicator,
                                      Button positiveButton) {
        if (indicator != null) {
            indicator.setVisibility(mostrando ? View.VISIBLE : View.GONE);
        }
        if (positiveButton != null) {
            positiveButton.setEnabled(!mostrando);
            positiveButton.setAlpha(mostrando ? 0.6f : 1f);
        }
    }

    private void configurarRecycler() {
        recyclerViewMascotas.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMascotas.setHasFixedSize(true);
        mascotasAdapter = new PacienteClienteAdapter();
        recyclerViewMascotas.setAdapter(mascotasAdapter);
        mascotasAdapter.setOnPacienteClickListener(paciente -> {
            Intent intent = new Intent(this, DetallePacienteClienteActivity.class);
            intent.putExtra(DetallePacienteClienteActivity.EXTRA_PACIENTE_ID, paciente.getId());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void configurarMenuInferior() {
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        menu.setOnItemSelectedListener(item -> {
            if (ignoreMenuSelection) {
                ignoreMenuSelection = false;
                return true;
            }
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, ClienteMainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_mascotas) {
                startActivity(new Intent(this, ListadoPacientesClienteActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_citas) {
                startActivity(new Intent(this, ClienteCitaListadoActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                return true;
            }
            return false;
        });
        menu.setSelectedItemId(R.id.nav_perfil);
    }

    private void cargarDatosUsuario() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Login_Peluditos.class));
            finish();
            return;
        }

        clienteId = currentUser.getUid();
        String email = currentUser.getEmail();

        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    usuarioActual = usuario;
                    clienteId = usuario.getUid();
                    mostrarUsuario(usuario);
                    aplicarFiltroMascotas();
                }
            });
        } else {
            usuarioViewModel.getUsuario(clienteId).observe(this, usuario -> {
                if (usuario != null) {
                    usuarioActual = usuario;
                    mostrarUsuario(usuario);
                    aplicarFiltroMascotas();
                }
            });
        }
    }

    private void mostrarUsuario(Usuario usuario) {
        String nombre = (usuario.getNombre() != null ? usuario.getNombre() : "").trim();
        String apellido = (usuario.getApellido() != null ? usuario.getApellido() : "").trim();
        String displayName = (nombre + " " + apellido).trim();
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.text_cliente_sin_nombre);
        }
        tvUserName.setText(displayName);
        tvUserEmail.setText(!TextUtils.isEmpty(usuario.getEmail()) ? usuario.getEmail() : "--");
        tvUserPhone.setText(!TextUtils.isEmpty(usuario.getTelefono()) ? usuario.getTelefono() : "--");
        loadProfilePhoto(usuario.getFotoUrl());
    }

    private void loadProfilePhoto(String fotoUrl) {
        if (TextUtils.isEmpty(fotoUrl)) {
            ivUserProfile.setImageResource(R.drawable.icono_perfil);
            return;
        }
        Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.icono_perfil)
                .error(R.drawable.icono_perfil)
                .into(ivUserProfile);
    }

    private void observarPacientes() {
        pacienteViewModel.getAllPacientes().observe(this, pacientes -> {
            cachePacientes.clear();
            if (pacientes != null) {
                cachePacientes.addAll(pacientes);
            }
            aplicarFiltroMascotas();
        });
    }

    private void aplicarFiltroMascotas() {
        if (mascotasAdapter == null) {
            return;
        }
        List<Paciente> filtrados = new ArrayList<>();
        if (!TextUtils.isEmpty(clienteId)) {
            for (Paciente paciente : cachePacientes) {
                if (clienteId.equals(paciente.getClienteId())) {
                    filtrados.add(paciente);
                }
            }
        }
        mascotasAdapter.setPacientes(filtrados);
        actualizarEstadoMascotas(filtrados.isEmpty());
    }

    private void actualizarEstadoMascotas(boolean vacio) {
        if (tvMascotasEmpty != null) {
            tvMascotasEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewMascotas != null) {
            recyclerViewMascotas.setVisibility(vacio ? View.GONE : View.VISIBLE);
        }
    }

    private void seleccionarNuevaFoto() {
        if (usuarioActual == null) {
            Toast.makeText(this, R.string.error_usuario_no_cargado, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.error_red_requerida, Toast.LENGTH_SHORT).show();
            return;
        }
        photoPickerLauncher.launch("image/*");
    }

    private void subirFotoPerfil(Uri uri) {
        if (usuarioActual == null || photoManager == null) {
            return;
        }
        mostrarCargaFoto(true);
        Toast.makeText(this, R.string.msg_foto_subiendo, Toast.LENGTH_SHORT).show();
        photoManager.uploadPhoto(getApplicationContext(), uri, usuarioActual.getUid(),
                new PacientePhotoManager.UploadCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull String downloadUrl) {
                        usuarioActual.setFotoUrl(downloadUrl);
                        usuarioViewModel.update(usuarioActual);
                        loadProfilePhoto(downloadUrl);
                        mostrarCargaFoto(false);
                        Toast.makeText(PerfilClienteActivity.this,
                                R.string.msg_foto_actualizada,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception exception) {
                        mostrarCargaFoto(false);
                        Toast.makeText(PerfilClienteActivity.this,
                                R.string.error_subir_foto,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarCargaFoto(boolean mostrando) {
        if (photoProgress != null) {
            photoProgress.setVisibility(mostrando ? View.VISIBLE : View.GONE);
        }
        if (btnCambiarFoto != null) {
            btnCambiarFoto.setEnabled(!mostrando);
            btnCambiarFoto.setAlpha(mostrando ? 0.5f : 1f);
        }
    }

    private void cerrarSesion() {
        firebaseAuth.signOut();
        sessionManager.logoutUser();
        Intent intent = new Intent(this, Login_Peluditos.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoManager != null) {
            photoManager.dispose();
        }
    }
}
