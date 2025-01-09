package com.example.jimenez_lozano_ruben_imdbapp.models;



import com.google.gson.annotations.SerializedName;
import java.util.List;


/**
 * Clase que representa la respuesta de búsqueda de películas desde la API de TMDB.
 * Contiene información sobre la pagina actual, los resultados, y el total de paginas y resultados.
 */
public class MovieSearchResponse {
    @SerializedName("page")
    private int page;

    @SerializedName("results")
    private List<TMDBMovie> results;

    @SerializedName("total_pages")
    private int totalPages;

    @SerializedName("total_results")
    private int totalResults;



    // Constructor sin parametros
    public MovieSearchResponse() {
    }

    // Getters y Setters
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<TMDBMovie> getResults() {
        return results;
    }

    public void setResults(List<TMDBMovie> results) {
        this.results = results;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    @Override
    public String toString() {
        return "MovieSearchResponse{" +
                "page=" + page +
                ", results=" + results +
                ", totalResults=" + totalResults +
                ", totalPages=" + totalPages +
                '}';
    }
}