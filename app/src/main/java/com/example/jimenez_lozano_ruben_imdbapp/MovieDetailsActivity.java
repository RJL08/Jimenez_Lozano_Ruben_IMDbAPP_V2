package com.example.jimenez_lozano_ruben_imdbapp;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.jimenez_lozano_ruben_imdbapp.models.Movies;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;


/**
 * actividad para mostrar los detalles de una pelicula seleccionada
 * y proporcionar opciones como enviar mensajes sms con informacion de la pelicula.
 */
public class MovieDetailsActivity extends AppCompatActivity {

    // Declaracion de variables
    private ImageView imageMovie;
    private TextView titleMovie, releaseDate, rating, description;
    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private Movies movies;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        // Inicializamos vistas
        imageMovie = findViewById(R.id.image_movie);
        titleMovie = findViewById(R.id.title_movie);
        releaseDate = findViewById(R.id.release_date_view);
        rating = findViewById(R.id.rating_view);
        description = findViewById(R.id.description_view);



        // Configuramos el lanzador para elegir contacto
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        if (contactUri != null) {
                            retrievePhoneNumber(contactUri);
                        }
                    } else {
                        Toast.makeText(this, "No se seleccionó ningún contacto", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        // configuramos la barra de herramientas
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configuramos titulo y habilitar el boton de retroceso por si fuera necesario
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Detalles de la Película");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Obtenemos los datos desde el Intent de la actividad anterior
       Intent intent = getIntent();
        movies = intent.getParcelableExtra("movie");
        String movieId = intent.getStringExtra("movie_id");

        // Boton para enviar SMS con informacion de la pelicula seleccionada
        Button btnSendSms = findViewById(R.id.btn_send_sms);
        btnSendSms.setOnClickListener(v -> checkPermissionsAndSendSms());

        // Mostramos los detalles de la pelicula seleccionada
        if (movies != null) {
            // Mostramos detalles de la película obtenidos del Parcelable
            titleMovie.setText(movies.getTitle() != null && !movies.getTitle().isEmpty() ? movies.getTitle() : "Título no disponible");
            description.setText(movies.getOverview() != null && !movies.getOverview().isEmpty() ? movies.getOverview() : "Sin descripción");
            releaseDate.setText("Release Date: " + (movies.getReleaseYear() != null && !movies.getReleaseYear().isEmpty() ? movies.getReleaseYear() : "Fecha no disponible"));
            rating.setText("Rating: " + (movies.getRating() != null && !movies.getRating().equals("0") ? movies.getRating() : "No hay valoraciones disponibles"));
            //Uso de Glide para cargar la imagen de la pelicula
            Glide.with(this)
                    .load(movies.getImageUrl() != null && !movies.getImageUrl().isEmpty() ? movies.getImageUrl() : R.drawable.esperando)
                    .placeholder(R.drawable.esperando)
                    .into(imageMovie);
        } else if (movieId != null) {
            // Si solo tenemos el ID, obtener los detalles desde el servicio
            fetchMovieDetails(movieId);
        } else {
            Toast.makeText(this, "Error: No se encontró información de la película", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Finalizar la actividad y regresar a la anterior
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Método para obtener detalles de la película seleccionada.
     * utilizando la API de IMDB.
     * @param movieId id de la película a buscar
     */
    private void fetchMovieDetails(String movieId) {
        new Thread(() -> {
            // Llamada a la API de IMDB
            AsyncHttpClient client = new DefaultAsyncHttpClient();
            try {
                // Construir la URL de la API con el ID de la película proporcionado
                String url = "https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + movieId;
                client.prepare("GET", url)
                        .setHeader("x-rapidapi-key", "8c8a3cbdefmsh5b39dc7ade88a71p1ca1bdjsn245a12339ee4")
                        .setHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                        .execute()
                        .toCompletableFuture()
                        .thenAccept(response -> {
                            // Manejamos la respuesta de la API
                            if (response.getStatusCode() == 200) {
                                try {
                                    String responseBody = response.getResponseBody();
                                    parseMovieDetails(responseBody);
                                } catch (Exception e) {
                                    Log.e("API_ERROR", "Error al parsear detalles: " + e.getMessage());
                                }
                            } else {
                                Log.e("API_ERROR", "Error en la respuesta: " + response.getStatusCode());
                            }
                        })
                        // Manejamos errores en la llamada a la API
                        .join();
            } catch (Exception e) {
                Log.e("API_ERROR", "Error en la llamada: " + e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    Log.e("API_ERROR", "Error al cerrar el cliente: " + e.getMessage());
                }
            }
        }).start();
    }


    /**
     * parseamos los detalles de la película obtenidos del json de la API
     * @param responseBody cuerpo de la respuesta del json
     */
    private void parseMovieDetails(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);

            // Extraemos campos del JSON para mostrar en la UI de la app
            final String releaseDateText = json.optJSONObject("releaseDate") != null ?
                    json.optJSONObject("releaseDate").optString("year", "N/A") : "N/A";
            final String ratingText = json.optJSONObject("ratings") != null ?
                    json.optJSONObject("ratings").optString("rating", "No disponible") : "No disponible";
            final String descriptionText = json.optJSONObject("plot") != null ?
                    json.optJSONObject("plot").optString("text", "Sin descripción") : "Sin descripción";

            // Actualizamos la UI en el hilo principal
            runOnUiThread(() -> {
                releaseDate.setText("Release Date: " + releaseDateText);
                rating.setText("Rating: " + ratingText);
                description.setText(descriptionText);
            });
        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Error al parsear JSON: " + e.getMessage());
        }
    }

    /**
     * verificamos y solicitamos los permisos necesarios para enviar sms.
     *
     */
    private void checkPermissionsAndSendSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Solicitamos los  permisos necesarios para enviar sms si no se han otorgado
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS}, 100);
        } else {
            // Continuamos con el proceso de envío
            chooseContactAndSendSms();
        }
    }

    // Manejamos la respuesta de los permisos solicitados en el lanzador de actividad anterior
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                chooseContactAndSendSms();
            } else {
                Toast.makeText(this, "Permisos denegados. No se puede enviar SMS.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Metodo para elegir contacto y enviar SMS.
     */
    private void chooseContactAndSendSms() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        // Usa el lanzador en lugar de startActivityForResult
        contactPickerLauncher.launch(intent);
    }

    /**
     * obtenemos el número de telefono del contacto seleccionado.
     *
     * @param contactUri uri del contacto seleccionado
     */
    private void retrievePhoneNumber(Uri contactUri) {
        String phoneNumber = null;

        // Consultamos el contenido del contacto seleccionado
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        // Si el cursor no está vacío, obtenemos el número de teléfono
        if (cursor != null && cursor.moveToFirst()) {
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            phoneNumber = cursor.getString(numberIndex);
            cursor.close();
        }
        // Si el número de telefono no es nulo, abrimos la aplicación de mensajerias con el contacto seleccionada
        if (phoneNumber != null) {
            openMessagingApp(phoneNumber);
        } else {
            Toast.makeText(this, "El contacto no tiene número de teléfono", Toast.LENGTH_SHORT).show();
        }
    }


    // Manejamos la respuesta del lanzador de la actividad anterior
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();

            // Consultar el número de teléfono del contacto seleccionado
            Cursor cursor = getContentResolver().query(contactUri,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phoneNumber = cursor.getString(numberIndex);
                cursor.close();

                // Abrir la aplicación de mensajería con el contacto seleccionado
                openMessagingApp(phoneNumber);
            } else {
                Toast.makeText(this, "No se pudo obtener el número del contacto", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * abrimos la aplicación de mensajería con el contacto seleccionado.
     *
     * @param phoneNumber número de teléfono del contacto
     */
    private void openMessagingApp(String phoneNumber) {
        // Obtenemos los datos de la pelicula seleccionada
        String movieTitle = titleMovie.getText().toString();
        String movieRating = rating.getText().toString();

        // Creamos el mensaje para enviar
        String message = "Esta película te gustará: " + movieTitle + "\n" + movieRating;

        // Creamos un Intent para abrir la aplicacion de mensajes
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
        smsIntent.putExtra("sms_body", message);

        try {
            // Abrimos la aplicación de mensajes
            startActivity(smsIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró una aplicación de mensajería", Toast.LENGTH_SHORT).show();
        }
    }


}
