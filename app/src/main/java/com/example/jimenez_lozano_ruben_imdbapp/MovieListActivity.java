package com.example.jimenez_lozano_ruben_imdbapp;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import androidx.recyclerview.widget.RecyclerView;
import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesManager;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import com.example.jimenez_lozano_ruben_imdbapp.ui.adapter.MovieAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * actividad para mostrar la lista de peliculas, una vez que e ha filtrado por
 * genero y año
 */
public class MovieListActivity extends AppCompatActivity {

    //Declaramos las variables
    private RecyclerView moviesRecyclerView;
    private MovieAdapter movieAdapter;
    private List<Movies> moviesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        // Inicializamos el RecyclerView
        moviesRecyclerView = findViewById(R.id.moviesRecyclerView);
        moviesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Obtenemos la lista de películas desde el Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("movies_list")) {
            moviesList = intent.getParcelableArrayListExtra("movies_list");
        } else {
            moviesList = new ArrayList<>();
        }



        /*
         * Configuramos el adaptador para gestionar la lista de películas.
         * Implementamos la logica para abrir detalles de una pelicula al hacer clic
         * y para añadirla a favoritos con un clic largo.
         */
        movieAdapter = new MovieAdapter(
                moviesList,
                movie -> {
                    // onClick: para abrir los detalles de la pelicula
                    Intent detailIntent = new Intent(this, MovieDetailsActivity.class);
                    detailIntent.putExtra("movie", movie); // Mantiene la funcionalidad Parcelable
                    startActivity(detailIntent);
                },
                movie -> {
                    // onLongClick: para añadir la pelicula a favoritos
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                    String userEmail = prefs.getString("userEmail", "");
                    String userId = prefs.getString("userId", "");//********

                    if (userId.isEmpty()) {
                        Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Crear una instancia de FavoritesManager para gestionar favoritos en nuestra base de datos
                    FavoritesManager favoritesManager = new FavoritesManager(this);

                    // Obtenemos la lista actual de favoritos
                    Cursor cursor = favoritesManager.getFavoritesCursor(userId);
                    List<Movies> existingFavorites = favoritesManager.getFavoritesList(cursor);

                    // Cerramos el cursor y asi liberamos recuersos
                    if (cursor != null) {
                        cursor.close();
                    }

                    // Comprobamos que no tengamos peliculas duplicadas
                    for (Movies favorite : existingFavorites) {
                        if (favorite.getTitle().equals(movie.getTitle())) {
                            Toast.makeText(this, "Esta película ya está en favoritos", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // Comprobamos el  límite de favoritos
                    if (existingFavorites.size() >= 6) {
                        Toast.makeText(this, "No puedes añadir más de 6 películas a favoritos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        // Añadimos la pelicula a favoritos
                        boolean added = favoritesManager.addFavorite(
                                movie.getId(),              // Nuevo argumento: ID de la películ
                                userEmail,                  // Email del usuario
                                movie.getTitle(),           // Título de la pelicula
                                movie.getImageUrl(),        // URL de la imagen
                                movie.getReleaseYear(),     // Fecha de lanzamiento
                                movie.getRating(),          // Puntuacion
                                movie.getOverview(),       // Nuevo argumento: Descripción de la película
                                userId

                        );

                        if (added) {
                            Toast.makeText(this, "Película agregada a favoritos", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error al agregar a favoritos", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error inesperado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("MovieListActivity", "Error al agregar a favoritos", e);
                    }
                }
        );
        // Asignamos el adaptador al RecyclerView para mostrar las peliculas
        moviesRecyclerView.setAdapter(movieAdapter);
    }
}