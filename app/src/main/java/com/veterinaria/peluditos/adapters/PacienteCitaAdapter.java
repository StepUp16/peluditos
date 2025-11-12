package com.veterinaria.peluditos.adapters;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PacienteCitaAdapter extends RecyclerView.Adapter<PacienteCitaAdapter.CitaViewHolder> {

    private final List<Cita> citas = new ArrayList<>();
    private final SimpleDateFormat fechaFormato =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente_cita_simple, parent, false);
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

    public void setCitas(List<Cita> nuevasCitas) {
        citas.clear();
        if (nuevasCitas != null) {
            citas.addAll(nuevasCitas);
        }
        notifyDataSetChanged();
    }

    static class CitaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitulo;
        private final TextView tvFecha;
        private final TextView tvEstado;
        private final SimpleDateFormat fechaFormato =
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvCitaTitulo);
            tvFecha = itemView.findViewById(R.id.tvCitaFecha);
            tvEstado = itemView.findViewById(R.id.tvCitaEstado);
        }

        void bind(Cita cita) {
            tvTitulo.setText(!isEmpty(cita.getMotivo()) ? cita.getMotivo() : itemView.getContext().getString(R.string.label_motivo_consulta));
            String fechaTexto = cita.getFechaHoraTimestamp() > 0
                    ? fechaFormato.format(new Date(cita.getFechaHoraTimestamp()))
                    : cita.getFecha() + " " + cita.getHora();
            tvFecha.setText(fechaTexto);
            tvEstado.setText(!isEmpty(cita.getEstado())
                    ? cita.getEstado()
                    : itemView.getContext().getString(R.string.cita_estado_pendiente));
        }

        private boolean isEmpty(String text) {
            return text == null || text.trim().isEmpty();
        }
    }
}
