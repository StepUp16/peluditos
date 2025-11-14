package com.veterinaria.peluditos;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pantalla para que los clientes agreguen sus propias mascotas.
 */
public class NuevoPacienteClienteActivity extends AppCompatActivity {

    private EditText etNombre;
    private EditText etEdad;
    private EditText etPeso;
    private Spinner spinnerSexo;
    private Spinner spinnerEspecie;
    private Spinner spinnerRaza;
    private Button btnGuardar;
    private TextView tvClienteActual;

    private FirebaseAuth firebaseAuth;
    private AdminPacienteViewModel pacienteViewModel;
    private AdminUsuarioViewModel usuarioViewModel;
    private final Map<String, Integer> razasPorEspecie = new LinkedHashMap<>();

    private String clienteId;
    private String clienteNombre;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nuevo_paciente_cliente);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        firebaseAuth = FirebaseAuth.getInstance();
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);

        initViews();
        initRazaLookup();
        setupSexoSpinner();
        setupEspecieSpinner();
        setupRazaSpinner();
        cargarClienteActual();

        btnGuardar.setOnClickListener(v -> guardarPaciente());
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombrePaciente);
        etEdad = findViewById(R.id.etEdad);
        etPeso = findViewById(R.id.etPeso);
        spinnerSexo = findViewById(R.id.spinnerSexo);
        spinnerEspecie = findViewById(R.id.spinnerEspecie);
        spinnerRaza = findViewById(R.id.spinnerRaza);
        btnGuardar = findViewById(R.id.btnGuardarPaciente);
        tvClienteActual = findViewById(R.id.tvClienteActual);
    }

    private void setupSexoSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sexo_paciente_array,
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerSexo.setAdapter(adapter);
    }

    private void initRazaLookup() {
        razasPorEspecie.put(getString(R.string.especie_perro), R.array.razas_perro_array);
        razasPorEspecie.put(getString(R.string.especie_gato), R.array.razas_gato_array);
        razasPorEspecie.put(getString(R.string.especie_ave), R.array.razas_ave_array);
        razasPorEspecie.put(getString(R.string.especie_reptil), R.array.razas_reptil_array);
        razasPorEspecie.put(getString(R.string.especie_otro), R.array.razas_otro_array);
    }

    private void setupEspecieSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.especies_array,
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerEspecie.setAdapter(adapter);
        spinnerEspecie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String especieSeleccionada = spinnerEspecie.getSelectedItem() != null
                        ? spinnerEspecie.getSelectedItem().toString()
                        : "";
                actualizarRazasParaEspecie(especieSeleccionada);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                actualizarRazasParaEspecie(null);
            }
        });
    }

    private void setupRazaSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.razas_placeholder_array,
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerRaza.setAdapter(adapter);
        spinnerRaza.setEnabled(false);
    }

    private void actualizarRazasParaEspecie(String especie) {
        int arrayRes = R.array.razas_placeholder_array;
        boolean habilitar = false;

        if (!TextUtils.isEmpty(especie)
                && razasPorEspecie.containsKey(especie)
                && !especie.equals(getString(R.string.placeholder_select_especie))) {
            arrayRes = razasPorEspecie.get(especie);
            habilitar = true;
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayRes,
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerRaza.setAdapter(adapter);
        spinnerRaza.setEnabled(habilitar);
    }

    private void cargarClienteActual() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            clienteId = null;
            clienteNombre = null;
            tvClienteActual.setText(R.string.text_cliente_no_disponible);
            return;
        }

        clienteId = user.getUid();
        tvClienteActual.setText(getString(R.string.text_cliente_actual,
                getString(R.string.text_cliente_sin_nombre)));

        String email = user.getEmail();
        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    clienteId = usuario.getUid();
                    clienteNombre = construirNombreCompleto(usuario);
                    tvClienteActual.setText(getString(R.string.text_cliente_actual, clienteNombre));
                } else {
                    asignarNombreDesdeFirebase(user);
                }
            });
        } else {
            asignarNombreDesdeFirebase(user);
        }
    }

    private void asignarNombreDesdeFirebase(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = user.getEmail();
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.text_cliente_sin_nombre);
        }
        clienteNombre = displayName;
        tvClienteActual.setText(getString(R.string.text_cliente_actual, clienteNombre));
    }

    private String construirNombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";

        if (!TextUtils.isEmpty(nombre) && !TextUtils.isEmpty(apellido)) {
            return nombre + " " + apellido;
        } else if (!TextUtils.isEmpty(nombre)) {
            return nombre;
        } else if (!TextUtils.isEmpty(apellido)) {
            return apellido;
        } else {
            return getString(R.string.text_cliente_sin_nombre);
        }
    }

    private void guardarPaciente() {
        if (clienteId == null) {
            Toast.makeText(this, R.string.error_user_no_auth, Toast.LENGTH_LONG).show();
            return;
        }

        String nombre = etNombre.getText().toString().trim();
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError(getString(R.string.error_campo_obligatorio));
            etNombre.requestFocus();
            return;
        }

        String especie = spinnerEspecie.getSelectedItem() != null
                ? spinnerEspecie.getSelectedItem().toString()
                : "";
        if (TextUtils.isEmpty(especie)
                || especie.equals(getString(R.string.placeholder_select_especie))) {
            Toast.makeText(this, R.string.error_especie, Toast.LENGTH_SHORT).show();
            spinnerEspecie.requestFocus();
            return;
        }

        String raza = spinnerRaza.getSelectedItem() != null
                ? spinnerRaza.getSelectedItem().toString()
                : "";
        if (!spinnerRaza.isEnabled()
                || TextUtils.isEmpty(raza)
                || raza.equals(getString(R.string.placeholder_select_raza))) {
            Toast.makeText(this, R.string.error_raza, Toast.LENGTH_SHORT).show();
            spinnerRaza.requestFocus();
            return;
        }

        String edadTexto = etEdad.getText().toString().trim();
        int edad;
        try {
            edad = Integer.parseInt(edadTexto);
        } catch (NumberFormatException exception) {
            etEdad.setError(getString(R.string.error_valor_no_valido));
            etEdad.requestFocus();
            return;
        }
        if (edad <= 0) {
            etEdad.setError(getString(R.string.error_valor_no_valido));
            etEdad.requestFocus();
            return;
        }

        String pesoTexto = etPeso.getText().toString().trim();
        double peso;
        try {
            peso = Double.parseDouble(pesoTexto);
        } catch (NumberFormatException exception) {
            etPeso.setError(getString(R.string.error_valor_no_valido));
            etPeso.requestFocus();
            return;
        }
        if (peso <= 0) {
            etPeso.setError(getString(R.string.error_valor_no_valido));
            etPeso.requestFocus();
            return;
        }

        String sexoSeleccionado = spinnerSexo.getSelectedItem() != null
                ? spinnerSexo.getSelectedItem().toString()
                : "";
        if (TextUtils.isEmpty(sexoSeleccionado)
                || sexoSeleccionado.equals(getString(R.string.placeholder_select_sexo))) {
            Toast.makeText(this, R.string.error_sexo_obligatorio, Toast.LENGTH_SHORT).show();
            return;
        }

        Paciente paciente = new Paciente(
                UUID.randomUUID().toString(),
                nombre,
                especie,
                raza,
                edad,
                peso,
                sexoSeleccionado,
                clienteId,
                clienteNombre,
                null
        );

        pacienteViewModel.insert(paciente);
        Toast.makeText(
                this,
                getString(R.string.msg_paciente_guardado_cliente, nombre),
                Toast.LENGTH_LONG
        ).show();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
