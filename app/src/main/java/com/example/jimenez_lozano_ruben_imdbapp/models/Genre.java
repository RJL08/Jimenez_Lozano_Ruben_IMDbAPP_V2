package com.example.jimenez_lozano_ruben_imdbapp.models;


/**
 * Clase que representa un género de película.
 * Contiene el identificador único y el nombre del género.
 */
public class Genre {

    //Declaramos los atributos
    private int id;
    private String name;

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Devuelve el nombre del genero  en texto.
     * Esto asegura que el Spinner muestre correctamente el nombre del genero.
     *
     * @return El nombre del genero como una cadena de texto.
     */
    @Override
    public String toString() {
        // Con esto aseguramos que el Spinner muestre el nombre del genero.
        return name;
    }
}
