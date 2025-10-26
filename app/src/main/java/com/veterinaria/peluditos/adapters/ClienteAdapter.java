package com.veterinaria.peluditos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder> {
    private List<Usuario> usuarios = new ArrayList<>();

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.bind(usuario);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
        notifyDataSetChanged();
    }

    static class ClienteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvClientName;
        private final TextView tvClientPhone;
        private final ShapeableImageView ivUserAvatar;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientPhone = itemView.findViewById(R.id.tvClientPhone);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }

        public void bind(Usuario usuario) {
            String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
            tvClientName.setText(nombreCompleto);
            tvClientPhone.setText(usuario.getTelefono());
            // Por ahora usamos un avatar por defecto
            ivUserAvatar.setImageResource(R.drawable.user_javier);
        }
    }
}
