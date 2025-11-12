package com.veterinaria.peluditos;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.veterinaria.peluditos.data.Paciente;
import com.veterinaria.peluditos.data.Usuario;
import com.veterinaria.peluditos.util.NetworkUtils;
import com.veterinaria.peluditos.util.PacientePhotoManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminPacienteNuevoActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "EXTRA_PACIENTE_ID";

    private EditText etNombre;
    private EditText etEdad;
    private EditText etPeso;
    private Spinner spinnerEspecie;
    private Spinner spinnerRaza;
    private Spinner spinnerSexo;
    private Spinner spinnerCliente;
    private Button btnGuardar;
    private ShapeableImageView ivPatientPhoto;
    private ImageButton btnChangePhoto;
    private ProgressBar photoProgress;

    private final List<Usuario> clientes = new ArrayList<>();
    private ArrayAdapter<String> clienteAdapter;
    private final Map<String, Integer> razasPorEspecie = new LinkedHashMap<>();

    private AdminUsuarioViewModel usuarioViewModel;
    private AdminPacienteViewModel pacienteViewModel;
    private String pacienteId;
    private boolean esEdicion = false;
    private String razaPendiente;
    private String clientePendienteId;
    private String fotoUrlSeleccionada;
    private PacientePhotoManager photoManager;
    private ActivityResultLauncher<String> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_paciente_nuevo);

        photoManager = new PacientePhotoManager();
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        subirFotoPaciente(uri);
                    }
                }
        );

        initViews();
        initRazaLookup();
        setupSexoSpinner();
        setupClienteSpinner();
        setupRazaSpinner();
        setupEspecieSpinner();

        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        pacienteViewModel = new ViewModelProvider(this).get(AdminPacienteViewModel.class);

        String pacienteIdExtra = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        if (!TextUtils.isEmpty(pacienteIdExtra)) {
            pacienteId = pacienteIdExtra;
            esEdicion = true;
        } else {
            pacienteId = UUID.randomUUID().toString();
            esEdicion = false;
        }

        observeClientes();
        if (esEdicion) {
            btnGuardar.setText(R.string.btn_guardar_cambios_paciente);
            cargarPacienteParaEdicion();
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnGuardar.setOnClickListener(v -> guardarPaciente());
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombrePaciente);
        etEdad = findViewById(R.id.etEdad);
        etPeso = findViewById(R.id.etPeso);
        spinnerEspecie = findViewById(R.id.spinnerEspecie);
        spinnerRaza = findViewById(R.id.spinnerRaza);
        spinnerSexo = findViewById(R.id.spinnerSexo);
        spinnerCliente = findViewById(R.id.spinnerCliente);
        btnGuardar = findViewById(R.id.btnGuardarPaciente);
        ivPatientPhoto = findViewById(R.id.ivPatientPhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        photoProgress = findViewById(R.id.photoProgress);

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> seleccionarNuevaFoto());
        }
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
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

        if (!TextUtils.isEmpty(especie) && razasPorEspecie.containsKey(especie)
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

        if (habilitar && !TextUtils.isEmpty(razaPendiente)) {
            seleccionarValorSpinner(spinnerRaza, razaPendiente);
            razaPendiente = null;
        }
    }

    private void setupSexoSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sexo_paciente_array,
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerSexo.setAdapter(adapter);
    }

    private void setupClienteSpinner() {
        List<String> placeholder = new ArrayList<>();
        placeholder.add(getString(R.string.placeholder_select_cliente));
        clienteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                placeholder
        );
        spinnerCliente.setAdapter(clienteAdapter);
    }

    private void observeClientes() {
        usuarioViewModel.getAllUsuarios().observe(this, usuarios -> {
            clientes.clear();
            List<String> nombres = new ArrayList<>();
            nombres.add(getString(R.string.placeholder_select_cliente));

            if (usuarios != null && !usuarios.isEmpty()) {
                for (Usuario usuario : usuarios) {
                    clientes.add(usuario);
                    nombres.add(usuario.getNombre() + " " + usuario.getApellido());
                }
            }

            clienteAdapter.clear();
            clienteAdapter.addAll(nombres);
            clienteAdapter.notifyDataSetChanged();
            aplicarSeleccionClientePendiente();
        });
    }

    private void cargarPacienteParaEdicion() {
        pacienteViewModel.getPaciente(pacienteId).observe(this, paciente -> {
            if (paciente != null) {
                llenarFormulario(paciente);
            }
        });
    }

    private void llenarFormulario(Paciente paciente) {
        etNombre.setText(paciente.getNombre());
        etEdad.setText(String.valueOf(paciente.getEdad()));
        etPeso.setText(String.valueOf(paciente.getPeso()));
        clientePendienteId = paciente.getClienteId();
        razaPendiente = paciente.getRaza();
        fotoUrlSeleccionada = paciente.getFotoUrl();
        actualizarPreviewFoto(fotoUrlSeleccionada);

        seleccionarValorSpinner(spinnerSexo, paciente.getSexo());
        seleccionarValorSpinner(spinnerEspecie, paciente.getEspecie());
    }

    private void aplicarSeleccionClientePendiente() {
        if (clientePendienteId == null || spinnerCliente.getAdapter() == null) {
            return;
        }
        for (int i = 1; i <= clientes.size(); i++) {
            Usuario usuario = clientes.get(i - 1);
            if (clientePendienteId.equals(usuario.getUid())) {
                spinnerCliente.setSelection(i);
                clientePendienteId = null;
                break;
            }
        }
    }

    private void seleccionarValorSpinner(Spinner spinner, String valor) {
        if (spinner == null || spinner.getAdapter() == null || TextUtils.isEmpty(valor)) {
            return;
        }
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            Object item = spinner.getAdapter().getItem(i);
            String itemValue = item != null ? item.toString() : "";
            if (valor.equals(itemValue)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void guardarPaciente() {
        String nombre = etNombre.getText().toString().trim();
        String especie = spinnerEspecie.getSelectedItem() != null
                ? spinnerEspecie.getSelectedItem().toString()
                : "";
        String raza = spinnerRaza.getSelectedItem() != null
                ? spinnerRaza.getSelectedItem().toString()
                : "";
        String edadTexto = etEdad.getText().toString().trim();
        String pesoTexto = etPeso.getText().toString().trim();
        String sexo = spinnerSexo.getSelectedItem() != null
                ? spinnerSexo.getSelectedItem().toString()
                : "";

        Usuario clienteSeleccionado = obtenerClienteSeleccionado();

        if (!validarCampos(nombre, especie, raza, edadTexto, pesoTexto, sexo, clienteSeleccionado)) {
            return;
        }

        int edad = Integer.parseInt(edadTexto);
        double peso = Double.parseDouble(pesoTexto);

        btnGuardar.setEnabled(false);
        btnGuardar.setText(R.string.btn_guardando);

        String pacienteIdFinal = TextUtils.isEmpty(pacienteId)
                ? UUID.randomUUID().toString()
                : pacienteId;
        pacienteId = pacienteIdFinal;
        Paciente paciente = new Paciente(
                pacienteIdFinal,
                nombre,
                especie,
                raza,
                edad,
                peso,
                sexo,
                clienteSeleccionado != null ? clienteSeleccionado.getUid() : "",
                clienteSeleccionado != null
                        ? clienteSeleccionado.getNombre() + " " + clienteSeleccionado.getApellido()
                        : "",
                fotoUrlSeleccionada
        );

        if (esEdicion) {
            pacienteViewModel.update(paciente);
            Toast.makeText(this, R.string.msg_paciente_actualizado, Toast.LENGTH_SHORT).show();
        } else {
            pacienteViewModel.insert(paciente);
            Toast.makeText(this, R.string.msg_paciente_guardado, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private Usuario obtenerClienteSeleccionado() {
        int position = spinnerCliente.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= clientes.size()) {
            return null;
        }
        return clientes.get(position - 1);
    }

    private boolean validarCampos(@NonNull String nombre,
                                  @NonNull String especie,
                                  @NonNull String raza,
                                  @NonNull String edad,
                                  @NonNull String peso,
                                  @NonNull String sexo,
                                  Usuario cliente) {
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError(getString(R.string.error_nombre_paciente));
            etNombre.requestFocus();
            return false;
        }

        if (nombre.length() < 2) {
            etNombre.setError(getString(R.string.error_nombre_largo));
            etNombre.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(especie)) {
            Toast.makeText(this, R.string.error_especie, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (especie.equals(getString(R.string.placeholder_select_especie))) {
            Toast.makeText(this, R.string.error_especie, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(raza) || raza.equals(getString(R.string.placeholder_select_raza))) {
            Toast.makeText(this, R.string.error_raza, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(edad)) {
            etEdad.setError(getString(R.string.error_edad));
            etEdad.requestFocus();
            return false;
        }

        try {
            int edadVal = Integer.parseInt(edad);
            if (edadVal <= 0) {
                etEdad.setError(getString(R.string.error_edad_valor));
                etEdad.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etEdad.setError(getString(R.string.error_edad_valor));
            etEdad.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(peso)) {
            etPeso.setError(getString(R.string.error_peso));
            etPeso.requestFocus();
            return false;
        }

        try {
            double pesoVal = Double.parseDouble(peso);
            if (pesoVal <= 0) {
                etPeso.setError(getString(R.string.error_peso_valor));
                etPeso.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etPeso.setError(getString(R.string.error_peso_valor));
            etPeso.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(sexo)) {
            Toast.makeText(this, R.string.error_sexo, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cliente == null) {
            Toast.makeText(this, R.string.msg_seleccione_cliente, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void seleccionarNuevaFoto() {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.error_red_requerida, Toast.LENGTH_SHORT).show();
            return;
        }
        if (photoPickerLauncher != null) {
            photoPickerLauncher.launch("image/*");
        }
    }

    private void subirFotoPaciente(Uri uri) {
        if (photoManager == null || TextUtils.isEmpty(pacienteId)) {
            return;
        }
        mostrarCargaFoto(true);
        Toast.makeText(this, R.string.msg_foto_subiendo, Toast.LENGTH_SHORT).show();
        photoManager.uploadPhoto(getApplicationContext(), uri, pacienteId,
                new PacientePhotoManager.UploadCallback() {
                    @Override
                    public void onSuccess(@NonNull String downloadUrl) {
                        fotoUrlSeleccionada = downloadUrl;
                        actualizarPreviewFoto(downloadUrl);
                        mostrarCargaFoto(false);
                        Toast.makeText(AdminPacienteNuevoActivity.this,
                                R.string.msg_foto_actualizada,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        mostrarCargaFoto(false);
                        Toast.makeText(AdminPacienteNuevoActivity.this,
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

    private void actualizarPreviewFoto(String url) {
        if (ivPatientPhoto == null) {
            return;
        }
        if (TextUtils.isEmpty(url)) {
            ivPatientPhoto.setImageResource(R.drawable.paciente);
            return;
        }
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.paciente)
                .error(R.drawable.paciente)
                .into(ivPatientPhoto);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoManager != null) {
            photoManager.dispose();
        }
    }
}
