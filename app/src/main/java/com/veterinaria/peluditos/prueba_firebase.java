package com.veterinaria.peluditos;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class prueba_firebase extends AppCompatActivity {

    private static final String TAG = "PruebaFirebase";
    private static final String COLLECTION_NAME = "usuarios_prueba";
    private static final String KEY_NOMBRE = "nombre";

    private EditText etNombrePrueba;
    private Button btnGuardarFirebase;
    private Button btnMostrarFirebase;
    private TextView tvDatoRecuperado;

    // Instancia de Cloud Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prueba_firebase);

        // Inicializar vistas
        etNombrePrueba = findViewById(R.id.etNombrePrueba);
        btnGuardarFirebase = findViewById(R.id.btnGuardarFirebase);
        btnMostrarFirebase = findViewById(R.id.btnMostrarFirebase);
        tvDatoRecuperado = findViewById(R.id.tvDatoRecuperado);

        // Inicializar Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Configurar listener para el botón de guardar
        btnGuardarFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarNombreEnFirebase();
            }
        });

        // Configurar listener para el botón de mostrar
        btnMostrarFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarNombreDesdeFirebase();
            }
        });
    }

    private void guardarNombreEnFirebase() {
        String nombre = etNombrePrueba.getText().toString().trim();

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Intentando guardar nombre: " + nombre);
        Log.d(TAG, "Colección destino: " + COLLECTION_NAME);

        // Crear un nuevo usuario con un nombre
        Map<String, Object> usuario = new HashMap<>();
        usuario.put(KEY_NOMBRE, nombre);
        usuario.put("timestamp", System.currentTimeMillis()); // Agregamos timestamp para mejor control

        // Agregar un nuevo documento con un ID generado a la colección
        db.collection(COLLECTION_NAME)
                .add(usuario)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "✅ ÉXITO: Documento agregado con ID: " + documentReference.getId());
                        Log.d(TAG, "✅ Datos guardados en Firestore correctamente");
                        Log.d(TAG, "✅ Ruta completa: " + COLLECTION_NAME + "/" + documentReference.getId());
                        Toast.makeText(prueba_firebase.this, "✅ Nombre guardado correctamente en Firebase", Toast.LENGTH_LONG).show();
                        etNombrePrueba.setText(""); // Limpiar el campo de texto
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ ERROR al agregar el documento", e);
                        Log.e(TAG, "❌ Tipo de error: " + e.getClass().getSimpleName());
                        Log.e(TAG, "❌ Mensaje de error: " + e.getMessage());
                        Toast.makeText(prueba_firebase.this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarNombreDesdeFirebase() {
        Log.d(TAG, "Consultando colección: " + COLLECTION_NAME);

        db.collection(COLLECTION_NAME)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Mostrar el más reciente primero
                .limit(1) // Para este ejemplo, solo traemos el primer documento que encuentre
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Consulta exitosa. Documentos encontrados: " + task.getResult().size());

                            if (task.getResult().isEmpty()) {
                                Log.d(TAG, "⚠️ No se encontraron documentos en la colección: " + COLLECTION_NAME);
                                tvDatoRecuperado.setText("No hay datos guardados");
                                Toast.makeText(prueba_firebase.this, "No se encontraron datos en Firebase", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Recorremos el resultado (aunque solo esperamos uno)
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "📄 Documento ID: " + document.getId());
                                Log.d(TAG, "📄 Datos completos: " + document.getData());

                                if (document.contains(KEY_NOMBRE)) {
                                    String nombreRecuperado = document.getString(KEY_NOMBRE);
                                    Log.d(TAG, "✅ Nombre recuperado: " + nombreRecuperado);
                                    tvDatoRecuperado.setText(nombreRecuperado);
                                    Toast.makeText(prueba_firebase.this, "✅ Dato mostrado: " + nombreRecuperado, Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.w(TAG, "⚠️ El documento no contiene el campo '" + KEY_NOMBRE + "'");
                                    tvDatoRecuperado.setText("Error: campo no encontrado");
                                }
                            }
                        } else {
                            Log.e(TAG, "❌ Error obteniendo documentos", task.getException());
                            if (task.getException() != null) {
                                Log.e(TAG, "❌ Mensaje de error: " + task.getException().getMessage());
                            }
                            tvDatoRecuperado.setText("Error al consultar");
                            Toast.makeText(prueba_firebase.this, "❌ Error al obtener los datos: " +
                                (task.getException() != null ? task.getException().getMessage() : "Error desconocido"),
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
