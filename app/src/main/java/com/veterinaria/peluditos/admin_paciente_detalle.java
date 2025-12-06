package com.veterinaria.peluditos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.veterinaria.peluditos.adapters.HistorialMedicoAdapter;
import com.veterinaria.peluditos.adapters.PacienteCitaAdapter;
import com.veterinaria.peluditos.data.Cita;
import com.veterinaria.peluditos.data.HistorialMedico;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.util.NetworkUtils;
import com.veterinaria.peluditos.util.PacientePhotoManager;

import java.util.List;
import java.util.Locale;

public class admin_paciente_detalle extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private ShapeableImageView ivPatientPhoto;
    private TextView tvPatientName;
    private TextView tvPatientBreed;
    private TextView tvPatientInfo;
    private ShapeableImageView ivClientPhoto;
    private TextView tvClientName;
    private TextView tvClientPhone;
    private Button btnEdit;
    private Button btnAgregarHistorial;
    private ImageButton btnChangePhoto;
    private ProgressBar photoProgress;
    private RecyclerView recyclerViewHistory;
    private TextView tvEmptyHistorial;
    private TabLayout tabLayout;

    private HistorialMedicoAdapter historialAdapter;
    private PacienteCitaAdapter citaAdapter;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminHistorialMedicoViewModel historialMedicoViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private AdminCitaViewModel citaViewModel;

    private String pacienteId;
    private Paciente pacienteActual;
    private List<HistorialMedico> historialActual;
    private List<Cita> citasFuturas;
    private List<Cita> citasPasadas;
    private PacientePhotoManager photoManager;
    private ActivityResultLauncher<String> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_paciente_detalle);

        photoManager = new PacientePhotoManager();
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        subirFotoPaciente(uri);
                    }
                }
        );

        pacienteId = getIntent() != null ? getIntent().getStringExtra(EXTRA_PACIENTE_ID) : null;
        if (TextUtils.isEmpty(pacienteId)) {
            Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupTabLayout();
        setupBottomMenu();

        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        historialMedicoViewModel = new ViewModelProvider(this).get(AdminHistorialMedicoViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);

        pacienteViewModel.getPaciente(pacienteId).observe(this, paciente -> {
            if (paciente == null) {
                Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            pacienteActual = paciente;
            renderPaciente(paciente);
        });

        historialMedicoViewModel.getPorPaciente(pacienteId).observe(this, historiales -> {
            historialActual = historiales;
            historialAdapter.setHistoriales(historiales);
            updateEmptyState();
        });

        citaViewModel.getAllCitas().observe(this, citas -> {
            if (citas == null) {
                citasFuturas = null;
                citasPasadas = null;
            } else {
                long now = System.currentTimeMillis();
                citasFuturas = new java.util.ArrayList<>();
                citasPasadas = new java.util.ArrayList<>();
                for (Cita cita : citas) {
                    if (!TextUtils.equals(cita.getPacienteId(), pacienteId)) continue;
                    long time = cita.getFechaHoraTimestamp();
                    if (time >= now) {
                        citasFuturas.add(cita);
                    } else {
                        citasPasadas.add(cita);
                    }
                }
            }
            updateCitaAdapter();
        });
    }

    private void initViews() {
        ivPatientPhoto = findViewById(R.id.ivPatientPhoto);
        tvPatientName = findViewById(R.id.tvPatientName);
        tvPatientBreed = findViewById(R.id.tvPatientBreed);
        tvPatientInfo = findViewById(R.id.tvPatientInfo);
        ivClientPhoto = findViewById(R.id.ivClientPhoto);
        tvClientName = findViewById(R.id.tvClientName);
        tvClientPhone = findViewById(R.id.tvClientPhone);
        btnEdit = findViewById(R.id.btnEdit);
        btnAgregarHistorial = findViewById(R.id.btnAgregarHistorial);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        tvEmptyHistorial = findViewById(R.id.tvEmptyHistorial);
        tabLayout = findViewById(R.id.tabLayout);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        photoProgress = findViewById(R.id.photoProgress);

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> abrirEditarPaciente());
        btnAgregarHistorial.setOnClickListener(v -> abrirHistorialMedico());
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> seleccionarNuevaFoto());
        }
    }

    private void setupRecyclerView() {
        historialAdapter = new HistorialMedicoAdapter();
        citaAdapter = new PacienteCitaAdapter();
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(historialAdapter);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                manejarSeleccionTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
        manejarSeleccionTab(0);
    }

    private void setupBottomMenu() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomMenu);
        bottomNav.setSelectedItemId(R.id.iconPacientes);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.iconHome) {
                Intent intent = new Intent(this, admin_home.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityWithAnimation(intent);
                finish();
                return true;
            } else if (itemId == R.id.iconCitas) {
                Intent intent = new Intent(this, admin_cita_listado.class);
                startActivityWithAnimation(intent);
                finish();
                return true;
            } else if (itemId == R.id.iconPacientes) {
                // Ya estás en la pantalla de detalle de paciente, no hacer nada
                return true;
            } else if (itemId == R.id.iconClientes) {
                Intent intent = new Intent(this, AdminUsuarioClienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
                return true;
            } else if (itemId == R.id.iconPerfil) {
                Intent intent = new Intent(this, AdminPerfil.class);
                startActivityWithAnimation(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void startActivityWithAnimation(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void renderPaciente(Paciente paciente) {
        tvPatientName.setText(paciente.getNombre());

        String especie = !TextUtils.isEmpty(paciente.getEspecie()) ? paciente.getEspecie() : "-";
        String raza = !TextUtils.isEmpty(paciente.getRaza()) ? paciente.getRaza() : "";
        tvPatientBreed.setText(raza.isEmpty() ? especie : especie + ", " + raza);

        String info = String.format(Locale.getDefault(),
                "Edad: %d %s, Sexo: %s",
                paciente.getEdad(),
                paciente.getEdad() == 1 ? "año" : "años",
                !TextUtils.isEmpty(paciente.getSexo()) ? paciente.getSexo() : "-");
        tvPatientInfo.setText(info);

        String nombreCliente = !TextUtils.isEmpty(paciente.getClienteNombre())
                ? paciente.getClienteNombre()
                : getString(R.string.paciente_sin_cliente);
        tvClientName.setText(nombreCliente);

        cargarDatosCliente(paciente.getClienteId(), nombreCliente);
        loadPacientePhoto(paciente.getFotoUrl());
        ivClientPhoto.setImageResource(R.drawable.icono_perfil);
    }

    private void cargarDatosCliente(String clienteId, String nombreFallback) {
        if (TextUtils.isEmpty(clienteId)) {
            tvClientPhone.setText(R.string.msg_seleccione_cliente);
            ivClientPhoto.setImageResource(R.drawable.icono_perfil);
            return;
        }
        usuarioViewModel.getUsuario(clienteId).observe(this, usuario -> {
            if (usuario != null) {
                if (!TextUtils.isEmpty(usuario.getNombre()) || !TextUtils.isEmpty(usuario.getApellido())) {
                    String nombre = (usuario.getNombre() != null ? usuario.getNombre().trim() : "") + " " +
                            (usuario.getApellido() != null ? usuario.getApellido().trim() : "");
                    tvClientName.setText(nombre.trim());
                } else {
                    tvClientName.setText(nombreFallback);
                }

                if (!TextUtils.isEmpty(usuario.getTelefono())) {
                    tvClientPhone.setText(getString(R.string.cliente_telefono_formato, usuario.getTelefono()));
                } else {
                    tvClientPhone.setText(R.string.msg_seleccione_cliente);
                }
                cargarFotoCliente(usuario.getFotoUrl());
            } else {
                tvClientPhone.setText(R.string.msg_seleccione_cliente);
                tvClientName.setText(nombreFallback);
                ivClientPhoto.setImageResource(R.drawable.icono_perfil);
            }
        });
    }

    private void cargarFotoCliente(String fotoBase64) {
        // Placeholder dinámico como en listado de clientes
        android.graphics.drawable.Drawable imagenActual = ivClientPhoto.getDrawable();
        if (imagenActual == null) {
            ivClientPhoto.setImageResource(R.drawable.icono_perfil);
        }
        if (TextUtils.isEmpty(fotoBase64)) {
            ivClientPhoto.setImageResource(R.drawable.icono_perfil);
            return;
        }
        if (fotoBase64.startsWith("http")) {
            ivClientPhoto.setImageResource(R.drawable.icono_perfil);
            return;
        }
        try {
            byte[] imageByteArray = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT);
            Glide.with(this)
                    .asBitmap()
                    .load(imageByteArray)
                    .placeholder(imagenActual)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .centerCrop()
                    .into(ivClientPhoto);
        } catch (IllegalArgumentException e) {
            ivClientPhoto.setImageResource(R.drawable.icono_perfil);
        }
    }

    private void manejarSeleccionTab(int position) {
        if (position == 0) {
            recyclerViewHistory.setVisibility(View.VISIBLE);
            btnAgregarHistorial.setVisibility(View.VISIBLE);
            recyclerViewHistory.setAdapter(historialAdapter);
        } else {
            recyclerViewHistory.setVisibility(View.VISIBLE);
            btnAgregarHistorial.setVisibility(View.GONE);
            recyclerViewHistory.setAdapter(citaAdapter);
            updateCitaAdapter();
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (tabLayout.getSelectedTabPosition() != 0) {
            boolean isEmpty;
            if (tabLayout.getSelectedTabPosition() == 1) {
                isEmpty = citasFuturas == null || citasFuturas.isEmpty();
                tvEmptyHistorial.setText(R.string.empty_citas_futuras);
            } else {
                isEmpty = citasPasadas == null || citasPasadas.isEmpty();
                tvEmptyHistorial.setText(R.string.empty_citas_pasadas);
            }
            tvEmptyHistorial.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            return;
        }
        boolean isEmpty = historialActual == null || historialActual.isEmpty();
        tvEmptyHistorial.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            tvEmptyHistorial.setText(R.string.empty_historial);
        }
    }

    private void updateCitaAdapter() {
        if (tabLayout == null || citaAdapter == null) {
            return;
        }
        if (tabLayout.getSelectedTabPosition() == 1) {
            citaAdapter.setCitas(citasFuturas);
        } else if (tabLayout.getSelectedTabPosition() == 2) {
            citaAdapter.setCitas(citasPasadas);
        } else {
            return;
        }
        updateEmptyState();
    }

    private void abrirEditarPaciente() {
        if (pacienteActual == null) {
            return;
        }
        Intent intent = new Intent(this, AdminPacienteEditarActivity.class);
        intent.putExtra(AdminPacienteEditarActivity.EXTRA_PACIENTE_ID, pacienteActual.getId());
        startActivity(intent);
    }

    private void abrirHistorialMedico() {
        Intent intent = new Intent(this, admin_historial_medico.class);
        intent.putExtra(admin_historial_medico.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(admin_historial_medico.EXTRA_PACIENTE_NOMBRE,
                pacienteActual != null ? pacienteActual.getNombre() : "");
        startActivity(intent);
    }

    private void seleccionarNuevaFoto() {
        if (pacienteActual == null) {
            Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
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

    private void subirFotoPaciente(Uri uri) {
        if (pacienteId == null || photoManager == null) {
            return;
        }
        mostrarCargaFoto(true);
        Toast.makeText(this, R.string.msg_foto_subiendo, Toast.LENGTH_SHORT).show();
        photoManager.uploadPhoto(getApplicationContext(), uri, pacienteId,
                new PacientePhotoManager.UploadCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull String downloadUrl) {
                        if (pacienteActual != null) {
                            pacienteActual.setFotoUrl(downloadUrl);
                            pacienteViewModel.update(pacienteActual);
                            loadPacientePhoto(downloadUrl);
                        }
                        mostrarCargaFoto(false);
                        Toast.makeText(admin_paciente_detalle.this,
                                R.string.msg_foto_actualizada,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception exception) {
                        mostrarCargaFoto(false);
                        Toast.makeText(admin_paciente_detalle.this,
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

    private void loadPacientePhoto(String fotoUrl) {
        // 1. EL TRUCO DEL PLACEHOLDER:
        android.graphics.drawable.Drawable imagenActual = ivPatientPhoto.getDrawable();
        if (imagenActual == null) {
            ivPatientPhoto.setImageResource(R.drawable.paciente);
        }

        if (TextUtils.isEmpty(fotoUrl)) {
            ivPatientPhoto.setImageResource(R.drawable.paciente);
            return;
        }
        if (fotoUrl.startsWith("http")) {
            // Legacy URL (broken/paid) - Show placeholder immediately
            ivPatientPhoto.setImageResource(R.drawable.paciente);
        } else {
            try {
                byte[] imageByteArray = android.util.Base64.decode(fotoUrl, android.util.Base64.DEFAULT);
                Glide.with(this)
                        .asBitmap()
                        .load(imageByteArray)
                        .placeholder(imagenActual) // Dynamic placeholder
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .into(ivPatientPhoto);
            } catch (IllegalArgumentException e) {
                ivPatientPhoto.setImageResource(R.drawable.paciente);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoManager != null) {
            photoManager.dispose();
        }
    }
}
