package com.veterinaria.peluditos;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class AdminConfirmacionEnvioContrasenaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    public static void open(Context context, String email) {
        Intent intent = new Intent(context, AdminConfirmacionEnvioContrasenaActivity.class);
        intent.putExtra(EXTRA_EMAIL, email);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_confirmacion_envio_contrasena);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvCorreoDestino = findViewById(R.id.tvCorreoDestino);
        MaterialButton btnAbrirCorreo = findViewById(R.id.btnAbrirCorreo);
        MaterialButton btnVolverLogin = findViewById(R.id.btnVolverLogin);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email != null) {
            tvCorreoDestino.setText(getString(R.string.text_envio_confirmacion_correo, email));
        }

        btnBack.setOnClickListener(v -> finish());
        btnVolverLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, Login_Peluditos.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        btnAbrirCorreo.setOnClickListener(v -> abrirAppCorreo());
    }

    private void abrirAppCorreo() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.chooser_correo)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_email_app, Toast.LENGTH_SHORT).show();
        }
    }
}
