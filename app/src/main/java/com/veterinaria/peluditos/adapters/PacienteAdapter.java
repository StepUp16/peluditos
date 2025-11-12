package com.veterinaria.peluditos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.Paciente;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder> {

    private final List<Paciente> pacientes = new ArrayList<>();
    private final DecimalFormat pesoFormat = new DecimalFormat("#.##");
    private OnPacienteActionListener actionListener;

    public interface OnPacienteActionListener {
        void onEdit(Paciente paciente);
        void onDelete(Paciente paciente);
        void onView(Paciente paciente);
    }

    public void setOnPacienteActionListener(OnPacienteActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente_listados, parent, false);
        return new PacienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
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

    class PacienteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPacienteName;
        private final TextView tvClientInfo;
        private final ShapeableImageView ivAvatar;
        private final View btnEdit;
        private final View btnDelete;

        PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPacienteName = itemView.findViewById(R.id.tvPacienteName);
            tvClientInfo = itemView.findViewById(R.id.tvClientPhone);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            btnEdit = itemView.findViewById(R.id.btnEditPaciente);
            btnDelete = itemView.findViewById(R.id.btnDeletePaciente);
        }

        void bind(Paciente paciente) {
            tvPacienteName.setText(paciente.getNombre());

            String especieRaza = buildEspecieRaza(paciente);
            String cliente = paciente.getClienteNombre();
            if (cliente == null || cliente.trim().isEmpty()) {
                cliente = itemView.getContext().getString(R.string.paciente_sin_cliente);
            }
            String detalle = itemView.getContext().getString(
                    R.string.paciente_item_detalle,
                    especieRaza,
                    cliente,
                    pesoFormat.format(paciente.getPeso())
            );
            tvClientInfo.setText(detalle);

            // Avatar por defecto; en el futuro podríamos cargar foto real
            ivAvatar.setImageResource(R.drawable.paciente);

            btnEdit.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEdit(paciente);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDelete(paciente);
                }
            });

            itemView.setOnClickListener(v -> {
                if (actionListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    actionListener.onView(pacientes.get(getBindingAdapterPosition()));
                }
            });
        }

        private String buildEspecieRaza(Paciente paciente) {
            String especie = paciente.getEspecie() != null ? paciente.getEspecie() : "";
            String raza = paciente.getRaza() != null ? paciente.getRaza() : "";
            if (raza.isEmpty()) {
                return especie;
            }
            return especie + " - " + raza;
        }
    }
}
