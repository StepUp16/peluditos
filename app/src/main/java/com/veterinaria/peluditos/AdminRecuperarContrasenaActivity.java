package com.veterinaria.peluditos;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class AdminRecuperarContrasenaActivity extends AppCompatActivity {

    private EditText etCorreo;
    private Button btnEnviar;
    private CircularProgressIndicator progressIndicator;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_recuperar_contrasena);

        firebaseAuth = FirebaseAuth.getInstance();

        ImageButton btnBack = findViewById(R.id.btnBack);
        etCorreo = findViewById(R.id.etCorreo);
        btnEnviar = findViewById(R.id.btnEnviarRecuperacion);
        progressIndicator = findViewById(R.id.progressRecuperacion);

        btnBack.setOnClickListener(v -> finish());
        btnEnviar.setOnClickListener(v -> enviarCorreoRecuperacion());
    }

    private void enviarCorreoRecuperacion() {
        String correo = etCorreo.getText().toString().trim();
        if (TextUtils.isEmpty(correo)) {
            etCorreo.setError(getString(R.string.error_recuperar_correo_invalid));
            etCorreo.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError(getString(R.string.error_recuperar_correo_invalid));
            etCorreo.requestFocus();
            return;
        }

        mostrarCarga(true);
        firebaseAuth.sendPasswordResetEmail(correo)
                .addOnCompleteListener(task -> {
                    mostrarCarga(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                R.string.msg_reset_email_enviado,
                                Toast.LENGTH_SHORT).show();
                        AdminConfirmacionEnvioContrasenaActivity.open(this, correo);
                        finish();
                    } else {
                        manejarError(task.getException());
                    }
                });
    }

    private void manejarError(Exception exception) {
        String mensaje = getString(R.string.error_operacion);
        if (exception instanceof FirebaseAuthInvalidUserException) {
            mensaje = getString(R.string.error_usuario_no_encontrado);
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void mostrarCarga(boolean mostrando) {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(mostrando ? View.VISIBLE : View.GONE);
        }
        if (btnEnviar != null) {
            btnEnviar.setEnabled(!mostrando);
        }
    }
}
