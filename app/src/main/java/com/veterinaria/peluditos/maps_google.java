package com.veterinaria.peluditos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

public class maps_google extends AppCompatActivity implements OnMapReadyCallback{
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager; // <--- 1. Instancia de SessionManager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.maps_google);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicializar FirebaseAuth y SessionManager
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this); // <--- 2. Inicializar SessionManager

        //Inicializar el fragment del mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.main);
        mapFragment.getMapAsync(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_superior, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_logout) {
            // --- 3. LÓGICA DE CIERRE DE SESIÓN COMPLETA Y CORREGIDA ---
            // a. Cerrar sesión en Firebase
            mAuth.signOut();
            // b. Limpiar la sesión local guardada
            sessionManager.logoutUser();

            // c. Redirigir a la pantalla de Login
            Intent intent = new Intent(maps_google.this, Login_Peluditos.class);
            // Limpiar la pila de actividades para que el usuario no pueda volver atrás
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); 
            return true;

        } else if (mMap != null) {
            // Lógica para cambiar el tipo de mapa
            if (itemId == R.id.perfil) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Coordenadas de UES, San Salvador (Tus cambios han sido respetados)
        LatLng ues = new LatLng(13.6988, -89.1913);
        
        mMap.addMarker(new MarkerOptions()
                .position(ues)
                .title("Universidad de El Salvador - San Salvador"));
        
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ues, 18));
        
        mMap.getUiSettings().setZoomControlsEnabled(true); 
        mMap.getUiSettings().setCompassEnabled(true); 
        mMap.getUiSettings().setMapToolbarEnabled(true); 
        mMap.getUiSettings().setMyLocationButtonEnabled(true); 
        mMap.getUiSettings().setAllGesturesEnabled(true); 
        
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); 
    }
}
