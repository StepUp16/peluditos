package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ClienteMainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_cliente); // Layout específico para clientes

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sessionManager = new SessionManager(this);
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_superior, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.mapa_google) {
            // Navegación al mapa (sin cerrar esta pantalla para una mejor UX)
            Intent intent = new Intent(this, maps_google.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_logout) {
            // 3. Lógica de cierre de sesión completa
            mAuth.signOut();
            sessionManager.logoutUser();

            Intent intent = new Intent(ClienteMainActivity.this, Login_Peluditos.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }else if (itemId == R.id.perfil) {
            // Navegación al perfil (sin cerrar esta pantalla para una mejor UX)
            Intent intent = new Intent(this, AdminPerfil.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
