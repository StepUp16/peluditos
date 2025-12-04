package com.veterinaria.peluditos.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64; // IMPORTANTE: Agregado para la conversión
import android.util.Log;

import androidx.annotation.NonNull;

// IMPORTANTE: Ya no necesitamos Firebase Storage
// import com.google.firebase.storage.FirebaseStorage;
// import com.google.firebase.storage.StorageReference;
// import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacientePhotoManager {

    private static final String TAG = "PacientePhotoManager";

    // REDUCIDO: De 1280 a 800. Firestore explota si el documento pesa mas de 1MB.
    // 800x800 es suficiente para verla bien en el celular.
    private static final int MAX_DIMENSION = 800;

    // private final FirebaseStorage storage = FirebaseStorage.getInstance(); // ELIMINADO
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String folder;

    public PacientePhotoManager() {
        this("pacientes");
    }

    public PacientePhotoManager(String folder) {
        this.folder = TextUtils.isEmpty(folder) ? "pacientes" : folder;
    }

    public interface UploadCallback {
        // El String ahora será la FOTO EN BASE64, no una URL.
        void onSuccess(@NonNull String downloadUrl);
        void onError(@NonNull Exception exception);
    }

    public void uploadPhoto(Context context,
                            Uri imageUri,
                            String entityId,
                            UploadCallback callback) {
        if (context == null || imageUri == null || TextUtils.isEmpty(entityId)) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Datos insuficientes para cargar la foto"));
            }
            return;
        }
        executor.execute(() -> {
            try {
                byte[] imageData = compressImage(context.getApplicationContext(), imageUri);
                if (imageData == null || imageData.length == 0) {
                    throw new IOException("No se pudo leer la imagen seleccionada");
                }

                // Convertir a Base64 en lugar de subir a Firebase Storage
                String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.DEFAULT);

                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(base64Image);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al procesar foto", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    private byte[] compressImage(Context context, Uri uri) throws IOException {
        Bitmap bitmap = decodeScaledBitmap(context, uri);
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // REDUCIDO: Calidad bajada a 60 para ahorrar espacio.
        // Aún se ve bien en pantalla de celular.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);

        bitmap.recycle();
        return stream.toByteArray();
    }

    private Bitmap decodeScaledBitmap(Context context, Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION);
        options.inJustDecodeBounds = false;

        InputStream secondStream = context.getContentResolver().openInputStream(uri);
        if (secondStream == null) {
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeStream(secondStream, null, options);
        secondStream.close();
        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    public void dispose() {
        executor.shutdown();
    }
}