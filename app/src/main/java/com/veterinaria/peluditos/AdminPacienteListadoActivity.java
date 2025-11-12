package com.veterinaria.peluditos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.veterinaria.peluditos.adapters.PacienteAdapter;
import com.veterinaria.peluditos.data.Paciente;

public class AdminPacienteListadoActivity extends AppCompatActivity {

    private PacienteAdapter pacienteAdapter;
    private TextView tvEmptyState;
    private AdminPacienteViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_paciente_listado);

        viewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        setupRecyclerView();
        setupAddPacienteButton();
        observePacientes();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewPacientes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        pacienteAdapter = new PacienteAdapter();
        recyclerView.setAdapter(pacienteAdapter);

        pacienteAdapter.setOnPacienteActionListener(new PacienteAdapter.OnPacienteActionListener() {
            @Override
            public void onEdit(Paciente paciente) {
                Intent intent = new Intent(AdminPacienteListadoActivity.this, AdminPacienteEditarActivity.class);
                intent.putExtra(AdminPacienteEditarActivity.EXTRA_PACIENTE_ID, paciente.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Paciente paciente) {
                mostrarDialogoEliminar(paciente);
            }

            @Override
            public void onView(Paciente paciente) {
                Intent intent = new Intent(AdminPacienteListadoActivity.this, admin_paciente_detalle.class);
                intent.putExtra(admin_paciente_detalle.EXTRA_PACIENTE_ID, paciente.getId());
                startActivity(intent);
            }
        });

        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void observePacientes() {
        viewModel.getAllPacientes().observe(this, pacientes -> {
            pacienteAdapter.setPacientes(pacientes);
            boolean isEmpty = pacientes == null || pacientes.isEmpty();
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupAddPacienteButton() {
        ImageButton btnAddPaciente = findViewById(R.id.btnAddPaciente);
        if (btnAddPaciente != null) {
            btnAddPaciente.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPacienteNuevoActivity.class);
                startActivity(intent);
            });
        }
    }

    private void mostrarDialogoEliminar(Paciente paciente) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_confirm_delete_patient, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        tvTitle.setText(R.string.confirm_eliminar_paciente_title);
        tvMessage.setText(getString(R.string.confirm_eliminar_paciente_message, paciente.getNombre()));

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            viewModel.delete(paciente);
            if (isNetworkAvailable()) {
                Toast.makeText(this, R.string.msg_paciente_eliminado, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.msg_paciente_eliminado_pendiente, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
