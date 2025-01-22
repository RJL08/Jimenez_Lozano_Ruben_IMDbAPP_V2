package com.example.jimenez_lozano_ruben_imdbapp.ui.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesManager;
import com.example.jimenez_lozano_ruben_imdbapp.databinding.FragmentGalleryBinding;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import com.example.jimenez_lozano_ruben_imdbapp.ui.adapter.FavoritesAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;




public class GalleryFragment extends Fragment {
    //Declaramos las variables
    private FragmentGalleryBinding binding; // Para usar ViewBinding
    private RecyclerView recyclerView; // RecyclerView para mostrar los favoritos
    private FavoritesAdapter adapter; // Adaptador personalizado
    private List<Movies> favoriteList = new ArrayList<>(); // Lista de peliculas de favoritas
    private FavoritesManager favoritesManager; // Gestor de favoritos



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inicializamos el ViewModel
        GalleryViewModel galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

        // Inflamos el diseño usando ViewBinding
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Configuramos el RecyclerView
        recyclerView = binding.recyclerViewFavorites;
        // Lista vertical de elementos
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FavoritesAdapter(requireContext(), favoriteList);
        // Vinculamos el adaptador al RecyclerView
        recyclerView.setAdapter(adapter);

        // Inicializamos el FavoritesManager
        favoritesManager = new FavoritesManager(requireContext());

        // Cargamos los favoritos desde la base de datos
        loadFavorites();


        // Configuramos el boton de compartir
        Button shareButton = binding.shareButton;
        shareButton.setOnClickListener(v -> {
            // Solicitamos los permisos antes de compartir
            requestBluetoothPermission();
            // Compartimos favoritos
            shareFavoritesAsJSON();
        });



        return root;
    }


    /**
     * lanzador para manejar los permisos relacionados con bluetooth.
     */
    private final ActivityResultLauncher<String[]> bluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean isBluetoothConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                Boolean isLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);

                if (isBluetoothConnectGranted != null && isBluetoothConnectGranted &&
                        isLocationGranted != null && isLocationGranted) {
                    Toast.makeText(getContext(), "Permisos de Bluetooth concedidos.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permisos de Bluetooth denegados.", Toast.LENGTH_SHORT).show();
                    showPermissionDeniedDialog();
                }
            });

    /**
     * mostramos un cuadro de dialogo cuando se niegan los permisos necesarios.
     */
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Permisos necesarios")
                .setMessage("Esta funcionalidad requiere acceso a Bluetooth y ubicación para compartir tus películas favoritas. Por favor, otorga los permisos desde la configuración de la aplicación.")
                .setPositiveButton("Configurar", (dialog, which) -> {
                    // Redirigimos a la configuración de la aplicacion
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    /**
     * solicitamos los permisos necesarios para usar bluetooth.
     */
    private void requestBluetoothPermission() {
            // Verificar si los permisos ya estan concedidos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Mostramos la alerta explicando la importancia de los permisos
                    showPermissionDeniedDialog();
                } else {
                    // Solicitamos los permisos normalmente
                    bluetoothPermissionLauncher.launch(new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    });
                }
            } else {
                Toast.makeText(getContext(), "No se requiere permiso en versiones anteriores.", Toast.LENGTH_SHORT).show();
            }
        }

    /**
     * Convierte la lista de favoritos a JSON y permite compartirla,
     * mostrandolo en cuadro de dialogo como JSON
     */
    private void shareFavoritesAsJSON() {
        if (favoriteList.isEmpty()) {
            Toast.makeText(getContext(), "No hay favoritos para compartir.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertimos la lista de favoritos a JSON
        JSONArray jsonArray = new JSONArray();
        // Recorremos la lista de favoritos
        for (Movies movie : favoriteList) {
            try {
                JSONObject jsonMovie = new JSONObject();
                jsonMovie.put("id", movie.getId());
                jsonMovie.put("overview", movie.getOverview() != null ? movie.getOverview() : ""); // Dejar vacío si es null
                jsonMovie.put("posterUrl", movie.getImageUrl() != null ? movie.getImageUrl() : ""); // Dejar vacío si es null
                jsonMovie.put("rating", movie.getRating() != null ? movie.getRating() : "0.0"); // Dejar "0.0" si es null
                jsonMovie.put("releaseDate", movie.getReleaseYear() != null ? movie.getReleaseYear() : ""); // Dejar vacío si es null
                jsonMovie.put("title", movie.getTitle() != null ? movie.getTitle() : "Sin título"); // Dejar "Sin título" si es null

                jsonArray.put(jsonMovie);
            } catch (JSONException e) {
                Log.e("GalleryFragment", "Error al crear JSON: " + e.getMessage());
            }
        }
        // Convertimos el JSONArray a una cadena
        String jsonString = jsonArray.toString();
        // Reemplazamos las barras diagonales con barras normales
        jsonString = jsonString.replace("\\/", "/");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Películas Favoritas en JSON")
                .setMessage(jsonString)
                .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();

    }





    /**
     * Metodo para cargar los favoritos desde la base de datos.
     */
    private void loadFavorites() {
        // Obtenemos el correo del usuario actual desde SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        //String userEmail = prefs.getString("userEmail", ""); // Obtiene el correo del usuario actual
        String userId = prefs.getString("userId", ""); // Obtener el userId del usuario************
        if (userId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            Log.e("GalleryFragment", "Error: Correo del usuario vacío");
            return;
        }

        // Cargamos favoritos del usuario
        Cursor cursor = favoritesManager.getFavoritesCursor(userId);
        // Si hay favoritos cargados los agregamos a la lista y notificamos al adaptador los cambios
        if (cursor != null && cursor.getCount() > 0) {
            favoriteList.clear();
            favoriteList.addAll(favoritesManager.getFavoritesList(cursor));
            adapter.notifyDataSetChanged(); // Actualizar el RecyclerView
            Log.d("GalleryFragment", "Favoritos cargados correctamente: " + favoriteList.size());
        } else {
            Log.d("GalleryFragment", "No hay favoritos para el usuario: " + userId);
            Toast.makeText(getContext(), "No tienes películas favoritas", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargamos favoritos al reanudar el fragmento
        loadFavorites();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Liberamos el binding al destruir la vista
        binding = null;
    }
}