package com.example.jimenez_lozano_ruben_imdbapp.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesSync {

    public void syncLocalToFirestore(Context context, FavoritesDatabaseHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // 1. Obtener todos los IDs de películas en la base de datos local
        List<String> localMovieIds = new ArrayList<>();
        Cursor cursor = db.query(FavoritesDatabaseHelper.TABLE_NAME, new String[]{FavoritesDatabaseHelper.COLUMN_ID}, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String movieId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ID));
                localMovieIds.add(movieId);
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();

        // 2. Obtener todos los IDs de películas en Firestore
        firestore.collection("favorites")
                .document("QVQmKkTKGVHCCfnBQNHL")
                .collection("movies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> firestoreMovieIds = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        firestoreMovieIds.add(document.getId());
                    }

                    // 3. Identificar registros en Firestore que no están en la base local (eliminarlos)
                    for (String movieId : firestoreMovieIds) {
                        if (!localMovieIds.contains(movieId)) {
                            firestore.collection("favorites")
                                    .document("QVQmKkTKGVHCCfnBQNHL")
                                    .collection("movies")
                                    .document(movieId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Favorito eliminado de Firestore: " + movieId))
                                    .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al eliminar favorito de Firestore: " + movieId, e));
                        }
                    }

                    // 4. Identificar registros en la base local que no están en Firestore (añadirlos)
                    for (String movieId : localMovieIds) {
                        if (!firestoreMovieIds.contains(movieId)) {
                            syncSingleFavoriteToFirestore(context, dbHelper, movieId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al obtener documentos de Firestore", e));
    }
    // Método para sincronizar una sola película en Firestore
    private void syncSingleFavoriteToFirestore(Context context, FavoritesDatabaseHelper dbHelper, String movieId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(FavoritesDatabaseHelper.TABLE_NAME, null, FavoritesDatabaseHelper.COLUMN_ID + "=?", new String[]{movieId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            String userId = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE));
            String image = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE));
            String releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE));
            String rating = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING));
            String overview = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_OVERVIEW));

            Map<String, Object> favoriteMap = new HashMap<>();
            favoriteMap.put("id", movieId);
            favoriteMap.put("user_id", userId);
            favoriteMap.put("movie_title", title);
            favoriteMap.put("movie_image", image);
            favoriteMap.put("release_date", releaseDate);
            favoriteMap.put("movie_rating", rating);
            favoriteMap.put("overview", overview);

            firestore.collection("favorites")
                    .document("QVQmKkTKGVHCCfnBQNHL")
                    .collection("movies")
                    .document(movieId)
                    .set(favoriteMap)
                    .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Favorito sincronizado: " + movieId))
                    .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al sincronizar favorito: " + movieId, e));
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }


    public void removeFavoriteFromFirestore(String userEmail, String movieTitle) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("favorites")
                .document("QVQmKkTKGVHCCfnBQNHL")
                .collection("movies")
                .whereEqualTo("user_email", userEmail)
                .whereEqualTo("movie_title", movieTitle)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        firestore.collection("favorites")
                                .document("QVQmKkTKGVHCCfnBQNHL")
                                .collection("movies")
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Favorito eliminado de Firestore: " + movieTitle))
                                .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al eliminar favorito de Firestore: " + movieTitle, e));
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreSync", "Error al buscar documento para eliminar en Firestore: " + movieTitle, e));
    }
}
