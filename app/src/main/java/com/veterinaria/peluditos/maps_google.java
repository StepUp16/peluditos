package com.veterinaria.peluditos;

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
public class maps_google extends AppCompatActivity implements OnMapReadyCallback{
    private GoogleMap mMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.maps_google);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        if (mMap == null) {
            return false;
        } else if (item.getItemId() == R.id.mapa_normal) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            return true;
        } else if (item.getItemId() == R.id.mapa_satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        } else if (item.getItemId() == R.id.mapa_terrain) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            return true;
        } else if (item.getItemId() == R.id.mapa_hybrid) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            return true;
        }else {
            return false;
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Coordenadas de UES, San Salvador
        LatLng ues = new LatLng(13.6988, -89.1913);
        // Agregar marcador en esa ubicación
        mMap.addMarker(new MarkerOptions()
                .position(ues)
                .title("Clínica Peluditos - San Salvador"));
        // Mover la cámara al marcador con zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ues, 18));
        // -----------------------------
        // HABILITAR CONTROLES DE MAPA
        // -----------------------------
        mMap.getUiSettings().setZoomControlsEnabled(true); // Botones de zoom (+/-)
        mMap.getUiSettings().setCompassEnabled(true); // Brújula (aparece al rotar el mapa)
        mMap.getUiSettings().setMapToolbarEnabled(true); // Barra de herramientas (cuando se toca un marcador)
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Botón "mi ubicación"
        mMap.getUiSettings().setAllGesturesEnabled(true); // Permitir gestos: zoom, mover, rotar, inclinar
        // Cambiar el tipo de mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // NORMAL, SATELLITE, HYBRID, TERRAIN
    }
}
