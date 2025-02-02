package com.example.jimenez_lozano_ruben_imdbapp.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;

import java.util.Map;

public class FavoritesSync {

    private static final String TAG = "FavoritesSync";

    /**
     * Sincroniza los favoritos locales desde SQLite a Firestore.
     */
    public void syncLocalToFirestore(Context context, FavoritesDatabaseHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Cursor cursor = db.query(FavoritesDatabaseHelper.TABLE_FAVORITES, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Leer datos de la base de datos local
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_ID));
                String movieId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_FAVORITE_ID));
                String userEmail = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_EMAIL));
                String movieTitle = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE));
                String movieImage = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE));
                String releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE));
                String movieRating = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING));
                String overview = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_OVERVIEW));

                // Crear mapa para los datos de la película
                Map<String, Object> movieMap = new HashMap<>();
                movieMap.put("id", movieId);
                movieMap.put("userEmail", userEmail);
                movieMap.put("movieTitle", movieTitle);
                movieMap.put("movieImage", movieImage);
                movieMap.put("releaseDate", releaseDate);
                movieMap.put("movieRating", movieRating);
                movieMap.put("overview", overview);

                // Subir datos a Firestore en la estructura deseada
                firestore.collection("favorites")
                        .document(userId)
                        .collection("movies")
                        .document(movieId)
                        .set(movieMap)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Favorito sincronizado: " + movieId))
                        .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar favorito: " + movieId, e));

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    /**
     * Sincroniza los favoritos desde Firestore a SQLite.
     */
    public void syncFirestoreToLocal(Context context, FavoritesDatabaseHelper dbHelper) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Obtener el userId del usuario autenticado
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (userId == null) {
            Log.e(TAG, "Usuario no autenticado. No se pueden descargar los favoritos.");
            return;
        }

        firestore.collection("favorites")
                .document(userId)
                .collection("movies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    // Limpiar favoritos locales antes de sincronizar
                    db.execSQL("DELETE FROM " + FavoritesDatabaseHelper.TABLE_FAVORITES + " WHERE " + FavoritesDatabaseHelper.COLUMN_USER_ID + "='" + userId + "'");

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        ContentValues values = new ContentValues();
                        values.put(FavoritesDatabaseHelper.COLUMN_FAVORITE_ID, document.getId());
                        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
                        values.put(FavoritesDatabaseHelper.COLUMN_USER_EMAIL, document.getString("userEmail"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE, document.getString("movieTitle"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE, document.getString("movieImage"));
                        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, document.getString("releaseDate"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING, document.getString("movieRating"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_OVERVIEW, document.getString("overview"));

                        db.insert(FavoritesDatabaseHelper.TABLE_FAVORITES, null, values);
                    }

                    db.close();
                    Log.d(TAG, "Favoritos descargados y guardados localmente.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al descargar favoritos desde Firestore.", e));
    }
}
