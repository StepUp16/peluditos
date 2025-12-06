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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.util.NetworkUtils;
import com.veterinaria.peluditos.util.PacientePhotoManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PerfilClienteActivity extends AppCompatActivity {

    private ShapeableImageView ivUserProfile;
    private ImageButton btnCambiarFoto;
    private CircularProgressIndicator photoProgress;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhone;
    private TextView tvMascotasEmpty;
    private LinearLayout llCitasContainer;
    private TextView tvCitasEmpty;
    private MaterialButton btnVerCitas;
    private RecyclerView recyclerViewMascotas;
    private LinearLayout llChangePassword;
    private LinearLayout llNotifications;
    private Button btnLogout;

    private FirebaseAuth firebaseAuth;
    private SessionManager sessionManager;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminCitaViewModel citaViewModel;
    private PacientePhotoManager photoManager;
    private PacienteClienteAdapter mascotasAdapter;
    private ActivityResultLauncher<String> photoPickerLauncher;

    private final List<Paciente> cachePacientes = new ArrayList<>();
    private final List<Cita> cacheCitas = new ArrayList<>();
    private Usuario usuarioActual;
    private String clienteId;
    private boolean ignoreMenuSelection = true;
    private final SimpleDateFormat fechaResumenFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat horaResumenFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil_cliente);

        firebaseAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);
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
        observarCitas();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        if (menu != null && menu.getSelectedItemId() != R.id.nav_perfil) {
            menu.setSelectedItemId(R.id.nav_perfil);
        }
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
        llCitasContainer = findViewById(R.id.llCitasContainer);
        tvCitasEmpty = findViewById(R.id.tvCitasEmpty);
        btnVerCitas = findViewById(R.id.btnVerCitas);
        recyclerViewMascotas = findViewById(R.id.recyclerViewMascotas);
        llChangePassword = findViewById(R.id.llChangePassword);
        //llNotifications = findViewById(R.id.llNotifications);
        btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> finish());
        btnCambiarFoto.setOnClickListener(v -> seleccionarNuevaFoto());
        if (btnVerCitas != null) {
            btnVerCitas.setOnClickListener(v -> abrirListadoCitas());
        }
        if (llChangePassword != null) {
            llChangePassword.setOnClickListener(v -> mostrarDialogoCambiarContrasena());
        }
        if (llNotifications != null) {
            llNotifications.setOnClickListener(v ->
                    Toast.makeText(this, R.string.msg_feature_en_construccion, Toast.LENGTH_SHORT).show());
        }
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
        actualizarCitasResumen();

        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    usuarioActual = usuario;
                    clienteId = usuario.getUid();
                    mostrarUsuario(usuario);
                    aplicarFiltroMascotas();
                    actualizarCitasResumen();
                }
            });
        } else {
            usuarioViewModel.getUsuario(clienteId).observe(this, usuario -> {
                if (usuario != null) {
                    usuarioActual = usuario;
                    mostrarUsuario(usuario);
                    aplicarFiltroMascotas();
                    actualizarCitasResumen();
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
        // 1. Obtener la imagen que se está viendo AHORA MISMO en la pantalla
        android.graphics.drawable.Drawable imagenActual = ivUserProfile.getDrawable();

        // Si por alguna razón es nula (está vacía), ponemos la default inmediatamente
        if (imagenActual == null) {
            imagenActual = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.icono_perfil);
            ivUserProfile.setImageDrawable(imagenActual);
        }

        if (TextUtils.isEmpty(fotoUrl)) {
            ivUserProfile.setImageResource(R.drawable.icono_perfil);
            return;
        }

        if (fotoUrl.startsWith("http")) {
            // Legacy URL (broken/paid) - Show placeholder immediately
            ivUserProfile.setImageResource(R.drawable.icono_perfil);
            return;
        }

        try {
            byte[] imageByteArray = android.util.Base64.decode(fotoUrl, android.util.Base64.DEFAULT);

            Glide.with(this)
                    .asBitmap()
                    .load(imageByteArray)
                    // LA CLAVE: Le decimos a Glide "Usa la imagen que YA está puesta mientras cargas la nueva"
                    .placeholder(imagenActual)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE) // En perfil a veces es mejor no cachear si se edita mucho, o usa ALL
                    .skipMemoryCache(true) // Forzamos recarga fresca para perfil
                    .dontAnimate() // <--- OBLIGATORIO: Elimina el efecto "Fade In" que se ve como parpadeo
                    .into(ivUserProfile);

        } catch (IllegalArgumentException e) {
            ivUserProfile.setImageResource(R.drawable.icono_perfil);
        }
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

    private void observarCitas() {
        if (citaViewModel == null) {
            return;
        }
        citaViewModel.getAllCitas().observe(this, citas -> {
            cacheCitas.clear();
            if (citas != null) {
                cacheCitas.addAll(citas);
            }
            actualizarCitasResumen();
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

    private void actualizarCitasResumen() {
        if (llCitasContainer == null) {
            return;
        }

        llCitasContainer.removeAllViews();

        List<Cita> delCliente = new ArrayList<>();
        if (!TextUtils.isEmpty(clienteId)) {
            for (Cita cita : cacheCitas) {
                if (clienteId.equals(cita.getClienteId())) {
                    delCliente.add(cita);
                }
            }
        }

        if (delCliente.isEmpty()) {
            toggleCitasEmptyState(true);
            return;
        }

        Collections.sort(delCliente, (c1, c2) ->
                Long.compare(c1.getFechaHoraTimestamp(), c2.getFechaHoraTimestamp()));

        long ahora = System.currentTimeMillis();
        List<Cita> proximas = new ArrayList<>();
        for (Cita cita : delCliente) {
            long timestamp = cita.getFechaHoraTimestamp();
            if (timestamp == 0 || timestamp >= ahora) {
                proximas.add(cita);
            }
        }

        List<Cita> fuente = proximas.isEmpty() ? delCliente : proximas;
        int limite = Math.min(2, fuente.size());
        for (int i = 0; i < limite; i++) {
            View itemView = getLayoutInflater().inflate(R.layout.item_cita_cliente, llCitasContainer, false);
            bindResumenCita(itemView, fuente.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                int spacing = (int) (getResources().getDisplayMetrics().density * 12);
                params.topMargin = spacing;
            }
            llCitasContainer.addView(itemView, params);
        }
        toggleCitasEmptyState(false);
    }

    private void bindResumenCita(View view, Cita cita) {
        if (view == null || cita == null) {
            return;
        }
        TextView tvPaciente = view.findViewById(R.id.tvPacienteNombre);
        TextView tvFechaHora = view.findViewById(R.id.tvFechaHora);
        TextView tvMotivo = view.findViewById(R.id.tvMotivo);
        TextView tvEstado = view.findViewById(R.id.tvEstadoCita);

        String nombreMascota = !TextUtils.isEmpty(cita.getPacienteNombre())
                ? cita.getPacienteNombre()
                : getString(R.string.text_cliente_mascota_sin_nombre);
        tvPaciente.setText(nombreMascota);

        String fechaTexto = cita.getFecha();
        String horaTexto = cita.getHora();
        if (TextUtils.isEmpty(fechaTexto) || TextUtils.isEmpty(horaTexto)) {
            long timestamp = cita.getFechaHoraTimestamp();
            if (timestamp > 0) {
                Date date = new Date(timestamp);
                if (TextUtils.isEmpty(fechaTexto)) {
                    fechaTexto = fechaResumenFormat.format(date);
                }
                if (TextUtils.isEmpty(horaTexto)) {
                    horaTexto = horaResumenFormat.format(date);
                }
            }
        }
        if (TextUtils.isEmpty(fechaTexto)) {
            fechaTexto = "--";
        }
        if (TextUtils.isEmpty(horaTexto)) {
            horaTexto = "--";
        }
        tvFechaHora.setText(getString(R.string.cita_cliente_fecha_hora, fechaTexto, horaTexto));

        String motivo = TextUtils.isEmpty(cita.getMotivo())
                ? getString(R.string.cita_cliente_sin_motivo)
                : cita.getMotivo();
        tvMotivo.setText(motivo);

        String estado = !TextUtils.isEmpty(cita.getEstado())
                ? cita.getEstado()
                : getString(R.string.cita_estado_pendiente);
        tvEstado.setText(estado);
        aplicarEstiloEstado(tvEstado, estado);
    }

    private void toggleCitasEmptyState(boolean mostrarVacio) {
        if (tvCitasEmpty != null) {
            tvCitasEmpty.setVisibility(mostrarVacio ? View.VISIBLE : View.GONE);
        }
        if (llCitasContainer != null) {
            llCitasContainer.setVisibility(mostrarVacio ? View.GONE : View.VISIBLE);
        }
        if (btnVerCitas != null) {
            btnVerCitas.setVisibility(View.VISIBLE);
        }
    }

    private void aplicarEstiloEstado(TextView chip, String estado) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(R.drawable.bg_estado_chip);
        chip.getBackground().setTint(obtenerColorEstado(estado));
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private int obtenerColorEstado(String estado) {
        if (estado == null) {
            return ContextCompat.getColor(this, R.color.estadoPendienteColor);
        }
        switch (estado.toLowerCase(Locale.getDefault())) {
            case "confirmada":
                return ContextCompat.getColor(this, R.color.estadoConfirmadaColor);
            case "pospuesta":
                return ContextCompat.getColor(this, R.color.estadoPospuestaColor);
            case "cancelada":
                return ContextCompat.getColor(this, R.color.estadoCanceladaColor);
            case "completada":
                return ContextCompat.getColor(this, R.color.estadoCompletadaColor);
            default:
                return ContextCompat.getColor(this, R.color.estadoPendienteColor);
        }
    }

    private void abrirListadoCitas() {
        Intent intent = new Intent(this, ClienteCitaListadoActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
