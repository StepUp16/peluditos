package com.veterinaria.peluditos;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CitaEstadoDialogFragment extends DialogFragment {

    private static final String ARG_CITA_ID = "cita_id";
    private static final String ARG_ESTADO = "estado";
    private static final String ARG_TIMESTAMP = "timestamp";
    private static final String ARG_NOTA = "nota";

    public interface OnEstadoConfirmadoListener {
        void onEstadoConfirmado(String citaId, String nuevoEstado, long nuevaFechaMillis, String nota);
    }

    private OnEstadoConfirmadoListener listener;
    private Spinner spinnerEstado;
    private View layoutReprogramar;
    private TextView tvFechaSeleccionada;
    private Button btnSeleccionarFecha;
    private TextInputLayout tilNota;
    private TextInputEditText etNota;

    private long fechaSeleccionadaMillis;
    private boolean fechaSeleccionada;
    private int colorPorDefectoFecha;

    private final Calendar calendarTemp = Calendar.getInstance();
    private final SimpleDateFormat formatoFecha =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public static CitaEstadoDialogFragment newInstance(String citaId,
                                                       String estado,
                                                       long timestamp,
                                                       String notaEstado) {
        CitaEstadoDialogFragment fragment = new CitaEstadoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CITA_ID, citaId);
        args.putString(ARG_ESTADO, estado);
        args.putLong(ARG_TIMESTAMP, timestamp);
        args.putString(ARG_NOTA, notaEstado);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnEstadoConfirmadoListener(OnEstadoConfirmadoListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_cita_estado, null, false);
        spinnerEstado = view.findViewById(R.id.spinnerDialogEstado);
        layoutReprogramar = view.findViewById(R.id.layoutReprogramar);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        tilNota = view.findViewById(R.id.tilNotaEstado);
        etNota = view.findViewById(R.id.etNotaEstado);
        colorPorDefectoFecha = requireContext().getColor(R.color.textColorSecondary);

        spinnerEstado.setAdapter(android.widget.ArrayAdapter.createFromResource(
                requireContext(),
                R.array.cita_estados_array,
                android.R.layout.simple_spinner_dropdown_item
        ));

        String estadoActual = getArguments() != null ? getArguments().getString(ARG_ESTADO) : null;
        String nota = getArguments() != null ? getArguments().getString(ARG_NOTA) : "";
        fechaSeleccionadaMillis = getArguments() != null ? getArguments().getLong(ARG_TIMESTAMP, 0L) : 0L;

        if (!TextUtils.isEmpty(nota)) {
            etNota.setText(nota);
        }

        if (fechaSeleccionadaMillis > 0) {
            fechaSeleccionada = true;
            tvFechaSeleccionada.setText(formatoFecha.format(fechaSeleccionadaMillis));
        }

        if (!TextUtils.isEmpty(estadoActual)) {
            String[] estados = getResources().getStringArray(R.array.cita_estados_array);
            for (int i = 0; i < estados.length; i++) {
                if (estadoActual.equalsIgnoreCase(estados[i])) {
                    spinnerEstado.setSelection(i);
                    break;
                }
            }
        }

        spinnerEstado.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String estado = spinnerEstado.getSelectedItem().toString();
                boolean esPospuesta = estado.equalsIgnoreCase(getString(R.string.cita_estado_pospuesta));
                layoutReprogramar.setVisibility(esPospuesta ? View.VISIBLE : View.GONE);
                if (!esPospuesta) {
                    tvFechaSeleccionada.setTextColor(colorPorDefectoFecha);
                }
                tilNota.setError(null);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        btnSeleccionarFecha.setOnClickListener(v -> mostrarPickers());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(R.string.dialog_cambiar_estado_title)
                .setNegativeButton(R.string.action_cancelar, null)
                .setPositiveButton(R.string.action_guardar, (dialog, which) -> {
                    if (listener != null && validarDatos()) {
                        listener.onEstadoConfirmado(
                                getArguments() != null ? getArguments().getString(ARG_CITA_ID) : null,
                                spinnerEstado.getSelectedItem().toString(),
                                layoutReprogramar.getVisibility() == View.VISIBLE ? fechaSeleccionadaMillis : -1,
                                etNota.getText() != null ? etNota.getText().toString().trim() : ""
                        );
                    }
                })
                .create();
    }

    private void mostrarPickers() {
        final Calendar actual = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendarTemp.set(year, month, dayOfMonth);
                    TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                calendarTemp.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendarTemp.set(Calendar.MINUTE, minute);
                                calendarTemp.set(Calendar.SECOND, 0);
                                calendarTemp.set(Calendar.MILLISECOND, 0);
                                fechaSeleccionadaMillis = calendarTemp.getTimeInMillis();
                                fechaSeleccionada = true;
                                tvFechaSeleccionada.setText(formatoFecha.format(calendarTemp.getTimeInMillis()));
                                tvFechaSeleccionada.setTextColor(colorPorDefectoFecha);
                            },
                            actual.get(Calendar.HOUR_OF_DAY),
                            actual.get(Calendar.MINUTE),
                            true);
                    timePicker.show();
                },
                actual.get(Calendar.YEAR),
                actual.get(Calendar.MONTH),
                actual.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.show();
    }

    private boolean validarDatos() {
        tilNota.setError(null);
        String estado = spinnerEstado.getSelectedItem().toString();
        if (estado.equalsIgnoreCase(getString(R.string.cita_estado_pospuesta))) {
            if (!fechaSeleccionada) {
                tvFechaSeleccionada.setTextColor(requireContext().getColor(R.color.estadoCanceladaColor));
                tvFechaSeleccionada.setText(getString(R.string.error_fecha_posponer));
                return false;
            }
        } else {
            tvFechaSeleccionada.setTextColor(colorPorDefectoFecha);
        }

        if (estado.equalsIgnoreCase(getString(R.string.cita_estado_cancelada))) {
            String nota = etNota.getText() != null ? etNota.getText().toString().trim() : "";
            if (nota.isEmpty()) {
                tilNota.setError(getString(R.string.error_nota_cancelacion));
                return false;
            }
        }

        return true;
    }
}
