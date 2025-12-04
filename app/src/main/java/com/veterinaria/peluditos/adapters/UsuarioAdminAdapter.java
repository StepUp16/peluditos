package com.veterinaria.peluditos.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.veterinaria.peluditos.R;
import com.veterinaria.peluditos.data.Usuario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UsuarioAdminAdapter extends RecyclerView.Adapter<UsuarioAdminAdapter.UsuarioViewHolder> {

    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Usuario> usuariosFiltrados = new ArrayList<>();
    private String currentQuery = "";
    private OnUsuarioActionListener actionListener;

    public interface OnUsuarioActionListener {
        void onDelete(Usuario usuario);
    }

    public void setOnUsuarioActionListener(OnUsuarioActionListener listener) {
        this.actionListener = listener;
    }

    public void setUsuarios(List<Usuario> nuevosUsuarios) {
        usuarios.clear();
        if (nuevosUsuarios != null) {
            usuarios.addAll(nuevosUsuarios);
            Collections.sort(usuarios, Comparator.comparing(this::buildNombreCompleto, String::compareToIgnoreCase));
        }
        aplicarFiltro(currentQuery);
    }

    public void filter(String query) {
        currentQuery = query != null ? query.trim().toLowerCase() : "";
        aplicarFiltro(currentQuery);
    }

    private void aplicarFiltro(String query) {
        usuariosFiltrados.clear();
        if (usuarios.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        if (query == null || query.isEmpty()) {
            usuariosFiltrados.addAll(usuarios);
        } else {
            for (Usuario usuario : usuarios) {
                if (matches(usuario, query)) {
                    usuariosFiltrados.add(usuario);
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean matches(Usuario usuario, String query) {
        String nombre = buildNombreCompleto(usuario).toLowerCase();
        String email = usuario.getEmail() != null ? usuario.getEmail().toLowerCase() : "";
        String rol = usuario.getRol() != null ? usuario.getRol().toLowerCase() : "";
        return nombre.contains(query) || email.contains(query) || rol.contains(query);
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario_admin, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        holder.bind(usuariosFiltrados.get(position));
    }

    @Override
    public int getItemCount() {
        return usuariosFiltrados.size();
    }

    class UsuarioViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView ivUserAvatar;
        private final TextView tvUserName;
        private final TextView tvUserEmail;
        private final TextView tvUserRole;
        private final ImageButton btnDelete;

        UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Usuario usuario) {
            String nombreCompleto = buildNombreCompleto(usuario);
            tvUserName.setText(nombreCompleto);
            tvUserEmail.setText(!TextUtils.isEmpty(usuario.getEmail()) ? usuario.getEmail() : "-");
            tvUserRole.setText(formatRole(usuario.getRol()));

            // Limpiamos la vista primero para evitar que se reciclen imágenes viejas
            ivUserAvatar.setImageDrawable(null);

            if (!TextUtils.isEmpty(usuario.getFotoUrl())) {
                String fotoUrl = usuario.getFotoUrl();
                if (fotoUrl.startsWith("http")) {
                    // Legacy URL (broken/paid) - Show placeholder immediately
                    ivUserAvatar.setImageResource(R.drawable.user_sofia);
                } else {
                    try {
                        byte[] imageByteArray = android.util.Base64.decode(fotoUrl, android.util.Base64.DEFAULT);
                        Glide.with(itemView.getContext())
                                .asBitmap()
                                .load(imageByteArray)
                                .placeholder(R.drawable.user_sofia)
                                .dontAnimate()
                                .into(ivUserAvatar);
                    } catch (IllegalArgumentException e) {
                        ivUserAvatar.setImageResource(R.drawable.user_sofia);
                    }
                }
            } else {
                ivUserAvatar.setImageResource(R.drawable.user_sofia);
            }

            btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && actionListener != null) {
                    actionListener.onDelete(usuariosFiltrados.get(pos));
                }
            });
        }
    }

    private String buildNombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";
        return (nombre + " " + apellido).trim();
    }

    private String formatRole(String rol) {
        if (TextUtils.isEmpty(rol)) {
            return "-";
        }
        rol = rol.trim();
        return rol.substring(0, 1).toUpperCase() + rol.substring(1).toLowerCase();
    }
}
