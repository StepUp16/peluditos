package com.veterinaria.peluditos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.util.NetworkUtils;
import com.veterinaria.peluditos.util.PacientePhotoManager;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

public class DetallePacienteClienteActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private ShapeableImageView ivPacientePhoto;
    private ShapeableImageView ivOwnerPhoto;
    private ImageButton btnCambiarFoto;
    private CircularProgressIndicator photoProgress;
    private TextView tvPacienteName;
    private TextView tvPacienteEspecie;
    private Chip chipEdad;
    private Chip chipPeso;
    private Chip chipSexo;
    private TextView tvOwnerName;
    private TextView tvOwnerPhone;
    private TextView tvOwnerEmail;
    private TextView tvUltimaActualizacion;
    private TabLayout tabLayout;
    private RecyclerView recyclerHistorial;
    private TextView tvEmptyList;

    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private PacientePhotoManager photoManager;
    private ActivityResultLauncher<String> photoPickerLauncher;
    private Paciente pacienteActual;
    private String pacienteId;

    private boolean ignoreFirstMenuSelection = true;

    private final DecimalFormat pesoFormat = new DecimalFormat("#.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalles_paciente_cliente);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        configurarMenuInferior();
        initViews();
        setupTabs();

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

        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);

        pacienteViewModel.getPaciente(pacienteId).observe(this, paciente -> {
            if (paciente == null) {
                Toast.makeText(this, R.string.error_paciente, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            pacienteActual = paciente;
            renderPaciente(paciente);
            cargarDatosPropietario(paciente.getClienteId());
        });
    }

    private void setupTabs() {
        if (tabLayout == null) {
            return;
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                actualizarSeccionSeleccionada(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                actualizarSeccionSeleccionada(tab.getPosition());
            }
        });

        int initial = tabLayout.getSelectedTabPosition();
        if (initial < 0) {
            initial = 0;
        }
        actualizarSeccionSeleccionada(initial);
    }

    private void actualizarSeccionSeleccionada(int position) {
        if (tvEmptyList == null) {
            return;
        }
        int message;
        if (position == 1) {
            message = R.string.empty_citas_futuras;
        } else if (position == 2) {
            message = R.string.empty_citas_pasadas;
        } else {
            message = R.string.empty_historial;
        }
        tvEmptyList.setText(message);
        tvEmptyList.setVisibility(View.VISIBLE);
        if (recyclerHistorial != null) {
            recyclerHistorial.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        ivPacientePhoto = findViewById(R.id.ivPacientePhoto);
        ivOwnerPhoto = findViewById(R.id.ivOwnerPhoto);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        photoProgress = findViewById(R.id.photoProgress);
        tvPacienteName = findViewById(R.id.tvPacienteName);
        tvPacienteEspecie = findViewById(R.id.tvPacienteEspecie);
        chipEdad = findViewById(R.id.chipEdad);
        chipPeso = findViewById(R.id.chipPeso);
        chipSexo = findViewById(R.id.chipSexo);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvOwnerPhone = findViewById(R.id.tvOwnerPhone);
        tvOwnerEmail = findViewById(R.id.tvOwnerEmail);
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerHistorial = findViewById(R.id.recyclerHistorial);
        tvEmptyList = findViewById(R.id.tvEmptyList);

        Button btnVerHistorial = findViewById(R.id.btnVerHistorial);
        Button btnProgramarCita = findViewById(R.id.btnProgramarCita);

        btnCambiarFoto.setOnClickListener(v -> seleccionarNuevaFoto());
        btnVerHistorial.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_historial_cliente_no_disponible, Toast.LENGTH_SHORT).show());
        btnProgramarCita.setOnClickListener(v -> {
            Intent intent = new Intent(this, NuevaCitaClienteActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        if (recyclerHistorial != null) {
            recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
            recyclerHistorial.setVisibility(View.GONE);
        }
        if (tvEmptyList != null) {
            tvEmptyList.setVisibility(View.VISIBLE);
            tvEmptyList.setText(R.string.empty_historial);
        }
    }

    private void configurarMenuInferior() {
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        menu.setOnItemSelectedListener(item -> {
            if (ignoreFirstMenuSelection) {
                ignoreFirstMenuSelection = false;
                return true;
            }
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, ClienteMainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_mascotas) {
                return true;
            } else if (itemId == R.id.nav_citas) {
                startActivity(new Intent(this, ClienteCitaListadoActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilClienteActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        menu.setSelectedItemId(R.id.nav_mascotas);
    }

    private void renderPaciente(Paciente paciente) {
        tvPacienteName.setText(paciente.getNombre());
        tvPacienteEspecie.setText(buildEspecieRaza(paciente));

        int edad = Math.max(0, paciente.getEdad());
        chipEdad.setText(getString(R.string.chip_edad_formato, edad));

        double peso = paciente.getPeso();
        String pesoTexto = peso > 0 ? pesoFormat.format(peso) : "--";
        chipPeso.setText(getString(R.string.chip_peso_formato, pesoTexto));

        String sexo = TextUtils.isEmpty(paciente.getSexo())
                ? getString(R.string.placeholder_select_sexo)
                : paciente.getSexo();
        chipSexo.setText(getString(R.string.chip_sexo_formato, sexo));

        String fechaTexto = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date(paciente.getTimestampModificacion()));
        tvUltimaActualizacion.setText(getString(R.string.text_paciente_ultima_actualizacion, fechaTexto));

        loadPacientePhoto(paciente.getFotoUrl());
    }

    private CharSequence buildEspecieRaza(Paciente paciente) {
        String especie = paciente.getEspecie() != null ? paciente.getEspecie().trim() : "";
        String raza = paciente.getRaza() != null ? paciente.getRaza().trim() : "";

        if (TextUtils.isEmpty(especie) && TextUtils.isEmpty(raza)) {
            return getString(R.string.placeholder_select_especie);
        }
        if (TextUtils.isEmpty(raza)) {
            return especie;
        }
        if (TextUtils.isEmpty(especie)) {
            return raza;
        }
        return especie + " • " + raza;
    }

    private void cargarDatosPropietario(String clienteId) {
        if (TextUtils.isEmpty(clienteId)) {
            mostrarDatosPropietario(null);
            return;
        }
        usuarioViewModel.getUsuario(clienteId).observe(this, this::mostrarDatosPropietario);
    }

    private void mostrarDatosPropietario(Usuario usuario) {
        if (usuario == null && pacienteActual != null) {
            tvOwnerName.setText(pacienteActual.getClienteNombre());
            tvOwnerPhone.setText(getString(R.string.text_cliente_owner_phone, getString(R.string.text_dato_no_disponible)));
            tvOwnerEmail.setText(getString(R.string.text_cliente_owner_email, getString(R.string.text_dato_no_disponible)));
            ivOwnerPhoto.setImageResource(R.drawable.icono_perfil);
            return;
        }

        if (usuario != null) {
            tvOwnerName.setText(usuario.getNombre() + " " + usuario.getApellido());

            String telefono = TextUtils.isEmpty(usuario.getTelefono())
                    ? getString(R.string.text_dato_no_disponible)
                    : usuario.getTelefono();
            tvOwnerPhone.setText(getString(R.string.text_cliente_owner_phone, telefono));

            String correo = TextUtils.isEmpty(usuario.getEmail())
                    ? getString(R.string.text_dato_no_disponible)
                    : usuario.getEmail();
            tvOwnerEmail.setText(getString(R.string.text_cliente_owner_email, correo));

            if (!TextUtils.isEmpty(usuario.getFotoUrl())) {
                Glide.with(this)
                        .load(usuario.getFotoUrl())
                        .placeholder(R.drawable.icono_perfil)
                        .error(R.drawable.icono_perfil)
                        .into(ivOwnerPhoto);
            } else {
                ivOwnerPhoto.setImageResource(R.drawable.icono_perfil);
            }
        }
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
        if (pacienteActual == null || TextUtils.isEmpty(pacienteId)) {
            return;
        }
        mostrarCargaFoto(true);
        Toast.makeText(this, R.string.msg_foto_subiendo, Toast.LENGTH_SHORT).show();
        photoManager.uploadPhoto(getApplicationContext(), uri, pacienteId,
                new PacientePhotoManager.UploadCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull String downloadUrl) {
                        pacienteActual.setFotoUrl(downloadUrl);
                        pacienteViewModel.update(pacienteActual);
                        loadPacientePhoto(downloadUrl);
                        mostrarCargaFoto(false);
                        Toast.makeText(DetallePacienteClienteActivity.this,
                                R.string.msg_foto_actualizada,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception exception) {
                        mostrarCargaFoto(false);
                        Toast.makeText(DetallePacienteClienteActivity.this,
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
            btnCambiarFoto.setAlpha(mostrando ? 0.4f : 1f);
        }
    }

    private void loadPacientePhoto(String fotoUrl) {
        if (TextUtils.isEmpty(fotoUrl)) {
            ivPacientePhoto.setImageResource(R.drawable.paciente);
            return;
        }
        Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.paciente)
                .error(R.drawable.paciente)
                .into(ivPacientePhoto);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoManager != null) {
            photoManager.dispose();
        }
    }
}
