package com.veterinaria.peluditos;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veterinaria.peluditos.data.Usuario;

public class AdminUsuarioEditar extends AppCompatActivity {
    private static final String TAG = "AdminUsuarioEditar";

    private EditText etNombre, etApellido, etCorreo, etTelefono, etDui, etDireccion;
    private Spinner spinnerRol;
    private Button btnGuardar;
    private ImageButton btnBack;
    private AdminUsuarioViewModel viewModel;
    private FirebaseFirestore firestore;
    private String usuarioId;
    private boolean esPerfilPropio;
    private Usuario usuarioOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_usuario_editar);

        // Inicializar ViewModel y Firestore
        viewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        firestore = FirebaseFirestore.getInstance();

        // Inicializar vistas
        initViews();
        setupSpinner();

        // Obtener parámetros del Intent
        usuarioId = getIntent().getStringExtra("USUARIO_ID");
        esPerfilPropio = getIntent().getBooleanExtra("ES_PERFIL_PROPIO", false);

        if (usuarioId != null) {
            cargarDatosUsuario();
        } else {
            Toast.makeText(this, "Error: No se especificó el usuario a editar", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etCorreo = findViewById(R.id.etCorreo);
        etTelefono = findViewById(R.id.etTelefono);
        etDui = findViewById(R.id.etDui);
        etDireccion = findViewById(R.id.etDireccion);
        spinnerRol = findViewById(R.id.spinnerRol);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnBack = findViewById(R.id.btnBack);

        // Si es perfil propio, deshabilitar edición del rol y DUI
        if (esPerfilPropio) {
            spinnerRol.setEnabled(false);
            etDui.setEnabled(false);
            etDui.setAlpha(0.6f); // Indicar visualmente que está deshabilitado
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosUsuario() {
        Log.d(TAG, "Cargando datos del usuario ID: " + usuarioId);

        if (isNetworkAvailable()) {
            // Intentar cargar desde Firebase primero
            cargarDesdeFirebase();
        } else {
            // Sin conexión, cargar desde local
            cargarDesdeLocal();
        }
    }

    private void cargarDesdeFirebase() {
        Log.d(TAG, "Cargando datos desde Firebase");

        firestore.collection("usuarios").document(usuarioId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Usuario usuario = createUsuarioFromDocument(documentSnapshot);
                        if (usuario != null) {
                            usuarioOriginal = usuario;
                            mostrarDatosEnUI(usuario);
                            Log.d(TAG, "Datos cargados desde Firebase exitosamente");
                        } else {
                            Log.e(TAG, "Error al crear objeto Usuario desde Firebase");
                            cargarDesdeLocal();
                        }
                    } else {
                        Log.w(TAG, "Usuario no encontrado en Firebase, intentando local");
                        cargarDesdeLocal();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar desde Firebase: " + e.getMessage());
                    cargarDesdeLocal();
                });
    }

    private void cargarDesdeLocal() {
        Log.d(TAG, "Cargando datos desde base local");

        viewModel.getUsuario(usuarioId).observe(this, usuario -> {
            if (usuario != null) {
                usuarioOriginal = usuario;
                mostrarDatosEnUI(usuario);
                Log.d(TAG, "Datos cargados desde base local exitosamente");
            } else {
                Log.e(TAG, "Usuario no encontrado en base local");
                Toast.makeText(this, "Error: No se encontraron datos del usuario",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private Usuario createUsuarioFromDocument(DocumentSnapshot document) {
        try {
            return new Usuario(
                    document.getId(),
                    document.getString("nombre"),
                    document.getString("apellido"),
                    document.getString("email"),
                    document.getString("telefono"),
                    document.getString("dui"),
                    document.getString("direccion"),
                    document.getString("rol")
            );
        } catch (Exception e) {
            Log.e(TAG, "Error al crear Usuario desde documento: " + e.getMessage());
            return null;
        }
    }

    private void mostrarDatosEnUI(Usuario usuario) {
        runOnUiThread(() -> {
            etNombre.setText(usuario.getNombre());
            etApellido.setText(usuario.getApellido());
            etCorreo.setText(usuario.getEmail());
            etTelefono.setText(usuario.getTelefono());
            etDui.setText(usuario.getDui());
            etDireccion.setText(usuario.getDireccion());

            // Establecer el rol en el spinner
            ArrayAdapter adapter = (ArrayAdapter) spinnerRol.getAdapter();
            if (usuario.getRol() != null) {
                int position = adapter.getPosition(usuario.getRol());
                if (position >= 0) {
                    spinnerRol.setSelection(position);
                }
            }

            Log.d(TAG, "Datos mostrados en UI para usuario: " + usuario.getNombre());
        });
    }

    private void guardarCambios() {
        if (usuarioOriginal == null) {
            Toast.makeText(this, "Error: No se han cargado los datos originales", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String dui = etDui.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String rol = spinnerRol.getSelectedItem().toString();

        // Validaciones
        if (!validarCampos(nombre, apellido, email, telefono, dui, direccion)) {
            return;
        }

        // Crear usuario actualizado
        Usuario usuarioActualizado = new Usuario(
                usuarioId, nombre, apellido, email,
                telefono, dui, direccion, rol
        );

        // Deshabilitar botón mientras se guarda
        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        if (isNetworkAvailable()) {
            // Guardar en Firebase primero, luego en local
            guardarEnFirebase(usuarioActualizado);
        } else {
            // Sin conexión, guardar solo en local
            guardarEnLocal(usuarioActualizado, true);
        }
    }

    private boolean validarCampos(String nombre, String apellido, String email,
                                  String telefono, String dui, String direccion) {
        // Validar campos vacíos
        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() ||
                telefono.isEmpty() || dui.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validaciones adicionales
        if (nombre.length() < 2) {
            etNombre.setError("El nombre debe tener al menos 2 caracteres");
            etNombre.requestFocus();
            return false;
        }

        if (apellido.length() < 2) {
            etApellido.setError("El apellido debe tener al menos 2 caracteres");
            etApellido.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etCorreo.setError("Ingrese un correo electrónico válido");
            etCorreo.requestFocus();
            return false;
        }

        // Validación especial para cambio de email en perfil propio
        if (esPerfilPropio && usuarioOriginal != null &&
                !email.equals(usuarioOriginal.getEmail())) {
            Toast.makeText(this, "Advertencia: Cambiar el email puede afectar el inicio de sesión. " +
                            "Será necesario iniciar sesión con el nuevo email.",
                    Toast.LENGTH_LONG).show();
        }

        if (telefono.length() < 8) {
            etTelefono.setError("El teléfono debe tener al menos 8 dígitos");
            etTelefono.requestFocus();
            return false;
        }

        if (dui.length() < 9) {
            etDui.setError("El DUI debe tener al menos 9 caracteres");
            etDui.requestFocus();
            return false;
        }

        // Validación especial para DUI - no permitir cambio si es perfil propio
        if (esPerfilPropio && usuarioOriginal != null &&
                !dui.equals(usuarioOriginal.getDui())) {
            etDui.setError("No se puede cambiar el DUI del perfil propio");
            etDui.requestFocus();
            return false;
        }

        return true;
    }

    private void guardarEnFirebase(Usuario usuario) {
        Log.d(TAG, "Guardando en Firebase");

        firestore.collection("usuarios")
                .document(usuarioId)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario actualizado en Firebase exitosamente");
                    // Después de Firebase, actualizar en local
                    guardarEnLocal(usuario, false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar en Firebase: " + e.getMessage());
                    Toast.makeText(this, "Error al actualizar en la nube: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // Aún así, intentar guardar en local
                    guardarEnLocal(usuario, true);
                });
    }

    private void guardarEnLocal(Usuario usuario, boolean mostrarMensaje) {
        Log.d(TAG, "Guardando en base local");

        viewModel.insert(usuario);

        runOnUiThread(() -> {
            btnGuardar.setEnabled(true);
            btnGuardar.setText("Guardar Cambios");

            if (mostrarMensaje) {
                if (isNetworkAvailable()) {
                    Toast.makeText(this, "Usuario actualizado exitosamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Usuario guardado localmente. Se sincronizará cuando haya conexión",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Usuario actualizado exitosamente", Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "Usuario actualizado en base local exitosamente");
            setResult(RESULT_OK); // Notificar que se guardaron los cambios
            finish();
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
