package com.example.jimenez_lozano_ruben_imdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import com.example.jimenez_lozano_ruben_imdbapp.sync.FavoritesSync;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.ArrayList;
import java.util.List;


/**
 * Clase que gestiona las operaciones relacionadas con la lista de favoritos de los usuarios.
 * implementamos metodos para agregar, eliminar y recuperar películas favoritas desde la base de datos.
 */
public class FavoritesManager {

    // Declaramos el helper de la base de datos
    private FavoritesDatabaseHelper dbHelper;
    private Context context;

    /**
     * Constructor que inicializa el gestor de favoritos con el contexto proporcionado.
     *
     * @param context El contexto de la aplicación o actividad.
     */
    public FavoritesManager(Context context) {

        this.context = context;
        dbHelper = new FavoritesDatabaseHelper(context);
    }

    /**
     * Agrega una pelicula a la lista de favoritos de la base de datos.
     * @param id          El ID unico de la pelicula.
     * @param userEmail   El correo del usuario actual.
     * @param movieTitle  El titulo de la pelicula.
     * @param movieImage  La URL de la imagen de la pelicula.
     * @param releaseDate La fecha de lanzamiento de la pelicula.
     * @param movieRating La puntuación de la pelicula.
     * @param overview    La descripción de la pelicula.
     * @return true si la pelicula se añadio correctamente, false en caso contrario.
     */
    public boolean addFavorite(String id, String userEmail, String movieTitle, String movieImage, String releaseDate, String movieRating, String overview, String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_FAVORITE_ID, id);
        values.put(FavoritesDatabaseHelper.COLUMN_USER_EMAIL, userEmail);
        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE, movieTitle);
        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE, movieImage);
        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, releaseDate);
        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING, movieRating);
        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_OVERVIEW, overview);
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);//********


        long result = db.insert(FavoritesDatabaseHelper.TABLE_FAVORITES, null, values);

       db.close();
        // Sincronizar con Firestore

        FavoritesSync favoritesSync = new FavoritesSync();
        favoritesSync.syncLocalToFirestore(context, dbHelper);

        // Devolvemos true si la inserción fue exitosaosa
        return result != -1;
    }

    /**
     * MEtodo para eliminar una pelicula de la lista de favoritos de la base de datos.
     * @param userEmail El correo del usuario actual.
     * @param movieTitle El titulo de la película a eliminar.
     * @return true si la pelicula fue eliminada correctamente, false en caso contrario.
     */
    public boolean removeFavorite(String userEmail, String movieTitle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                FavoritesDatabaseHelper.COLUMN_USER_ID + "=? AND " + FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE + "=?", //*******
                new String[]{userEmail, movieTitle}
        );
        db.close();
        // Sincronizar con Firestore
        if (rowsDeleted > 0) {
           new FavoritesSync().syncLocalToFirestore(context, dbHelper);
        }


        return rowsDeleted > 0;
    }


    /**
     * Recuperamos mediante un cursor con las peliculas favoritas del usuario desde la base de datos.
     * Consiguiendo que cada usuario tenga su propia lista de fovirtos
     * @param userId El id del usuario actual.
     * @return Un cursor con los registros de las películas favoritas.
     */
    public Cursor getFavoritesCursor(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.query(
                // Nombre de la tabla
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",//******
                new String[]{userId},
                null, null, null
        );

    }

    /**
     * Convierte un cursor en una lista de peliculas favoritas.
     * @param cursor El cursor con los registros de las peliculas favoritas.
     * @return Una lista de objetos de tipo movies con las peliculas favoritas.
     */
    public List<Movies> getFavoritesList(Cursor cursor) {
        List<Movies> favoriteMovies = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Movies movie = new Movies();
                movie.setId(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_FAVORITE_ID)));
                movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE)));
                movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE)));
                movie.setReleaseYear(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE)));
                movie.setRating(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING)));
                movie.setOverview(cursor.getString(cursor.getColumnIndexOrThrow("overview"))); // Obtener descripción
                favoriteMovies.add(movie);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return favoriteMovies;
    }

    /**
     * Metodo para eliminar una pelicula de la base de datos local y de Firestore.
     * @param userId
     * @param movieId
     * @return
     */
    public boolean deleteMovie(String userId, String movieId) {
        boolean isDeletedFromLocal = false;

        try {
            // 1. Eliminar de la base de datos local
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete(
                    FavoritesDatabaseHelper.TABLE_FAVORITES,
                    FavoritesDatabaseHelper.COLUMN_USER_ID + "=? AND " + FavoritesDatabaseHelper.COLUMN_FAVORITE_ID + "=?",
                    new String[]{userId, movieId}
            );
            db.close();

            if (rowsDeleted > 0) {
                isDeletedFromLocal = true;
                Log.d("DeleteMovie", "Película eliminada de SQLite: " + movieId);
            } else {
                Log.e("DeleteMovie", "Error al eliminar de SQLite. movieId: " + movieId + ", userId: " + userId);
            }

            // 2. Eliminar de Firestore si fue eliminada de SQLite
            if (isDeletedFromLocal) {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection("favorites")
                        .document(userId)
                        .collection("movies")
                        .document(movieId)
                        .delete()
                        .addOnSuccessListener(aVoid -> Log.d("DeleteMovie", "Película eliminada de Firestore: " + movieId))
                        .addOnFailureListener(e -> Log.e("DeleteMovie", "Error al eliminar de Firestore: " + e.getMessage(), e));
            }
        } catch (Exception e) {
            Log.e("DeleteMovie", "Error general al eliminar película: " + e.getMessage(), e);
        }

        return isDeletedFromLocal;
    }
}


