package com.veterinaria.peluditos

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminNuevoPacienteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_paciente_nuevo)

        // Back button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Inputs
        val etNombre = findViewById<EditText>(R.id.etNombrePaciente)
        val etEspecie = findViewById<EditText>(R.id.etEspecie)
        val etRaza = findViewById<EditText>(R.id.etRaza)
        val etEdad = findViewById<EditText>(R.id.etEdad)
        val etPeso = findViewById<EditText>(R.id.etPeso)
        val spinnerSexo = findViewById<Spinner>(R.id.spinnerSexo)
        val spinnerCliente = findViewById<Spinner>(R.id.spinnerCliente)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPaciente)

        // Spinner datos ejemplo (reemplaza por datos reales o recurso)
        val sexoList = listOf("Macho", "Hembra", "Otro")
        val sexoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sexoList)
        spinnerSexo.adapter = sexoAdapter

        val clienteList = listOf("Seleccionar Cliente", "Cliente 1", "Cliente 2")
        val clienteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clienteList)
        spinnerCliente.adapter = clienteAdapter

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val especie = etEspecie.text.toString().trim()
            val raza = etRaza.text.toString().trim()
            val edad = etEdad.text.toString().trim()
            val peso = etPeso.text.toString().trim()
            val sexo = spinnerSexo.selectedItem?.toString() ?: ""
            val clienteSel = spinnerCliente.selectedItem?.toString() ?: ""

            // Validaciones básicas
            if (nombre.isEmpty()) {
                etNombre.error = "Ingrese el nombre del paciente"
                etNombre.requestFocus()
                return@setOnClickListener
            }
            if (especie.isEmpty()) {
                etEspecie.error = "Ingrese la especie"
                etEspecie.requestFocus()
                return@setOnClickListener
            }
            if (clienteSel == "Seleccionar Cliente" || clienteSel.isEmpty()) {
                Toast.makeText(this, "Seleccione un cliente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simular guardado
            Toast.makeText(this, "Paciente guardado (simulado)", Toast.LENGTH_SHORT).show()
            // Aquí iría la lógica real (ViewModel / repository / API)
            finish()
        }
    }
}

