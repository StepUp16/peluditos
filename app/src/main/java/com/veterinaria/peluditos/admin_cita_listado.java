package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
    private ChipGroup chipGroupEstados;

    private final List<Cita> todasLasCitas = new ArrayList<>();
    private boolean isCalendarMode = true;
    private long selectedDateMillis;
    private String estadoFiltroActual;

    private String[] estadosDisponibles;

    private final SimpleDateFormat headerDateFormat =
            new SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault());
    private final SimpleDateFormat fechaFormato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat horaFormato = new SimpleDateFormat("HH:mm", Locale.getDefault());

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
        //setupBottomMenu();
        setupTabs();
        observeCitas();

        BottomNavigationView bottomNav = findViewById(R.id.bottomMenu);
        bottomNav.setSelectedItemId(R.id.iconCitas); // Establece el ítem seleccionado inicialmente


        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId(); // Obtiene el ID del ítem presionado

            if (itemId == R.id.iconCitas) {
                return true;
            }
            else if (itemId == R.id.iconHome) {
                Intent intent = new Intent(admin_cita_listado.this, admin_home.class);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.iconPacientes) {
                Intent intent = new Intent(admin_cita_listado.this, AdminPacienteListadoActivity.class);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.iconClientes) {
                Intent intent = new Intent(admin_cita_listado.this, AdminUsuarioClienteListadoActivity.class);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.iconPerfil) {
                Intent intent = new Intent(admin_cita_listado.this, AdminPerfil.class);
                startActivity(intent);
                return true;
            }

            return false; // false = la selección no fue manejada
        });
    }

    private void initViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvHeader = findViewById(R.id.tvHeaderHoy);
        calendarView = findViewById(R.id.calendarView);
        tabLayout = findViewById(R.id.tabLayout);
        chipGroupEstados = findViewById(R.id.chipGroupEstados);

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


        setupEstadoChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomMenu);
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.iconCitas) {
            bottomNav.setSelectedItemId(R.id.iconCitas);
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

   /* private void setupBottomMenu() {
        if (iconHomeContainer != null) {
            iconHomeContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, admin_home.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconCitasContainer != null) {
            iconCitasContainer.setOnClickListener(v -> {
                // ya estamos en Citas; opcionalmente podríamos refrescar o hacer scroll
            });
        }

        if (iconPacientesContainer != null) {
            iconPacientesContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPacienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconClientesContainer != null) {
            iconClientesContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminUsuarioClienteListadoActivity.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }

        if (iconPerfilContainer != null) {
            iconPerfilContainer.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPerfil.class);
                startActivityWithAnimation(intent);
                finish();
            });
        }
    }*/

    private void startActivityWithAnimation(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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

        if (!TextUtils.isEmpty(estadoFiltroActual)) {
            List<Cita> filtradasPorEstado = new ArrayList<>();
            for (Cita cita : resultado) {
                String estado = cita.getEstado();
                if (!TextUtils.isEmpty(estado) &&
                        estado.equalsIgnoreCase(estadoFiltroActual)) {
                    filtradasPorEstado.add(cita);
                }
            }
            resultado = filtradasPorEstado;
        }

        citaAdapter.setCitas(resultado);
        boolean isEmpty = resultado.isEmpty();
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void setupEstadoChips() {
        if (chipGroupEstados == null) {
            return;
        }
        chipGroupEstados.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                estadoFiltroActual = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipEstadoTodos) {
                    estadoFiltroActual = null;
                } else {
                    Chip chip = group.findViewById(checkedId);
                    estadoFiltroActual = chip != null ? chip.getText().toString() : null;
                }
            }
            applyCurrentFilter();
        });
        chipGroupEstados.check(R.id.chipEstadoTodos);
    }
    private void updateHeaderDate() {
        if (tvHeader == null) {
            return;
        }
        if (isCalendarMode) {
            tvHeader.setText(headerDateFormat.format(new Date(selectedDateMillis)));
        } else {
            tvHeader.setText(R.string.header_citas_general);
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
        Intent intent = new Intent(this, admin_cita_editar.class);
        if (cita != null && !TextUtils.isEmpty(cita.getId())) {
            intent.putExtra(admin_cita_editar.EXTRA_CITA_ID, cita.getId());
        }
        startActivity(intent);
    }

    private void showEstadoDialog(Cita cita) {
        if (cita == null) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        CitaEstadoDialogFragment dialog = CitaEstadoDialogFragment.newInstance(
                cita.getId(),
                cita.getEstado(),
                cita.getFechaHoraTimestamp(),
                cita.getNotaEstado()
        );
        dialog.setOnEstadoConfirmadoListener((citaId, nuevoEstado, nuevaFechaMillis, nota) -> {
            Cita citaSeleccionada = buscarCitaPorId(citaId);
            if (citaSeleccionada != null) {
                citaSeleccionada.setEstado(nuevoEstado);
                if (nuevoEstado.equalsIgnoreCase(getString(R.string.cita_estado_pospuesta)) && nuevaFechaMillis > 0) {
                    citaSeleccionada.setFecha(fechaFormato.format(new Date(nuevaFechaMillis)));
                    citaSeleccionada.setHora(horaFormato.format(new Date(nuevaFechaMillis)));
                    citaSeleccionada.setFechaHoraTimestamp(nuevaFechaMillis);
                }
                if (!TextUtils.isEmpty(nota)) {
                    citaSeleccionada.setNotaEstado(nota);
                }
                citaViewModel.update(citaSeleccionada);
                Toast.makeText(this, R.string.msg_estado_actualizado, Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show(fm, "CitaEstadoDialog");
    }

    private Cita buscarCitaPorId(String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        for (Cita cita : todasLasCitas) {
            if (id.equals(cita.getId())) {
                return cita;
            }
        }
        return null;
    }
}
