package com.veterinaria.peluditos.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    private OnCitaClickListener listener;

    public interface OnCitaClickListener {
        void onCitaClick(Cita cita);
    }

    public void setOnCitaClickListener(OnCitaClickListener listener) {
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

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreMascotaCliente = itemView.findViewById(R.id.tvNombreMascotaCliente);
            tvHoraCita = itemView.findViewById(R.id.tvHoraCita);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCitaClick(citas.get(getBindingAdapterPosition()));
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
        }
    }
}
