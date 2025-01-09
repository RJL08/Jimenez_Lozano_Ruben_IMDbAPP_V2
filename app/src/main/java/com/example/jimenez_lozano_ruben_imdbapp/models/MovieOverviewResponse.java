package com.example.jimenez_lozano_ruben_imdbapp.models;

import com.google.gson.annotations.SerializedName;


/**
 * Clase que representa la respuesta detallada de una pelucula desde la API.
 * Incluye informacion como fecha de lanzamiento, puntuacion, y trama.
 */
public class MovieOverviewResponse {
    @SerializedName("data")
    public Data data;

    public Data getData() {
        return data;
    }
    /**
     * Clase interna que contiene los detalles principales de la pelicula.
     */
    public static class Data {
        @SerializedName("title")
        public Title title;

        public Title getTitle() {
            return title;
        }
    }
    /**
     * Clase interna que representa el titulo y detalles relacionados.
     */
    public static class Title {
        @SerializedName("releaseDate")
        public ReleaseDate releaseDate;

        @SerializedName("ratingsSummary")
        public RatingsSummary ratingsSummary;

        @SerializedName("plot")
        public Plot plot;

        public ReleaseDate getReleaseDate() {
            return releaseDate;
        }

        public RatingsSummary getRatingsSummary() {
            return ratingsSummary;
        }

        public Plot getPlot() {
            return plot;
        }
    }
    /**
     * Clase interna que representa la fecha de lanzamiento de la pelicula.
     */
    public static class ReleaseDate {
        @SerializedName("day")
        public Integer day;

        @SerializedName("month")
        public Integer month;

        @SerializedName("year")
        public Integer year;

        public Integer getDay() {
            return day;
        }

        public Integer getMonth() {
            return month;
        }

        public Integer getYear() {
            return year;
        }
    }

    /**
     * Clase interna que representa el resumen de puntuacion de la pelicula.
     */
    public static class RatingsSummary {
        @SerializedName("aggregateRating")
        public Double aggregateRating;

        public Double getAggregateRating() {
            return aggregateRating;
        }
    }
    /**
     * Clase interna que representa la trama de la pelicula.
     */
    public static class Plot {
        @SerializedName("plotText")
        public PlotText plotText;

        public PlotText getPlotText() {
            return plotText;
        }
    }
    /**
     * Clase interna que representa el texto de la trama de la pelicula.
     */
    public static class PlotText {
        @SerializedName("plainText")
        public String plainText;

        public String getPlainText() {
            return plainText;
        }
    }
}