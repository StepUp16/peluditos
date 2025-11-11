package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.veterinaria.peluditos.adapters.CitaAdapter;
import com.veterinaria.peluditos.data.Cita;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class admin_cita_listado extends AppCompatActivity {

    private CitaAdapter citaAdapter;
    private TextView tvEmptyState;
    private TextView tvHeader;
    private CalendarView calendarView;
    private TabLayout tabLayout;
    private AdminCitaViewModel citaViewModel;

    private final List<Cita> todasLasCitas = new ArrayList<>();
    private boolean isCalendarMode = true;
    private long selectedDateMillis;

    private LinearLayout iconHomeContainer;
    private LinearLayout iconCitasContainer;
    private LinearLayout iconPacientesContainer;
    private LinearLayout iconClientesContainer;
    private LinearLayout iconPerfilContainer;
    private String[] estadosDisponibles;

    private final SimpleDateFormat headerDateFormat =
            new SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_cita_listado);

        selectedDateMillis = startOfDay(System.currentTimeMillis());
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);
        estadosDisponibles = getResources().getStringArray(R.array.cita_estados_array);

        initViews();
        setupRecyclerView();
        setupAddCitaButton();
        setupBottomMenu();
        setupTabs();
        observeCitas();
    }

    private void initViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvHeader = findViewById(R.id.tvHeaderHoy);
        calendarView = findViewById(R.id.calendarView);
        tabLayout = findViewById(R.id.tabLayout);

        if (calendarView != null) {
            calendarView.setDate(selectedDateMillis, false, true);
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                selectedDateMillis = cal.getTimeInMillis();
                updateHeaderDate();
                applyCurrentFilter();
            });
        }

        updateHeaderDate();

        iconHomeContainer = (LinearLayout) findViewById(R.id.iconHome).getParent();
        iconCitasContainer = (LinearLayout) findViewById(R.id.iconCitas).getParent();
        iconPacientesContainer = (LinearLayout) findViewById(R.id.iconPacientes).getParent();
        iconClientesContainer = (LinearLayout) findViewById(R.id.iconClientes).getParent();
        iconPerfilContainer = (LinearLayout) findViewById(R.id.iconPerfil).getParent();

        ImageView iconCitas = findViewById(R.id.iconCitas);
        if (iconCitas != null) {
            iconCitas.setColorFilter(ContextCompat.getColor(this, R.color.textColorPrimary));
        }
    }

    private void setupTabs() {
        if (tabLayout == null) {
            return;
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    isCalendarMode = true;
                    if (calendarView != null) {
                        calendarView.setVisibility(View.VISIBLE);
                    }
                    updateHeaderDate();
                } else {
                    isCalendarMode = false;
                    if (calendarView != null) {
                        calendarView.setVisibility(View.GONE);
                    }
                    tvHeader.setText(R.string.header_citas_general);
                }
                applyCurrentFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        TabLayout.Tab calendarTab = tabLayout.getTabAt(0);
        if (calendarTab != null) {
            calendarTab.select();
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewCitas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        citaAdapter = new CitaAdapter();
        recyclerView.setAdapter(citaAdapter);

        citaAdapter.setOnCitaActionListener(new CitaAdapter.OnCitaActionListener() {
            @Override
            public void onCitaClick(Cita cita) {
                openCitaDetail(cita);
            }

            @Override
            public void onCambiarEstado(Cita cita) {
                showEstadoDialog(cita);
            }
        });
    }

    private void setupAddCitaButton() {
        ImageButton btnAddCita = findViewById(R.id.btnAddCita);
        if (btnAddCita != null) {
            btnAddCita.setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_cita_nueva.class);
                startActivity(intent);
            });
        }
    }

    private void setupBottomMenu() {
        if (iconHomeContainer != null) {
            iconHomeContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_home.class);
                startActivity(intent);
                finish();
            });
        }

        if (iconCitasContainer != null) {
            iconCitasContainer.setOnClickListener(v -> {
                // ya estamos aquí
            });
        }

        if (iconPacientesContainer != null) {
            iconPacientesContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPacienteListadoActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (iconClientesContainer != null) {
            iconClientesContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminUsuarioClienteListadoActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (iconPerfilContainer != null) {
            iconPerfilContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPerfil.class);
                startActivity(intent);
            });
        }
    }

    private void observeCitas() {
        citaViewModel.getAllCitas().observe(this, citas -> {
            todasLasCitas.clear();
            if (citas != null) {
                todasLasCitas.addAll(citas);
            }
            applyCurrentFilter();
        });
    }

    private void applyCurrentFilter() {
        List<Cita> resultado = new ArrayList<>();
        if (isCalendarMode) {
            long dayStart = startOfDay(selectedDateMillis);
            long dayEnd = dayStart + 24 * 60 * 60 * 1000L;
            for (Cita cita : todasLasCitas) {
                long citaTime = cita.getFechaHoraTimestamp();
                if (citaTime >= dayStart && citaTime < dayEnd) {
                    resultado.add(cita);
                }
            }
        } else {
            resultado.addAll(todasLasCitas);
        }

        citaAdapter.setCitas(resultado);
        boolean isEmpty = resultado.isEmpty();
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void updateHeaderDate() {
        if (tvHeader != null) {
            tvHeader.setText(headerDateFormat.format(new Date(selectedDateMillis)));
        }
    }

    private long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void openCitaDetail(Cita cita) {
        Intent intent = new Intent(this, admin_cita_nueva.class);
        startActivity(intent);
    }

    private void showEstadoDialog(Cita cita) {
        if (cita == null) {
            return;
        }
        int checked = 0;
        String estadoActual = cita.getEstado();
        if (estadoActual != null) {
            for (int i = 0; i < estadosDisponibles.length; i++) {
                if (estadoActual.equalsIgnoreCase(estadosDisponibles[i])) {
                    checked = i;
                    break;
                }
            }
        }
        final int[] selected = {checked};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cambiar_estado_title)
                .setSingleChoiceItems(estadosDisponibles, checked, (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.action_cancelar, null)
                .setPositiveButton(R.string.action_guardar, (dialog, which) -> {
                    citaViewModel.updateEstado(cita, estadosDisponibles[selected[0]]);
                    android.widget.Toast.makeText(this, R.string.msg_estado_actualizado, android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
