package com.example.jimenez_lozano_ruben_imdbapp.ui.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.jimenez_lozano_ruben_imdbapp.R;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import java.util.List;


/**
 * Adaptador personalizado para gestionar y mostrar una lista de peliculas en el RecyclerView.
 * Permite manejar clics corrtos y clics largos en los elementos de la lista.
 */
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {


    //Delcarmos las variables
    private List<Movies> movieList;
    private OnMovieClickListener listener;
    private OnMovieLongClickListener longClickListener;

    /**
     * Constructor para iniciar la lista de peliculas y los listeners.
     *
     * @param movieList Lista de peliculas.
     * @param listener Listener para clics cortos.
     * @param longClickListener Listener para clics largos.
     */
    public MovieAdapter(List<Movies> movieList, OnMovieClickListener listener, OnMovieLongClickListener longClickListener) {
        this.movieList = movieList;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    /**
     * Actualiza la lista de películas en el adaptador y notifica los cambios.
     *
     * @param newMovieList La nueva lista de películas a mostrar.
     */
    public void updateMovies(List<Movies> newMovieList) {
        this.movieList.clear();
        this.movieList.addAll(newMovieList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño de cada elemento de la lista desde el archivo XML
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moive, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        // Obtenemos la pelicula actual de la lista
        Movies movie = movieList.get(position);
        // Enlazamos los datos de la pelicula con el ViewHolder
        holder.bind(movie, listener, longClickListener);
    }

    @Override
    public int getItemCount() {
        // Devolvemos el tamaño de la lista de peliculas
        return movieList.size();
    }

    /**
     * ViewHolder para representar cada película en el RecyclerView.
     */
    public static class MovieViewHolder extends RecyclerView.ViewHolder {

        //Declaramos las variables
        private ImageView movieImage;
        private TextView movieTitle;

        /**
         * Constructor del ViewHolder para iniciar las vistas.
         * @param itemView Vista del elemento del RecyclerView.
         */
        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inicializar las vistas del diseño
            movieImage = itemView.findViewById(R.id.movie_image);
            movieTitle = itemView.findViewById(R.id.movie_title);
        }

        /**
         * Metodo para enlazar los datos de la pelicula y los listeners a las vistas.
         * @param movie Pelicula a mostrar.
         * @param listener Listener para clics cortos.
         * @param longClickListener Listener para clics largos.
         */
        public void bind(Movies movie, OnMovieClickListener listener, OnMovieLongClickListener longClickListener) {
            // Establecemos el título de la pelicula
            movieTitle.setText(movie.getTitle() != null ? movie.getTitle() : "Título no disponible");


            // Cargamos la imagen de la pelicula usando Glide
            Glide.with(itemView.getContext())
                    .load(movie.getImageUrl())
                    .placeholder(R.drawable.esperando)
                    .into(movieImage);

            // Configuramos el listener para clics cortos
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovieClick(movie);
                }
            });


            // Configuramos el listener para clics largos
            itemView.setOnLongClickListener(v -> {
                longClickListener.onMovieLongClick(movie);
                return true; // Indicar que el evento fue manejado
            });
        }
    }

    /**
     * Interfaz para manejar clics en una película.
     */
    public interface OnMovieClickListener {
        void onMovieClick(Movies movie);
    }

    /**
     * Interfaz para manejar clics largos en una película.
     */
    public interface OnMovieLongClickListener {
        void onMovieLongClick(Movies movie);
    }
}