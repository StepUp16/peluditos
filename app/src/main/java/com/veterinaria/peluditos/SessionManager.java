package com.veterinaria.peluditos;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "PeluditosPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ROLE = "user_role";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Crear una sesión de login
     */
    public void createLoginSession(String userRole) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ROLE, userRole);
        editor.apply();
    }

    /**
     * Guardar datos de "Recordarme"
     */
    public void saveRememberMe(boolean remember, String email) {
        if (remember) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_EMAIL, email);
        } else {
            editor.putBoolean(KEY_REMEMBER_ME, false);
            editor.remove(KEY_EMAIL);
        }
        editor.apply();
    }

    /**
     * Verificar si el usuario está logueado
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Obtener el rol del usuario
     */
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, "");
    }

    /**
     * Verificar si "Recordarme" está activado
     */
    public boolean isRememberMeEnabled() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    /**
     * Obtener el email guardado
     */
    public String getSavedEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "");
    }

    /**
     * Cerrar sesión y limpiar datos
     */
    public void logoutUser() {
        // Mantener los datos de "Recordarme" si estaban activados
        boolean rememberMe = isRememberMeEnabled();
        String savedEmail = getSavedEmail();

        // Limpiar todas las preferencias
        editor.clear();

        // Restaurar datos de "Recordarme" si era necesario
        if (rememberMe) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_EMAIL, savedEmail);
        }

        editor.apply();
    }

    /**
     * Limpiar completamente todas las preferencias
     */
    public void clearAllData() {
        editor.clear();
        editor.apply();
    }
}