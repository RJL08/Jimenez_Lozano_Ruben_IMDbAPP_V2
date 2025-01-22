package com.example.jimenez_lozano_ruben_imdbapp.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.jimenez_lozano_ruben_imdbapp.MovieDetailsActivity;
import com.example.jimenez_lozano_ruben_imdbapp.R;
import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesManager;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import java.util.List;



/**
 * Adaptador para manejar la lista de películas favoritas en un RecyclerView.
 * Permite visualizar, gestionar y eliminar películas de favoritos.
 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    //Declaramos las variables
    private List<Movies> favoriteList;
    private Context context;


    /**
     * Constructor para inicializar la lista de favortios y los listeners..
     */
    public FavoritesAdapter(Context context, List<Movies> favoriteList) {
        this.context = context;
        this.favoriteList = favoriteList;
    }


    /**
     * Metodo para crear un nuevo ViewHolder para un elemento de la lista.
     * @param parent El ViewGroup al que se añadira la nueva vista.
     * @param viewType El tipo de vista (no utilizado en este caso).
     * @return Una nueva instancia de FavoriteViewHolder.
     */
    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    /**
     * Metodo con el que enlazamos los datos de una pelicula con el ViewHolder correspondiente.
     * @param holder El ViewHolder que se actualizara con los datos de la pelicula.
     * @param position La posicion del elemento en la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Movies movie = favoriteList.get(position);

        // Vinculamos los datos de la película al ViewHolder
        holder.bind(movie);

        // Clic corto para abrir los detalles de la película
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailsActivity.class);
            intent.putExtra("movie", movie); // Pasamos el objeto Movie como Parcelable
            context.startActivity(intent);
        });

        // Manejamos el  clic largo para eliminar de favoritos
        holder.itemView.setOnLongClickListener(v -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                builder.setTitle("Eliminar de Favoritos");
                builder.setMessage("¿Estás seguro de que quieres eliminar " + movie.getTitle() + " de favoritos?");
                builder.setPositiveButton("Sí", (dialog, which) -> removeMovie(position, movie));
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                builder.show();
            } catch (Exception e) {
                Toast.makeText(context, "Error inesperado al manejar clic largo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("FavoritesAdapter", "Error en onLongClick: " + e.getMessage());
            }
            // Indicamos que se manejo el clic largo
            return true;
        });
    }

    /**
     * Nos devuelve el numero total de elementos en la lista de favoritos.
     *
     * @return El tamaño de la lista de favoritos.
     */
    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    /**
     *Metodo con el que podemos eliminar una pelicula de favoritos y actualiza la lista.
     *
     * @param position La posición del elemento en la lista.
     * @param movie    La película a eliminar.
     */
    private void removeMovie(int position, Movies movie) {

        try {
            // Validamos el índice para ver si sigue siendo valido a la hora de borrar
            if (position < 0 || position >= favoriteList.size()) {
                Toast.makeText(context, "Error: índice inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Creamos una instancia de FavoritesManager para gestionar favoritos
            FavoritesManager favoritesManager = new FavoritesManager(context);
            SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            String userEmail = prefs.getString("userEmail", "");
            String userId = prefs.getString("userId", "");///*********************

            if (userId.isEmpty()) {
                Toast.makeText(context, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Evitamos duplicados: Comprobando si la pelicula aún esta en favoritos
            if (!favoriteList.contains(movie)) {
                Toast.makeText(context, "La película ya no está en favoritos.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Eliminamos de la base de datos en un hilo secundario para no bloquear la interfaz
            new Thread(() -> {
                boolean isRemoved = false;
                try {
                    isRemoved = favoritesManager.removeFavorite(userId, movie.getTitle());
                } catch (SQLiteException e) {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Error en la base de datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("FavoritesAdapter", "SQLiteException: " + e.getMessage());
                    });
                }

                if (isRemoved) {
                    // Actualizamos la lista en el hilo principal
                    ((Activity) context).runOnUiThread(() -> {
                        synchronized (favoriteList) {
                            favoriteList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, favoriteList.size()); // Ajustar posiciones restantes
                        }
                        Toast.makeText(context, movie.getTitle() + " eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Error al eliminar de favoritos", Toast.LENGTH_SHORT).show();
                    });
                }
                // Actualizamos la lista en el hilo principal
            }).start();

        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(context, "Error: índice fuera de rango. " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("FavoritesAdapter", "IndexOutOfBoundsException: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(context, "Error inesperado: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("FavoritesAdapter", "Unexpected Exception: " + e.getMessage());
        }
    }

    /**
     * ViewHolder para representar cada película favorita en el RecyclerView.
     */
    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private TextView movieTitle;
        private ImageView movieImage;

        /**
         * Constructor que inicializa las vistas del ViewHolder.
         *
         * @param itemView La vista del elemento en el RecyclerView.
         */
        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            movieTitle = itemView.findViewById(R.id.movie_title);
            movieImage = itemView.findViewById(R.id.movie_image);
        }

        /**
         * Vincula los datos de la pelicula a las vistas del ViewHolder.
         *
         * @param movie La película que se mostrará.
         */
        public void bind(Movies movie) {
            movieTitle.setText(movie.getTitle());
            Glide.with(itemView.getContext())
                    .load(movie.getImageUrl())
                    .placeholder(R.drawable.esperando)
                    .into(movieImage);
        }
    }
}
