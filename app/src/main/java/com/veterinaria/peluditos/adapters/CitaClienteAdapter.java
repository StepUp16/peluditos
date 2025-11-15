package com.veterinaria.peluditos.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.Cita;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CitaClienteAdapter extends RecyclerView.Adapter<CitaClienteAdapter.CitaClienteViewHolder> {

    private final List<Cita> citas = new ArrayList<>();
    private final SimpleDateFormat fechaFormato = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat horaFormato = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public void setCitas(List<Cita> nuevasCitas) {
        citas.clear();
        if (nuevasCitas != null) {
            citas.addAll(nuevasCitas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CitaClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita_cliente, parent, false);
        return new CitaClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaClienteViewHolder holder, int position) {
        holder.bind(citas.get(position));
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    class CitaClienteViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvPacienteNombre;
        private final TextView tvFechaHora;
        private final TextView tvMotivo;
        private final TextView tvEstado;

        CitaClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPacienteNombre = itemView.findViewById(R.id.tvPacienteNombre);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvMotivo = itemView.findViewById(R.id.tvMotivo);
            tvEstado = itemView.findViewById(R.id.tvEstadoCita);
        }

        void bind(Cita cita) {
            Context context = itemView.getContext();
            String nombreMascota = !TextUtils.isEmpty(cita.getPacienteNombre())
                    ? cita.getPacienteNombre()
                    : context.getString(R.string.text_cliente_mascota_sin_nombre);
            tvPacienteNombre.setText(nombreMascota);

            String fechaTexto = cita.getFecha();
            String horaTexto = cita.getHora();
            if (TextUtils.isEmpty(fechaTexto) || TextUtils.isEmpty(horaTexto)) {
                long timestamp = cita.getFechaHoraTimestamp();
                if (timestamp > 0) {
                    Date date = new Date(timestamp);
                    fechaTexto = fechaFormato.format(date);
                    horaTexto = horaFormato.format(date);
                }
            }
            if (TextUtils.isEmpty(fechaTexto)) {
                fechaTexto = "--";
            }
            if (TextUtils.isEmpty(horaTexto)) {
                horaTexto = "--";
            }
            tvFechaHora.setText(context.getString(R.string.cita_cliente_fecha_hora, fechaTexto, horaTexto));

            String motivo = cita.getMotivo();
            if (TextUtils.isEmpty(motivo)) {
                motivo = context.getString(R.string.cita_cliente_sin_motivo);
            }
            tvMotivo.setText(motivo);

            String estado = cita.getEstado();
            if (TextUtils.isEmpty(estado)) {
                estado = context.getString(R.string.cita_estado_pendiente);
            }
            tvEstado.setText(estado);
            applyEstadoStyle(tvEstado, estado);
        }

        private void applyEstadoStyle(TextView chip, String estado) {
            chip.setBackgroundResource(R.drawable.bg_estado_chip);
            chip.getBackground().setTint(getEstadoColor(chip.getContext(), estado));
            chip.setTextColor(ContextCompat.getColor(chip.getContext(), android.R.color.white));
        }

        @ColorInt
        private int getEstadoColor(Context context, String estado) {
            if (estado == null) {
                return ContextCompat.getColor(context, R.color.estadoPendienteColor);
            }
            switch (estado.toLowerCase(Locale.getDefault())) {
                case "confirmada":
                    return ContextCompat.getColor(context, R.color.estadoConfirmadaColor);
                case "pospuesta":
                    return ContextCompat.getColor(context, R.color.estadoPospuestaColor);
                case "cancelada":
                    return ContextCompat.getColor(context, R.color.estadoCanceladaColor);
                case "completada":
                    return ContextCompat.getColor(context, R.color.estadoCompletadaColor);
                default:
                    return ContextCompat.getColor(context, R.color.estadoPendienteColor);
            }
        }
    }
}
