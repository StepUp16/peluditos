package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ClienteMainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_cliente); // Layout específico para clientes

        mAuth = FirebaseAuth.getInstance();

        // Mostrar mensaje de bienvenida para cliente
        Toast.makeText(this, "¡Bienvenido Cliente!", Toast.LENGTH_LONG).show();

        // Aquí puedes agregar funcionalidades específicas para clientes
        // Por ejemplo: agendar citas, ver historial de mascotas, etc.
    }

    // Método para cerrar sesión (opcional)
    public void cerrarSesion(View view) {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
