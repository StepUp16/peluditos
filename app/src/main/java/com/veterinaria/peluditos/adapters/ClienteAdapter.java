package com.veterinaria.peluditos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Usuario usuario);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new ClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.bind(usuario, deleteListener);
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
        private final TextView tvUserName;
        private final TextView tvUserRole;
        private final ShapeableImageView ivUserAvatar;
        private final ImageButton btnDelete;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Usuario usuario, OnDeleteClickListener listener) {
            String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
            tvUserName.setText(nombreCompleto);
            tvUserRole.setText(usuario.getRol());
            // Por ahora usamos un avatar por defecto
            ivUserAvatar.setImageResource(R.drawable.user_javier);

            // Configurar el botón de eliminar
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(usuario);
                }
            });
        }
    }
}
