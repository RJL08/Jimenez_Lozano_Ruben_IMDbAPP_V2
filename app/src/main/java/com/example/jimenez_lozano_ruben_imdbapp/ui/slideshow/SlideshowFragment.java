package com.example.jimenez_lozano_ruben_imdbapp.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.example.jimenez_lozano_ruben_imdbapp.MovieListActivity;
import com.example.jimenez_lozano_ruben_imdbapp.api.TMDBApiService;
import com.example.jimenez_lozano_ruben_imdbapp.databinding.FragmentSlideshowBinding;
import com.example.jimenez_lozano_ruben_imdbapp.models.Genre;
import com.example.jimenez_lozano_ruben_imdbapp.models.GenresResponse;
import com.example.jimenez_lozano_ruben_imdbapp.models.MovieSearchResponse;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import com.example.jimenez_lozano_ruben_imdbapp.models.TMDBMovie;
import com.example.jimenez_lozano_ruben_imdbapp.ui.adapter.MovieAdapter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Fragmento que permite al usuario buscar peliculas por genero y año.
 *
 */
public class SlideshowFragment extends Fragment {

    //Declaracion de variables
    private FragmentSlideshowBinding binding;
    private TMDBApiService apiService;
    private Spinner genreSpinner;
    private EditText yearEditText;
    private MovieAdapter moviesAdapter;
    private List<Movies> moviesList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializamos vistas
        genreSpinner = binding.genreSpinner;
        yearEditText = binding.yearEditText;
        Button searchButton = binding.searchButton;

        // Configuramos Retrofit y API Service
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.themoviedb.org/3/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(TMDBApiService.class);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al inicializar la API Service", Toast.LENGTH_SHORT).show();
        }

        // Limitamos la entrada del año a 4 digitos
        yearEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        // Configuramos acción del botón "Buscar"
        binding.searchButton.setOnClickListener(v -> {
            try {

                String selectedYear = binding.yearEditText.getText().toString().trim();
                Genre selectedGenre = (Genre) binding.genreSpinner.getSelectedItem();

                // Validamos que el año no este vacio y sea mayor o igual a 1940
                if (selectedYear.isEmpty()) {
                    Toast.makeText(getContext(), "Por favor introduce un año", Toast.LENGTH_SHORT).show();
                    return;
                }

                int year;
                try {
                    year = Integer.parseInt(selectedYear);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Por favor introduce un año válido", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Obtenemos el año actual para poder controlar que no se introduzca un año mayor al actual
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                if (year < 1940) {
                    Toast.makeText(getContext(), "El año no puede ser menor a 1940", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (year > currentYear) {
                    Toast.makeText(getContext(), "El año no puede ser mayor al año actual: " + currentYear, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedGenre == null) {
                    Toast.makeText(getContext(), "Por favor selecciona un género", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Realizamos la búsqueda y mostrar resultados
                searchMovies();

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error al procesar la búsqueda", Toast.LENGTH_SHORT).show();
            }
        });

        // Cargamos géneros
        try {
            loadGenres();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al cargar géneros", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    /**
     * Metodo para cargar generos desde la API de TMDB y poblar el Spinner.
     * de esta manera cargamos los generos de las peliculas en el spinner
     */
    private void loadGenres() {
        try {
            apiService.getGenres("943816107bcfe2fe25de64f7d3ea2ec0", "en-US").enqueue(new Callback<GenresResponse>() {
                @Override
                public void onResponse(Call<GenresResponse> call, Response<GenresResponse> response) {
                    try {
                        // Si la respuesta es exitosa y contiene datos, poblamos el Spinner
                        if (response.isSuccessful() && response.body() != null) {
                            // Añadir géneros a el Spinner con los datos obtenidos
                            List<Genre> genres = response.body().getGenres();
                            // Creamos un adaptador para el Spinner con los generos
                            ArrayAdapter<Genre> adapter = new ArrayAdapter<>(
                                    getContext(),
                                    android.R.layout.simple_spinner_item,
                                    genres
                            );
                            // Establecemos el diseño de la lista desplegable
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            genreSpinner.setAdapter(adapter);
                        } else {
                            Toast.makeText(getContext(), "Error al cargar géneros", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar la respuesta de géneros", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GenresResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al iniciar la carga de géneros", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metodo para buscar películas basadas en genero y año,
     * a traves de la llamada a la api de TMDB
     */
    private void searchMovies() {
        try {
            Genre selectedGenre = (Genre) genreSpinner.getSelectedItem();
            String year = yearEditText.getText().toString().trim();

            apiService.searchMovies(
                    "943816107bcfe2fe25de64f7d3ea2ec0",
                    "en-US",
                    "popularity.desc",
                    false,
                    1,
                    Integer.parseInt(year),
                    String.valueOf(selectedGenre.getId())
            ).enqueue(new Callback<MovieSearchResponse>() {
                @Override
                public void onResponse(Call<MovieSearchResponse> call, Response<MovieSearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ArrayList<Movies> movies = new ArrayList<>();
                        for (TMDBMovie tmdbMovie : response.body().getResults()) {
                            Movies movie = new Movies();
                            movie.setId(String.valueOf(tmdbMovie.getId()));
                            movie.setTitle(tmdbMovie.getTitle());
                            movie.setImageUrl(tmdbMovie.getPosterUrl());
                            movie.setReleaseYear(tmdbMovie.getReleaseDate());
                            movie.setOverview(tmdbMovie.getOverview());
                            movie.setRating(tmdbMovie.getVoteAverage() != 0.0 ? String.format("%.1f", tmdbMovie.getVoteAverage()) : "N/A");
                            movies.add(movie);
                        }

                        // Enviamos los resultados a MovieListActivity po medio del intent
                        Intent intent = new Intent(getContext(), MovieListActivity.class);
                        intent.putParcelableArrayListExtra("movies_list", movies);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "Error al buscar películas", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<MovieSearchResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al iniciar la búsqueda de películas", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiamos el binding para evitar fugas de memoria al destruir el fragmento
        binding = null;
    }
}
