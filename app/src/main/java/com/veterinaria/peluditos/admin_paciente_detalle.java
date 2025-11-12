package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.veterinaria.peluditos.adapters.HistorialMedicoAdapter;
import com.veterinaria.peluditos.data.HistorialMedico;
import com.veterinaria.peluditos.data.Paciente;

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
    private RecyclerView recyclerViewHistory;
    private TextView tvEmptyHistorial;
    private TabLayout tabLayout;

    private HistorialMedicoAdapter historialAdapter;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminHistorialMedicoViewModel historialMedicoViewModel;

    private String pacienteId;
    private Paciente pacienteActual;
    private List<HistorialMedico> historialActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_paciente_detalle);

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

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> abrirEditarPaciente());
        btnAgregarHistorial.setOnClickListener(v -> abrirHistorialMedico());
    }

    private void setupRecyclerView() {
        historialAdapter = new HistorialMedicoAdapter();
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
        ImageView iconClientes = findViewById(R.id.iconClientes);
        ImageView iconPacientes = findViewById(R.id.iconPacientes);
        ImageView iconCitas = findViewById(R.id.iconCitas);
        ImageView iconPerfil = findViewById(R.id.iconPerfil);

        if (iconClientes != null) {
            ((View) iconClientes.getParent()).setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminUsuarioClienteListadoActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (iconPacientes != null) {
            ((View) iconPacientes.getParent()).setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPacienteListadoActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (iconCitas != null) {
            ((View) iconCitas.getParent()).setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_cita_listado.class);
                startActivity(intent);
                finish();
            });
        }

        if (iconPerfil != null) {
            ((View) iconPerfil.getParent()).setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPerfil.class);
                startActivity(intent);
            });
        }
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

        tvClientName.setText(!TextUtils.isEmpty(paciente.getClienteNombre())
                ? paciente.getClienteNombre()
                : getString(R.string.paciente_sin_cliente));
        tvClientPhone.setText(R.string.msg_seleccione_cliente);

        ivPatientPhoto.setImageResource(R.drawable.paciente);
        ivClientPhoto.setImageResource(R.drawable.user_sofia);
    }

    private void manejarSeleccionTab(int position) {
        if (position == 0) {
            recyclerViewHistory.setVisibility(View.VISIBLE);
            btnAgregarHistorial.setVisibility(View.VISIBLE);
            updateEmptyState();
        } else {
            recyclerViewHistory.setVisibility(View.GONE);
            btnAgregarHistorial.setVisibility(View.GONE);
            tvEmptyHistorial.setVisibility(View.VISIBLE);
            tvEmptyHistorial.setText(R.string.msg_feature_en_construccion);
        }
    }

    private void updateEmptyState() {
        if (tabLayout.getSelectedTabPosition() != 0) {
            return;
        }
        boolean isEmpty = historialActual == null || historialActual.isEmpty();
        tvEmptyHistorial.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            tvEmptyHistorial.setText(R.string.empty_historial);
        }
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
}
