package com.example.jimenez_lozano_ruben_imdbapp.models;

import java.util.List;

/**
 * Clase que representa la respuesta de generos obtenida desde la API.
 * Contiene una lista de generos disponibles.
 */
public class GenresResponse {

    //Declaramos la variable
    private List<Genre> genres;

    // Getters y Setters
    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }
}
