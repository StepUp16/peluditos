package com.veterinaria.peluditos;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;      // <-- 1. IMPORT NECESARIO
import android.os.Looper;     // <-- 2. IMPORT NECESARIO
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // 3. VARIABLE PARA CONTROLAR EL ESTADO DE CARGA
    // Esta variable le dirá al sistema cuándo estamos listos para mostrar la UI.
    private boolean isAppReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // La instalación de la splash screen debe ser lo primero
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // --- INICIO DE LA SOLUCIÓN ---

        // 4. MANTENER LA SPLASH SCREEN VISIBLE
        // Le decimos al sistema que mantenga la splash screen en pantalla
        // hasta que la condición (que isAppReady sea true) se cumpla.
        splashScreen.setKeepOnScreenCondition(() -> !isAppReady);

        // 5. SIMULAR UNA CARGA DE DATOS
        // Usamos un Handler para esperar un par de segundos antes de indicar que la app está lista.
        // En una aplicación real, aquí harías una llamada a una API, cargarías una base de datos, etc.
        // y solo cuando esa tarea termine, pondrías isAppReady = true.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Cuando el tiempo de espera termina, marcamos la app como lista.
            // Esto hará que la condición de setKeepOnScreenCondition se vuelva falsa
            // y se active la animación de salida (OnExitAnimationListener).
            isAppReady = true;
        }, 2000L); // 2000 milisegundos = 2 segundos de espera

        // --- FIN DE LA SOLUCIÓN ---

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Tu código de animación de salida está perfecto, se ejecutará ahora correctamente.
        splashScreen.setOnExitAnimationListener(new SplashScreen.OnExitAnimationListener() {
            @Override
            public void onSplashScreenExit(@NonNull SplashScreenViewProvider splashScreenView) {

                final View iconView = splashScreenView.getIconView();

                ObjectAnimator slideUp = ObjectAnimator.ofFloat(
                        iconView,
                        View.TRANSLATION_Y,
                        0f,
                        -iconView.getHeight() * 2f
                );
                slideUp.setInterpolator(new AnticipateInterpolator());
                slideUp.setDuration(800L);

                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                        iconView,
                        View.ALPHA,
                        1f,
                        0f
                );
                fadeOut.setInterpolator(new AnticipateInterpolator());
                fadeOut.setDuration(800L);

                slideUp.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        splashScreenView.remove();
                    }
                });

                slideUp.start();
                fadeOut.start();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.imgBienvenida), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnIniciar = findViewById(R.id.btnIniciar);
        btnIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Login_Peluditos.class);
                startActivity(intent);
            }
        });
    }
}