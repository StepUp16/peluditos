package com.veterinaria.peluditos;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsGoogleActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_google);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Ubicación de la Clínica");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Para el botón de atrás

        // Obtiene el SupportMapFragment y notifica cuando el mapa está listo para ser usado.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Se dispara cuando el mapa está listo. Aquí es donde puedes añadir marcadores, líneas,
     * listeners, y mover la cámara.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Añade un marcador de ejemplo en una ubicación y mueve la cámara
        // Puedes cambiar estas coordenadas por las de tu clínica
        LatLng clinicaUbicacion = new LatLng(13.6929, -89.2182); // Coordenadas de ejemplo (El Salvador)
        mMap.addMarker(new MarkerOptions().position(clinicaUbicacion).title("Clínica Peluditos"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clinicaUbicacion, 15)); // El 15 es el nivel de zoom
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Maneja el clic en el botón de atrás de la toolbar
        return true;
    }
}
