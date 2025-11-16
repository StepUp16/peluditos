package com.veterinaria.peluditos.adapters;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veterinaria.peluditos.R;

import java.util.ArrayList;
import java.util.List;

public class AlertaClienteAdapter extends RecyclerView.Adapter<AlertaClienteAdapter.AlertaViewHolder> {

    public static class AlertaItem {
        public final String titulo;
        public final String descripcion;
        public final long timestamp;
        public final int iconRes;

        public AlertaItem(String titulo, String descripcion, long timestamp, int iconRes) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.timestamp = timestamp;
            this.iconRes = iconRes;
        }
    }

    private final List<AlertaItem> alertas = new ArrayList<>();

    public void setAlertas(List<AlertaItem> nuevasAlertas) {
        alertas.clear();
        if (nuevasAlertas != null) {
            alertas.addAll(nuevasAlertas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alerta_cliente, parent, false);
        return new AlertaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertaViewHolder holder, int position) {
        holder.bind(alertas.get(position));
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    static class AlertaViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgIcono;
        private final TextView tvTitulo;
        private final TextView tvDescripcion;
        private final TextView tvFecha;

        AlertaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcono = itemView.findViewById(R.id.imgIconoAlerta);
            tvTitulo = itemView.findViewById(R.id.tvTituloAlerta);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionAlerta);
            tvFecha = itemView.findViewById(R.id.tvFechaAlerta);
        }

        void bind(AlertaItem item) {
            if (item.iconRes != 0) {
                imgIcono.setImageResource(item.iconRes);
            }
            tvTitulo.setText(!TextUtils.isEmpty(item.titulo) ? item.titulo : "");
            tvDescripcion.setText(!TextUtils.isEmpty(item.descripcion) ? item.descripcion : "");

            if (item.timestamp > 0) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        item.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
                tvFecha.setText(relativeTime);
            } else {
                tvFecha.setText("");
            }
        }
    }
}
