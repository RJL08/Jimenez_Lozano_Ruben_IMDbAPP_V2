package com.example.jimenez_lozano_ruben_imdbapp.ui.home;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jimenez_lozano_ruben_imdbapp.MovieDetailsActivity;
import com.example.jimenez_lozano_ruben_imdbapp.api.IMDBApiService;
import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesManager;
import com.example.jimenez_lozano_ruben_imdbapp.database.UsersManager;
import com.example.jimenez_lozano_ruben_imdbapp.databinding.FragmentHomeBinding;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import com.example.jimenez_lozano_ruben_imdbapp.models.MovieOverviewResponse;
import com.example.jimenez_lozano_ruben_imdbapp.ui.adapter.MovieAdapter;
import com.example.jimenez_lozano_ruben_imdbapp.utils.IMDBApiClient;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



/**
 * fragmento principal que muestra el top 10 de películas populares
 * y permite al usuario interactuar con ellas, incluyendo añadirlas a favoritos.
 */
public class HomeFragment extends Fragment {

    //Declaramos las varibales
    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private List<Movies> movieList = new ArrayList<>();



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // configuramos el recyclerview con un diseño de cuadricula
        recyclerView = binding.recyclerViewTopMovies;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // Grid de 2 columnas

        // Inicializamos el adaptador con la logica de clic corto y clic largo
        adapter = new MovieAdapter(movieList, this::onMovieClick, this::onMovieLongClick);
        recyclerView.setAdapter(adapter);

