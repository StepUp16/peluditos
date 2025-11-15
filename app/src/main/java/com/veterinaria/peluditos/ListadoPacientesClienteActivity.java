package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.adapters.PacienteClienteAdapter;
import com.veterinaria.peluditos.data.Paciente;

import java.util.ArrayList;
import java.util.List;

public class ListadoPacientesClienteActivity extends AppCompatActivity {

    private final List<Paciente> cachePacientes = new ArrayList<>();
    private PacienteClienteAdapter adapter;
    private TextView tvEmptyState;
    private RecyclerView recyclerPacientes;

    private FirebaseAuth firebaseAuth;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private String clienteId;
    private boolean ignoreFirstMenuSelection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listado_pacientes_cliente);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add_pet) {
                abrirNuevoPaciente();
                return true;
            }
            return false;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.btnMenuCliente);
        configurarMenuInferior(bottomNavigationView);

        recyclerPacientes = findViewById(R.id.recyclerPacientes);
        recyclerPacientes.setLayoutManager(new LinearLayoutManager(this));
        recyclerPacientes.setHasFixedSize(true);

        adapter = new PacienteClienteAdapter();
        recyclerPacientes.setAdapter(adapter);
        adapter.setOnPacienteClickListener(paciente -> {
            Intent intent = new Intent(this, DetallePacienteClienteActivity.class);
            intent.putExtra(DetallePacienteClienteActivity.EXTRA_PACIENTE_ID, paciente.getId());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        tvEmptyState = findViewById(R.id.tvEmptyState);

        firebaseAuth = FirebaseAuth.getInstance();
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        clienteId = currentUser.getUid();
        resolverClienteDesdeUsuario(currentUser);

        pacienteViewModel.getAllPacientes().observe(this, pacientes -> {
            cachePacientes.clear();
            if (pacientes != null) {
                cachePacientes.addAll(pacientes);
            }
            aplicarFiltro();
        });
    }

    private void resolverClienteDesdeUsuario(FirebaseUser user) {
        String email = user.getEmail();
        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    clienteId = usuario.getUid();
                    aplicarFiltro();
                }
            });
        }
    }

    private void aplicarFiltro() {
        List<Paciente> filtrados = new ArrayList<>();
        if (!TextUtils.isEmpty(clienteId)) {
            for (Paciente paciente : cachePacientes) {
                if (clienteId.equals(paciente.getClienteId())) {
                    filtrados.add(paciente);
                }
            }
        }

        adapter.setPacientes(filtrados);
        boolean vacio = filtrados.isEmpty();
        tvEmptyState.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recyclerPacientes.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void configurarMenuInferior(BottomNavigationView menu) {
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

    private void abrirNuevoPaciente() {
        startActivity(new Intent(this, NuevoPacienteClienteActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
