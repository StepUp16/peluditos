package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veterinaria.peluditos.adapters.HistorialMedicoAdapter;
import com.veterinaria.peluditos.data.HistorialMedico;

import java.util.List;

public class admin_historial_medico extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";
    public static final String EXTRA_PACIENTE_NOMBRE = "extra_paciente_nombre";

    private HistorialMedicoAdapter historialAdapter;
    private AdminHistorialMedicoViewModel historialMedicoViewModel;
    private TextView tvSectionHistorial;
    private TextView tvHistorialEmpty;

    private String pacienteId;
    private String pacienteNombre;
    private List<HistorialMedico> historialActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_historial_medico);

        pacienteId = getIntent() != null ? getIntent().getStringExtra(EXTRA_PACIENTE_ID) : null;
        pacienteNombre = getIntent() != null ? getIntent().getStringExtra(EXTRA_PACIENTE_NOMBRE) : null;

        initViews();
        setupBottomMenu();
        setupRecyclerView();

        historialMedicoViewModel = new ViewModelProvider(this).get(AdminHistorialMedicoViewModel.class);
        if (!TextUtils.isEmpty(pacienteId)) {
            historialMedicoViewModel.getPorPaciente(pacienteId).observe(this, historiales -> {
                historialActual = historiales;
                historialAdapter.setHistoriales(historiales);
                updateEmptyState();
            });
        } else {
            historialMedicoViewModel.getTodos().observe(this, historiales -> {
                historialActual = historiales;
                historialAdapter.setHistoriales(historiales);
                updateEmptyState();
            });
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnNuevaEntrada = findViewById(R.id.btnNuevaEntrada);
        btnNuevaEntrada.setOnClickListener(v -> abrirNuevoHistorial());

        tvSectionHistorial = findViewById(R.id.tvSectionHistorial);
        if (!TextUtils.isEmpty(pacienteNombre)) {
            tvSectionHistorial.setText(getString(R.string.section_historial) + " • " + pacienteNombre);
        }

        tvHistorialEmpty = findViewById(R.id.tvHistorialEmpty);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historialAdapter = new HistorialMedicoAdapter();
        recyclerView.setAdapter(historialAdapter);
    }

    private void updateEmptyState() {
        boolean isEmpty = historialActual == null || historialActual.isEmpty();
        tvHistorialEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void abrirNuevoHistorial() {
        Intent intent = new Intent(this, admin_historial_medico_nuevo.class);
        if (!TextUtils.isEmpty(pacienteId)) {
            intent.putExtra(admin_historial_medico_nuevo.EXTRA_PACIENTE_ID, pacienteId);
            intent.putExtra(admin_historial_medico_nuevo.EXTRA_PACIENTE_NOMBRE, pacienteNombre);
        }
        startActivity(intent);
    }

    private void setupBottomMenu() {
        ImageView iconClientes = findViewById(R.id.iconClientes);
        ImageView iconPacientes = findViewById(R.id.iconPacientes);
        ImageView iconCitas = findViewById(R.id.iconCitas);
        ImageView iconPerfil = findViewById(R.id.iconPerfil);

        if (iconClientes != null) {
            ((View) iconClientes.getParent()).setOnClickListener(v -> {
                startActivity(new Intent(this, AdminUsuarioClienteListadoActivity.class));
                finish();
            });
        }

        if (iconPacientes != null) {
            ((View) iconPacientes.getParent()).setOnClickListener(v -> {
                startActivity(new Intent(this, AdminPacienteListadoActivity.class));
                finish();
            });
        }

        if (iconCitas != null) {
            ((View) iconCitas.getParent()).setOnClickListener(v -> {
                startActivity(new Intent(this, admin_cita_listado.class));
                finish();
            });
        }

        if (iconPerfil != null) {
            ((View) iconPerfil.getParent()).setOnClickListener(v -> {
                startActivity(new Intent(this, AdminPerfil.class));
            });
        }
    }
}
