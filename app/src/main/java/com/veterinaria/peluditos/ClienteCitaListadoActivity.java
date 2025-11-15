package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.adapters.CitaClienteAdapter;
import com.veterinaria.peluditos.data.Cita;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClienteCitaListadoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private CalendarView calendarView;
    private TabLayout tabLayout;
    private ChipGroup chipGroupEstados;
    private TextView tvHeaderHoy;
    private ExtendedFloatingActionButton fabNuevaCita;

    private FirebaseAuth firebaseAuth;
    private AdminCitaViewModel citaViewModel;
    private AdminUsuarioViewModel usuarioViewModel;

    private final List<Cita> cacheCitas = new ArrayList<>();
    private final CitaClienteAdapter citaAdapter = new CitaClienteAdapter();

    private String clienteId;
    private boolean ignoreMenuSelection = true;
    private boolean isCalendarMode = true;
    private long selectedDateMillis;
    private String estadoFiltroActual;

    private final SimpleDateFormat headerDateFormat =
            new SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cliente_cita_listado);

        firebaseAuth = FirebaseAuth.getInstance();
        citaViewModel = new ViewModelProvider(this).get(AdminCitaViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        selectedDateMillis = startOfDay(System.currentTimeMillis());

        setupToolbar();
        initViews();
        setupRecycler();
        setupFab();
        setupBottomMenu();
        setupTabs();
        setupEstadoChips();
        observeCitas();
        cargarClienteActual();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewCitas);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        calendarView = findViewById(R.id.calendarView);
        tabLayout = findViewById(R.id.tabLayout);
        chipGroupEstados = findViewById(R.id.chipGroupEstados);
        tvHeaderHoy = findViewById(R.id.tvHeaderHoy);
        fabNuevaCita = findViewById(R.id.fabNuevaCita);

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
    }

    private void setupRecycler() {
        if (recyclerView == null) {
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(citaAdapter);
    }

    private void setupFab() {
        if (fabNuevaCita == null) {
            return;
        }
        fabNuevaCita.setOnClickListener(v -> {
            Intent intent = new Intent(this, NuevaCitaClienteActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void setupBottomMenu() {
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        if (menu == null) {
            return;
        }
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
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilClienteActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
        menu.setSelectedItemId(R.id.nav_citas);
    }

    private void setupTabs() {
        if (tabLayout == null) {
            return;
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    isCalendarMode = true;
                    if (calendarView != null) {
                        calendarView.setVisibility(View.VISIBLE);
                    }
                } else {
                    isCalendarMode = false;
                    if (calendarView != null) {
                        calendarView.setVisibility(View.GONE);
                    }
                }
                updateHeaderDate();
                applyCurrentFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
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

    private void observeCitas() {
        citaViewModel.getAllCitas().observe(this, citas -> {
            cacheCitas.clear();
            if (citas != null) {
                cacheCitas.addAll(citas);
            }
            applyCurrentFilter();
        });
    }

    private void cargarClienteActual() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        clienteId = currentUser.getUid();
        String email = currentUser.getEmail();

        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    clienteId = usuario.getUid();
                    applyCurrentFilter();
                }
            });
        } else {
            usuarioViewModel.getUsuario(clienteId).observe(this, usuario -> {
                if (usuario != null) {
                    clienteId = usuario.getUid();
                    applyCurrentFilter();
                }
            });
        }

        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        List<Cita> delCliente = new ArrayList<>();
        if (!TextUtils.isEmpty(clienteId)) {
            for (Cita cita : cacheCitas) {
                if (clienteId.equals(cita.getClienteId())) {
                    delCliente.add(cita);
                }
            }
        }

        List<Cita> resultado = new ArrayList<>();
        if (isCalendarMode) {
            long dayStart = startOfDay(selectedDateMillis);
            long dayEnd = dayStart + 24 * 60 * 60 * 1000L;
            for (Cita cita : delCliente) {
                long citaTime = cita.getFechaHoraTimestamp();
                if (citaTime >= dayStart && citaTime < dayEnd) {
                    resultado.add(cita);
                }
            }
        } else {
            resultado.addAll(delCliente);
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
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void updateHeaderDate() {
        if (tvHeaderHoy == null) {
            return;
        }
        if (isCalendarMode) {
            tvHeaderHoy.setText(headerDateFormat.format(new Date(selectedDateMillis)));
        } else {
            tvHeaderHoy.setText(R.string.header_citas_general);
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
