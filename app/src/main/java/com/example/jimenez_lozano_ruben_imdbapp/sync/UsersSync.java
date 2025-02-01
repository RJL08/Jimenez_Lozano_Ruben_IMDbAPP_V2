package com.example.jimenez_lozano_ruben_imdbapp.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.example.jimenez_lozano_ruben_imdbapp.database.UsersManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersSync {

    private static final String TAG = "FirestoreSync";

    public void syncLocalToFirestore(Context context, FavoritesDatabaseHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Cursor cursor = db.query(FavoritesDatabaseHelper.TABLE_USERS, null, null, null, null, null, null); // Cambiar el nombre de la tabla

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Leer datos de la base de datos local

                String userId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_NAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_EMAIL));
                String loginTime = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME));
                String logoutTime = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ADDRESS));  // Nuevo campo
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_PHONE));  // Nuevo campo
                String image = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_IMAGE));  // Nuevo campo

                // Leer el documento actual desde Firestore
                firestore.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Obtener el activity_log actual
                                List<Map<String, Object>> activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
                                if (activityLog == null) {
                                    activityLog = new ArrayList<>();
                                }

                                boolean isUpdated = false;
                                if (!activityLog.isEmpty()) {
                                    // Verificar la última entrada en el activity_log
                                    Map<String, Object> lastEntry = activityLog.get(activityLog.size() - 1);

                                    String lastLoginTime = (String) lastEntry.get("login_time");
                                    String lastLogoutTime = (String) lastEntry.get("logout_time");

                                    if (lastLogoutTime == null && loginTime != null && loginTime.equals(lastLoginTime)) {
                                        // Si la última entrada no tiene logout_time, actualizarlo
                                        lastEntry.put("logout_time", logoutTime);
                                        isUpdated = true;
                                    }
                                }

                                if (!isUpdated) {
                                    // Solo agregar nueva entrada si no es un duplicado
                                    boolean exists = activityLog.stream().anyMatch(entry ->
                                            loginTime != null && loginTime.equals(entry.get("login_time")) &&
                                                    logoutTime != null && logoutTime.equals(entry.get("logout_time"))
                                    );

                                    if (!exists) {
                                        Map<String, Object> newActivity = new HashMap<>();
                                        newActivity.put("login_time", loginTime);
                                        newActivity.put("logout_time", null); // Será null hasta que haya un logout
                                        activityLog.add(newActivity);
                                    }
                                }

                                // Subir el array actualizado a Firestore
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("user_id", userId);
                                userMap.put("name", name);
                                userMap.put("email", email);
                                userMap.put("address", address);  // Agregar dirección
                                userMap.put("phone", phone);  // Agregar teléfono
                                userMap.put("image", image);
                                userMap.put("activity_log", activityLog);

                                firestore.collection("users").document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Usuario sincronizado: " + userId))
                                        .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al sincronizar usuario: " + userId, e));
                            } else {
                                // Si el documento no existe, crearlo
                                Map<String, Object> newActivity = new HashMap<>();
                                newActivity.put("login_time", loginTime);
                                newActivity.put("logout_time", logoutTime);

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("user_id", userId);
                                userMap.put("name", name);
                                userMap.put("email", email);
                                userMap.put("address", address);  // Agregar dirección
                                userMap.put("phone", phone);  // Agregar teléfono
                                userMap.put("image", image);
                                userMap.put("activity_log", Arrays.asList(newActivity));

                                firestore.collection("users").document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Usuario sincronizado: " + userId))
                                        .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al sincronizar usuario: " + userId, e));
                            }
                        })
                        .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al obtener documento: " + userId, e));
            } while (cursor.moveToNext());
            cursor.close();
        }

    }


    /**
     * Sincroniza los datos de Firestore con la base de datos local.
     * @param context
     */
    public void syncUsersFromFirestore(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Log.d(TAG, "Usuario ID: " + document.getId() + " => Datos: " + document.getData());
                    // Leer activity_log como lista de mapas
                    List<Map<String, Object>> activityLog = (List<Map<String, Object>>) document.get("activity_log");

                    // Procesar el último login_time y logout_time del registro
                    String loginTime = null;
                    String logoutTime = null;
                    if (activityLog != null && !activityLog.isEmpty()) {
                        Map<String, Object> lastEntry = activityLog.get(activityLog.size() - 1);
                        loginTime = (String) lastEntry.get("login_time");
                        logoutTime = (String) lastEntry.get("logout_time");
                    }

                    // Guardar datos en la base de datos local
                    UsersManager dbHelper = new UsersManager(context);
                    dbHelper.addOrUpdateUser(
                            document.getString("user_id"),
                            document.getString("name"),
                            document.getString("email"),
                            loginTime,
                            logoutTime,
                            document.getString("image"),   // Imagen
                            document.getString("address"), // Dirección
                            document.getString("phone")      // Teléfono
                    );
                }
            } else {
                Log.w(TAG, "Error al obtener documentos de Firestore.", task.getException());
            }
        });
    }

    /**
     * Sincroniza los datos de la colección "users" de Firestore con la base de datos local.
     * Para cada documento en Firestore, se actualiza o inserta el usuario en la base de datos local.
     *
     * @param context Contexto de la aplicación (usualmente el Activity o Application).
     */
    public void syncFirestoreToLocal(Context context) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Recorrer cada documento obtenido de Firestore
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Se asume que las claves del documento coinciden con los nombres de campo
                            String userId = document.getString("user_id");
                            String name = document.getString("name");
                            String email = document.getString("email");
                            String address = document.getString("address");
                            String phone = document.getString("phone");
                            String image = document.getString("image");

                            // Si tienes campos de login/logout time, también puedes leerlos
                            String loginTime = document.getString("login_time");
                            String logoutTime = document.getString("logout_time");

                            // Usamos el UsersManager para actualizar o insertar el usuario en la BD local
                            UsersManager usersManager = new UsersManager(context);
                            // En este ejemplo, si no tienes loginTime/logoutTime actualizados, puedes pasar null
                            usersManager.addOrUpdateUser(
                                    userId,
                                    name,
                                    email,
                                    loginTime,    // O null si no es relevante en esta sincronización
                                    logoutTime,   // O null
                                    image,
                                    address,
                                    phone
                            );
                        }
                        Log.d(TAG, "Sincronización de Firestore a local completada.");
                    } else {
                        Log.e(TAG, "Error al sincronizar de Firestore a local: " + task.getException().getMessage());
                    }
                });
    }
}

