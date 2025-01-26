package com.example.jimenez_lozano_ruben_imdbapp.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;

public class FavoritesSync {

    public void syncLocalToFirestore(Context context, FavoritesDatabaseHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Cursor cursor = db.query(FavoritesDatabaseHelper.TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Leer datos de la base de datos local
                String movieId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ID));
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE));
                String releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE));
                String rating = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING));
                String overview = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_OVERVIEW));

                // Crear mapa con los datos para enviar a Firestore
                Map<String, Object> favoriteMap = new HashMap<>();
                favoriteMap.put("id", movieId);
                favoriteMap.put("user_id", userId);
                favoriteMap.put("movie_title", title);
                favoriteMap.put("movie_image", image);
                favoriteMap.put("release_date", releaseDate);
                favoriteMap.put("movie_rating", rating);
                favoriteMap.put("overview", overview);

                // Subir datos a Firestore
                firestore.collection("favorites")

                        .document("QVQmKkTKGVHCCfnBQNHL")
                        .collection("movies")
                        .document(movieId) // Usa el movie_id como ID del documento en Firestore
                        .set(favoriteMap)
                        .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Favorito sincronizado: " + movieId))
                        .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al sincronizar favorito: " + movieId, e));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
    }
}