        // Llamamos al metodo que realiza la solicitud al API para cargar el Top 10
        fetchTopMovies();
        // Devolvemos la vista
        return root;

    }

    /**
     * Método para realizar la solicitud al API para cargar el Top 10 de peliculas.
     */
    private void fetchTopMovies() {
        // Creamos un nuevo hilo para la solicitud y asi evitar bloquear el hilo principal
        new Thread(() -> {
            AsyncHttpClient client = new DefaultAsyncHttpClient();
            try {
                // Realizamos la solicitud a la API
                client.prepare("GET", "https://imdb-com.p.rapidapi.com/title/get-top-meter?topMeterTitlesType=ALL")
                        .setHeader("x-rapidapi-key", IMDBApiClient.getApiKey())
                        .setHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                        .execute()
                        .toCompletableFuture()
                        .thenAccept(response -> {
                            if (response.getStatusCode() == 200) {
                                try {
                                    // Parseamos la respuesta del JSON
                                    String responseBody = response.getResponseBody();
                                    parseTopMovies(responseBody);
                                } catch (Exception e) {
                                    Log.e("API_ERROR", "Error al parsear la respuesta: " + e.getMessage());
                                }
                            } else if (response.getStatusCode() == 429) { // Límite alcanzado
                              //  Log.e("API_ERROR", "Límite de solicitudes alcanzado. Cambiando API Key.");
                                IMDBApiClient.switchApiKey(); // Cambiar a la siguiente clave
                                fetchTopMovies(); // Reintentar con la nueva clave
                            } else {
                                Log.e("API_ERROR", "Error en la respuesta: " + response.getStatusCode());
                            }
                        })
                        .join();
            } catch (Exception e) {
                Log.e("API_ERROR", "Error en la llamada: " + e.getMessage());
            } finally {
                try {
                    client.close(); // Cerrar el cliente
                } catch (IOException e) {
                    Log.e("API_ERROR", "Error al cerrar el cliente: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * parseamos los detalles del top 10 de peliculas desde la respuesta del api.
     * @param responseBody respuesta json del api
     */
    private void parseTopMovies(String responseBody) {
        try {
            // Parseamos el  JSON
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray edges = jsonResponse.getJSONObject("data")
                    .getJSONObject("topMeterTitles")
                    .getJSONArray("edges");

            // Creamos una lista de películas
            List<Movies> tempMovieList = new ArrayList<>();
            // limitamos el numero de peliculas a 10
            int maxResults = Math.min(edges.length(), 10);
            // Recorremos los resultados y creamos objetos de peliculas
            for (int i = 0; i < maxResults; i++) {
                JSONObject node = edges.getJSONObject(i).getJSONObject("node");

                Movies movie = new Movies();
                // Asignamos los valores a los atributos de la pelicula
                movie.setId(node.optString("id", "N/A"));
                movie.setTitle(
                        node.getJSONObject("titleText").optString("text", "Título no disponible")
                );
                movie.setImageUrl(
                        node.has("primaryImage") && node.getJSONObject("primaryImage").has("url")
                                ? node.getJSONObject("primaryImage").getString("url")
                                : "" // URL por defecto vacía si no hay imagen
                );
                // Obtenemos la fecha completa (día/mes/año)
                if (node.has("releaseDate")) {
                    JSONObject releaseDate = node.getJSONObject("releaseDate");
                    String day = releaseDate.has("day") ? String.valueOf(releaseDate.getInt("day")) : null;
                    String month = releaseDate.has("month") ? String.valueOf(releaseDate.getInt("month")) : null;
                    String year = releaseDate.has("year") ? String.valueOf(releaseDate.getInt("year")) : null;

                    // Concatenamos solo si los valores estan disponibles
                    if (day != null && month != null && year != null) {
                        movie.setReleaseYear(day + "/" + month + "/" + year);
                    } else {
                        // Dejamos null si falta algún dato
                        movie.setReleaseYear(null);
                    }
                } else {
                    movie.setReleaseYear(null);
                }

                // Verificamos si el campo rating existe antes de asignarlo

                if (node.has("ratingsSummary") && node.getJSONObject("ratingsSummary").has("aggregateRating")) {
                    String rating = node.getJSONObject("ratingsSummary").getString("aggregateRating");
                    movie.setRating(rating);
                    Log.d("MovieRating", "Película: " + movie.getTitle() + ", Rating: " + rating);
                } else {
                    // Dejamos null si no hay rating
                    movie.setRating(null);
                    Log.d("MovieRating", "Película: " + movie.getTitle() + ", Rating no disponible");
                }

                // Obtenemos el overview (descripción de la pelicula)
                if (node.has("plot") && node.getJSONObject("plot").has("plotText")) {
                    String overview = node.getJSONObject("plot").getJSONObject("plotText").getString("plainText");
                    movie.setOverview(overview);
                    Log.d("MovieOverview", "Película: " + movie.getTitle() + ", Overview: " + overview);
                } else {
                    // Dejamos null si no hay overview
                    movie.setOverview(null);
                    Log.d("MovieOverview", "Película: " + movie.getTitle() + ", Overview no disponible");
                }
                // Agregamos la pelicula a la lista
                tempMovieList.add(movie);
            }

            // Actualizamos la lista en el hilo principal
            requireActivity().runOnUiThread(() -> {
                movieList.clear();
                movieList.addAll(tempMovieList);
                // Notificamos los  cambios al RecyclerView
                adapter.notifyDataSetChanged();
            });
        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Error al parsear JSON: " + e.getMessage());
        }
    }

    /**
     * Metodo para obtener detalles de la película seleccionada.
     * @param movie pelicula seleccionada
     */
  private void fetchMovieOverview(Movies movie) {

      Retrofit retrofit = new Retrofit.Builder()
              .baseUrl("https://imdb-com.p.rapidapi.com/")
              .addConverterFactory(GsonConverterFactory.create())
              .build();

      IMDBApiService apiService = retrofit.create(IMDBApiService.class);

      Call<MovieOverviewResponse> call = apiService.getMovieOverview(
              movie.getId(),
              IMDBApiClient.getApiKey(), // Clave API
              "imdb-com.p.rapidapi.com"
      );

      call.enqueue(new Callback<MovieOverviewResponse>() {
          @Override
          public void onResponse(@NonNull Call<MovieOverviewResponse> call, @NonNull Response<MovieOverviewResponse> response) {
              if (response.isSuccessful() && response.body() != null) {
                  MovieOverviewResponse details = response.body();

                  // Actualizamos los detalles del objeto Movies
                  movie.setOverview(details.getData().getTitle().getPlot().getPlotText().getPlainText());
                  movie.setRating(details.getData().getTitle().getRatingsSummary().getAggregateRating() != null ?
                          String.valueOf(details.getData().getTitle().getRatingsSummary().getAggregateRating()) :
                          "No disponible");
                  MovieOverviewResponse.ReleaseDate releaseDate = details.getData().getTitle().getReleaseDate();
                  movie.setReleaseYear(releaseDate != null ?
                          releaseDate.getDay() + "/" + releaseDate.getMonth() + "/" + releaseDate.getYear() :
                          "Fecha no disponible");


                  // Iniciamos la actividad con los detalles completos de la pelicula
                  Intent intent = new Intent(getContext(), MovieDetailsActivity.class);
                  intent.putExtra("movie", movie); // Pasar el objeto actualizado
                  startActivity(intent);

              } else if (response.code() == 429) { // Límite alcanzado
                  Log.e("API_ERROR", "Límite de solicitudes alcanzado. Cambiando API Key.");
                  IMDBApiClient.switchApiKey(); // Cambiar a la siguiente clave
                  fetchMovieOverview(movie); // Reintentar con la nueva clave
              } else {
                  Toast.makeText(getContext(), "No se pudieron cargar los detalles de la película", Toast.LENGTH_SHORT).show();
              }
          }

          @Override
          public void onFailure(@NonNull Call<MovieOverviewResponse> call, @NonNull Throwable t) {
              Toast.makeText(getContext(), "Error al conectar con el servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
          }
      });
            }

    /**
     * Metodo para manejar el clic en una pelicula. Abre la actividad de detalles.
     * @param movie  película seleccionada
     */
    private void onMovieClick(Movies movie) {
        if (movie.getId() != null) {
            // Llamar al metodo que obtiene los detalles del endpoint
            fetchMovieOverview(movie);
        } else {
            Toast.makeText(getContext(), "Información incompleta para esta película", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * obtenemos los detalles completos de una pelicula antes de agregarla a favoritos.
     * @param movie pelicula seleccionada
     */
    private void fetchMovieOverviewForFavorites(Movies movie) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://imdb-com.p.rapidapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        IMDBApiService apiService = retrofit.create(IMDBApiService.class);

        Call<MovieOverviewResponse> call = apiService.getMovieOverview(
                movie.getId(),
                IMDBApiClient.getApiKey(), // Clave API
                "imdb-com.p.rapidapi.com"
        );

        call.enqueue(new Callback<MovieOverviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieOverviewResponse> call, @NonNull Response<MovieOverviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MovieOverviewResponse details = response.body();

                    // Actualizamos los datos de la pelcula
                    movie.setOverview(details.getData().getTitle().getPlot().getPlotText().getPlainText());
                    movie.setRating(details.getData().getTitle().getRatingsSummary().getAggregateRating() != null ?
                            String.valueOf(details.getData().getTitle().getRatingsSummary().getAggregateRating()) :
                            "No disponible");
                    MovieOverviewResponse.ReleaseDate releaseDate = details.getData().getTitle().getReleaseDate();
                    movie.setReleaseYear(releaseDate != null ?
                            releaseDate.getDay() + "/" + releaseDate.getMonth() + "/" + releaseDate.getYear() :
                            "Fecha no disponible");

                    // Agregamos la pelicula a favoritos
                    addMovieToFavorites(movie);
                    // Iniciamos la actividad con los detalles completos de la pelicula seleccionada en favoritos
                } else if (response.code() == 429) { // Límite alcanzado
                    Log.e("API_ERROR", "Límite de solicitudes alcanzado. Cambiando API Key.");
                    IMDBApiClient.switchApiKey(); // Cambiar a la siguiente clave
                    fetchMovieOverviewForFavorites(movie); // Reintentar con la nueva clave
                } else {
                    Toast.makeText(getContext(), "No se pudieron cargar los detalles de la película", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieOverviewResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error al conectar con el servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * agregamos una pelicula a la lista de favoritos.
     * @param movie pelicula seleccionada
     */
    private void addMovieToFavorites(Movies movie) {
        // Inicializamos el gestor de favoritos
        FavoritesManager favoritesManager = new FavoritesManager(requireContext());

        // Obtenemos el correo del usuario actual desde SharedPreferences y lo validamos
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("userEmail", ""); // Obtiene el correo del usuario
        String userId = prefs.getString("userId", "");
        Log.d("DebugUserId", "userId recuperado: " + userId);

        if (userEmail.isEmpty()) {
            Toast.makeText(getContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cargamos la lista de favoritos actual desde la base de datos
        Cursor cursor = favoritesManager.getFavoritesCursor(userEmail);
        // Obtenemos la lista de peliculas desde el cursor
        List<Movies> existingFavorites = favoritesManager.getFavoritesList(cursor);

        // Verificamos si se ha alcanzado el límite de peliculas
        if (existingFavorites.size() >= 6) {
            Toast.makeText(getContext(), "No puedes añadir más de 6 películas a favoritos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificamos si la pelicula ya esta en favoritos y evitar duplicidades
        for (Movies existingMovie : existingFavorites) {
            if (existingMovie.getId().equals(movie.getId())) {
                Toast.makeText(getContext(), "Esta película ya está en favoritos", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Nos aseguramos  que el rating y overview no sean nulos
        if (movie.getRating() == null || movie.getRating().isEmpty()) {
            movie.setRating("No disponible");
        }
        if (movie.getOverview() == null || movie.getOverview().isEmpty()) {
            movie.setOverview("Descripción no disponible");
        }

        // Agregamos la pelicula a favoritos
        boolean isAdded = favoritesManager.addFavorite(
                movie.getId(),              // ID de la pelicula
                userEmail,                  // Email del usuario
                movie.getTitle(),           // Titulo de la pelicula
                movie.getImageUrl(),        // URL de la imagen
                movie.getReleaseYear(),     // Fecha de lanzamiento
                movie.getRating(),          // Puntuación
                movie.getOverview(),        // Descripcion de la pelicula
                userId
        );

        if (isAdded) {
            Toast.makeText(getContext(), "Película añadida a favoritos: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error al añadir a favoritos", Toast.LENGTH_SHORT).show();
        }
    }



    /**
     * Metodo para manejar el clic largo al agregar una pelicula a favoritos.
     */
    public void onMovieLongClick(Movies movie) {

        if (movie.getRating() == null || movie.getOverview() == null) {
            // Obtenemos los detalles completos de la pelicula antes de agregarla
            fetchMovieOverviewForFavorites( movie);
        } else {
            // Agregar la pelicula directamente si ya tiene todos los detalles
            addMovieToFavorites(movie);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Evitamos cargar datos si ya estan cargados
        if (movieList.isEmpty()) {
            fetchTopMovies();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiamos la referencia al binding para evitar fugas de memoria
        binding = null;
    }
}