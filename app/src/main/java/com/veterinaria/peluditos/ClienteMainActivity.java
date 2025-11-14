package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veterinaria.peluditos.data.Usuario;

public class ClienteMainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private AdminUsuarioViewModel usuarioViewModel;
    private ShapeableImageView imgAvatarCliente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_cliente);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();
        usuarioViewModel = new ViewModelProvider(this).get(AdminUsuarioViewModel.class);
        imgAvatarCliente = findViewById(R.id.imgAvatarCliente);

        Toast.makeText(this, R.string.toast_cliente_bienvenida, Toast.LENGTH_LONG).show();
        configurarSaludo();
        configurarAccionesPrincipales();
        configurarMenuInferior();
    }

    private void configurarSaludo() {
        TextView txtMensaje = findViewById(R.id.txtMensajeBenvenida);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            txtMensaje.setText(R.string.text_saludo_generico);
            actualizarAvatar(null);
            return;
        }

        String nombreFallback = obtenerNombreDesdeFirebase(currentUser);
        txtMensaje.setText(getString(R.string.text_saludo_cliente, nombreFallback));
        actualizarAvatar(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);

        String email = currentUser.getEmail();
        if (!TextUtils.isEmpty(email)) {
            usuarioViewModel.getUsuarioByEmail(email).observe(this, usuario -> {
                if (usuario != null) {
                    txtMensaje.setText(getString(R.string.text_saludo_cliente,
                            construirNombreCompleto(usuario)));
                    actualizarAvatar(usuario.getFotoUrl());
                }
            });
        } else {
            usuarioViewModel.getUsuario(currentUser.getUid()).observe(this, usuario -> {
                if (usuario != null) {
                    txtMensaje.setText(getString(R.string.text_saludo_cliente,
                            construirNombreCompleto(usuario)));
                    actualizarAvatar(usuario.getFotoUrl());
                }
            });
        }
    }

    private void configurarAccionesPrincipales() {
        Button btnAgregarMascota = findViewById(R.id.btnAgregarMascota);
        Button btnSolicitarCita = findViewById(R.id.btnSolicitarCita);

        btnAgregarMascota.setOnClickListener(v -> abrirNuevoPaciente());
        btnSolicitarCita.setOnClickListener(v -> abrirNuevaCita());
    }

    private void configurarMenuInferior() {
        BottomNavigationView menu = findViewById(R.id.btnMenuCliente);
        menu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_mascotas) {
                abrirNuevoPaciente();
                return true;
            } else if (itemId == R.id.nav_citas) {
                abrirNuevaCita();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                Toast.makeText(this, R.string.toast_funcion_no_disponible, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        menu.setSelectedItemId(R.id.nav_home);
    }

    private void abrirNuevoPaciente() {
        Intent intent = new Intent(this, NuevoPacienteClienteActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void abrirNuevaCita() {
        Intent intent = new Intent(this, NuevaCitaClienteActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void cerrarSesion(View view) {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_superior, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.mapa_google) {
            Intent intent = new Intent(this, maps_google.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_logout) {
            mAuth.signOut();
            sessionManager.logoutUser();

            Intent intent = new Intent(ClienteMainActivity.this, Login_Peluditos.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (itemId == R.id.perfil) {
            Toast.makeText(this, R.string.toast_funcion_no_disponible, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void actualizarAvatar(String fotoUrl) {
        if (imgAvatarCliente == null) {
            return;
        }

        Object source = TextUtils.isEmpty(fotoUrl) ? R.drawable.icono_perfil : fotoUrl;
        Glide.with(this)
                .load(source)
                .placeholder(R.drawable.icono_perfil)
                .error(R.drawable.icono_perfil)
                .centerCrop()
                .into(imgAvatarCliente);
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
        }
        return getString(R.string.text_cliente_sin_nombre);
    }

    private String obtenerNombreDesdeFirebase(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = user.getEmail();
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.text_cliente_sin_nombre);
        }
        return displayName;
    }
}
