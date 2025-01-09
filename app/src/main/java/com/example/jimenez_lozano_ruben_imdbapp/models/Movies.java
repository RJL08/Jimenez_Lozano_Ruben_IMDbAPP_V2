package com.example.jimenez_lozano_ruben_imdbapp.models;

import android.os.Parcel;
import android.os.Parcelable;



/**
 * Clase que representa una pelicula con atributos clave como titulo, puntuación y descripción.
 * Implementa Parcelable para facilitar la transferencia de objetos entre actividades.
 */
public class Movies implements Parcelable {
    // Declaramos los atributos
    private String id;          // ID de la película
    private String title;       // Título
    private String imageUrl;    // URL de la imagen
    private String releaseYear; // Año de lanzamiento
    private String rating;      // Puntuación de la pelicula
    private String overview;    // Resumen de la pelicula
    private String genreId;     // ID del genero para busquedas por genero

    // Constructor sin parametros
    public Movies() {
    }

    /**
     * Constructor que inicializa un objeto de pelicula a partir de un Parcel.
     * @param in El objeto Parcel desde el que se inicializan los atributos.
     */
    protected Movies(Parcel in) {
        id = in.readString();
        title = in.readString();
        imageUrl = in.readString();
        releaseYear = in.readString();
        rating = in.readString();
        overview = in.readString(); // Leer el nuevo campo 'overview'
        genreId = in.readString();  // Leer el nuevo campo 'genreId'
    }

    /**
     * CREATOR para la clase Movies, necesario para implementar el Parcelable.
     */
    public static final Creator<Movies> CREATOR = new Creator<Movies>() {
        @Override
        public Movies createFromParcel(Parcel in) {
            return new Movies(in);
        }

        @Override
        public Movies[] newArray(int size) {
            return new Movies[size];
        }
    };

    /**
     * Escribimos los datos de la pelicula en un Parcel.
     * @param dest  El Parcel donde se escribirán los datos.
     * @param flags Flags adicionales sobre cómo escribir el objeto.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(imageUrl);
        dest.writeString(releaseYear);
        dest.writeString(rating);
        dest.writeString(overview);
        dest.writeString(genreId);
    }

    /**
     * Describe el contenido del objeto Parcelable.
     * @return Un entero que representa el tipo de objeto.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    // Getters y Setters existentes
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }


    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }
}