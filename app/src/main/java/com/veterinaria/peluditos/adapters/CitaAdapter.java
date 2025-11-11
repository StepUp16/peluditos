package com.veterinaria.peluditos.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.Cita;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaViewHolder> {

    private final List<Cita> citas = new ArrayList<>();
    private final SimpleDateFormat horaFormato = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private OnCitaActionListener listener;

    public interface OnCitaActionListener {
        void onCitaClick(Cita cita);
        void onCambiarEstado(Cita cita);
    }

    public void setOnCitaActionListener(OnCitaActionListener listener) {
        this.listener = listener;
    }

    public void setCitas(List<Cita> nuevasCitas) {
        citas.clear();
        if (nuevasCitas != null) {
            citas.addAll(nuevasCitas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_cita_listado_item, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        holder.bind(citas.get(position));
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    class CitaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombreMascotaCliente;
        private final TextView tvHoraCita;
        private final TextView tvEstadoCita;
        private final ImageButton btnEstadoMenu;

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreMascotaCliente = itemView.findViewById(R.id.tvNombreMascotaCliente);
            tvHoraCita = itemView.findViewById(R.id.tvHoraCita);
            tvEstadoCita = itemView.findViewById(R.id.tvEstadoCita);
            btnEstadoMenu = itemView.findViewById(R.id.btnEstadoMenu);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCitaClick(citas.get(getBindingAdapterPosition()));
                }
            });

            btnEstadoMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCambiarEstado(citas.get(getBindingAdapterPosition()));
                }
            });
        }

        void bind(Cita cita) {
            String nombre = itemView.getContext().getString(
                    R.string.cita_item_titulo,
                    cita.getPacienteNombre(),
                    cita.getClienteNombre()
            );
            tvNombreMascotaCliente.setText(nombre);

            String horaTexto = cita.getHora();
            if (!TextUtils.isEmpty(horaTexto)) {
                tvHoraCita.setText(horaTexto);
            } else {
                tvHoraCita.setText(horaFormato.format(cita.getFechaHoraTimestamp()));
            }

            String estado = cita.getEstado();
            if (TextUtils.isEmpty(estado)) {
                estado = itemView.getContext().getString(R.string.cita_estado_pendiente);
            }
            tvEstadoCita.setText(estado);
            applyEstadoStyle(tvEstadoCita, estado);
        }

        private void applyEstadoStyle(TextView chip, String estado) {
            Context ctx = chip.getContext();
            chip.setBackgroundResource(R.drawable.bg_estado_chip);
            chip.getBackground().setTint(getEstadoColor(ctx, estado));
            chip.setTextColor(0xFFFFFFFF);
        }

        @ColorInt
        private int getEstadoColor(Context ctx, String estado) {
            if (estado == null) return ctx.getColor(R.color.estadoPendienteColor);
            switch (estado.toLowerCase(Locale.getDefault())) {
                case "confirmada":
                    return ctx.getColor(R.color.estadoConfirmadaColor);
                case "pospuesta":
                    return ctx.getColor(R.color.estadoPospuestaColor);
                case "cancelada":
                    return ctx.getColor(R.color.estadoCanceladaColor);
                case "completada":
                    return ctx.getColor(R.color.estadoCompletadaColor);
                default:
                    return ctx.getColor(R.color.estadoPendienteColor);
            }
        }
    }
}
