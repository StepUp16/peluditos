package com.veterinaria.peluditos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.HistorialMedico;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialMedicoAdapter extends RecyclerView.Adapter<HistorialMedicoAdapter.HistorialViewHolder> {

    private final List<HistorialMedico> historiales = new ArrayList<>();
    private final SimpleDateFormat fechaFormato =
            new SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_medico_timeline, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        holder.bind(historiales.get(position));
    }

    @Override
    public int getItemCount() {
        return historiales.size();
    }

    public void setHistoriales(List<HistorialMedico> nuevosHistoriales) {
        historiales.clear();
        if (nuevosHistoriales != null) {
            historiales.addAll(nuevosHistoriales);
        }
        notifyDataSetChanged();
    }

    static class HistorialViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHistoryTitle;
        private final TextView tvHistoryDetails;
        private final SimpleDateFormat fechaFormato =
                new SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault());

        HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvHistoryDetails = itemView.findViewById(R.id.tvHistoryDetails);
        }

        void bind(HistorialMedico historial) {
            tvHistoryTitle.setText(
                    historial.getMotivoConsulta() != null && !historial.getMotivoConsulta().isEmpty()
                            ? historial.getMotivoConsulta()
                            : itemView.getContext().getString(R.string.label_motivo_consulta)
            );

            String fechaDetalle = historial.getTimestampRegistro() > 0
                    ? fechaFormato.format(new Date(historial.getTimestampRegistro()))
                    : historial.getFechaConsulta() + " " + historial.getHoraConsulta();

            String diagnostico = historial.getDiagnostico() != null ? historial.getDiagnostico() : "";
            tvHistoryDetails.setText(itemView.getContext()
                    .getString(R.string.historial_detalle_formato, fechaDetalle, diagnostico));
        }
    }
}
