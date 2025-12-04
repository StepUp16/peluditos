package com.veterinaria.peluditos.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.Paciente;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PacienteClienteAdapter extends RecyclerView.Adapter<PacienteClienteAdapter.PacienteClienteViewHolder> {

    private final List<Paciente> pacientes = new ArrayList<>();
    private final DecimalFormat pesoFormat = new DecimalFormat("#.##");
    private OnPacienteClickListener clickListener;

    public interface OnPacienteClickListener {
        void onPacienteClick(Paciente paciente);
    }

    public void setOnPacienteClickListener(OnPacienteClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public PacienteClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente_cliente, parent, false);
        return new PacienteClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteClienteViewHolder holder, int position) {
        holder.bind(pacientes.get(position));
    }

    @Override
    public int getItemCount() {
        return pacientes.size();
    }

    public void setPacientes(List<Paciente> nuevosPacientes) {
        pacientes.clear();
        if (nuevosPacientes != null) {
            pacientes.addAll(nuevosPacientes);
        }
        notifyDataSetChanged();
    }

    class PacienteClienteViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNombre;
        private final TextView tvDetalle;
        private final ShapeableImageView ivAvatar;

        PacienteClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvPacienteName);
            tvDetalle = itemView.findViewById(R.id.tvPacienteDetalle);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }

        void bind(Paciente paciente) {
            tvNombre.setText(paciente.getNombre());
            String especieRaza = buildEspecieRaza(paciente);
            String peso = pesoFormat.format(paciente.getPeso());
            tvDetalle.setText(itemView.getContext().getString(
                    R.string.paciente_cliente_detalle,
                    especieRaza,
                    peso
            ));

            // Limpiamos la vista primero para evitar que se reciclen imágenes viejas
            ivAvatar.setImageDrawable(null);

            if (!TextUtils.isEmpty(paciente.getFotoUrl())) {
                String fotoUrl = paciente.getFotoUrl();
                if (fotoUrl.startsWith("http")) {
                    // Legacy URL (broken/paid) - Show placeholder immediately
                    ivAvatar.setImageResource(R.drawable.paciente);
                } else {
                    try {
                        byte[] imageByteArray = android.util.Base64.decode(fotoUrl, android.util.Base64.DEFAULT);
                        Glide.with(itemView.getContext())
                                .asBitmap()
                                .load(imageByteArray)
                                .placeholder(R.drawable.paciente)
                                .dontAnimate()
                                .into(ivAvatar);
                    } catch (IllegalArgumentException e) {
                        ivAvatar.setImageResource(R.drawable.paciente);
                    }
                }
            } else {
                ivAvatar.setImageResource(R.drawable.paciente);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onPacienteClick(pacientes.get(getBindingAdapterPosition()));
                }
            });
        }

        private String buildEspecieRaza(Paciente paciente) {
            String especie = paciente.getEspecie() != null ? paciente.getEspecie().trim() : "";
            String raza = paciente.getRaza() != null ? paciente.getRaza().trim() : "";

            if (TextUtils.isEmpty(especie)) {
                return raza;
            } else if (TextUtils.isEmpty(raza)) {
                return especie;
            }
            return especie + " • " + raza;
        }
    }
}


