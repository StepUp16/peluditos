package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AdminMainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_home); // Layout específico para administradores

        mAuth = FirebaseAuth.getInstance();

        // Mostrar mensaje de bienvenida para admin
        Toast.makeText(this, "¡Bienvenido Administrador!", Toast.LENGTH_LONG).show();

        // Aquí puedes agregar botones específicos para administradores
        // Por ejemplo: crear usuarios, ver todas las citas, etc.
    }

    // Método para cerrar sesión (opcional)
    public void cerrarSesion(View view) {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
