package com.example.jimenez_lozano_ruben_imdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Clase que gestiona la creación y actualización de la base de datos de favoritos.
 * Proporciona el esquema de la base de datos y maneja su versión.
 */
    public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

        // Declaramos los atributos de la base de datos
        private static final String DATABASE_NAME = "favorites_db";
        private static final int DATABASE_VERSION = 4;
        public static final String TABLE_NAME = "favorites";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USER_EMAIL = "user_email";
        public static final String COLUMN_MOVIE_TITLE = "movie_title";
        public static final String COLUMN_MOVIE_IMAGE = "movie_image";
        public static final String COLUMN_RELEASE_DATE = "release_date";
        public static final String COLUMN_MOVIE_RATING = "movie_rating";
        public static final String COLUMN_MOVIE_OVERVIEW = "overview";

        // Constructor
        public FavoritesDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

    /**
     * Metodo llamado cuando se crea la base de datos por primera vez.
     * Define el esquema inicial de la tabla de favoritos.
     * @param db Instancia de la base de datos donde se ejecutara el SQL.
     */
        @Override
        public void onCreate(SQLiteDatabase db) {

                String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_USER_EMAIL + " TEXT NOT NULL, " +
                        COLUMN_MOVIE_TITLE + " TEXT NOT NULL, " +
                        COLUMN_MOVIE_IMAGE + " TEXT NOT NULL, " +
                        COLUMN_RELEASE_DATE + " TEXT NOT NULL, " +
                        COLUMN_MOVIE_RATING + " TEXT NOT NULL, " +
                        COLUMN_MOVIE_OVERVIEW + " TEXT);"; // Nueva columna para descripción
                db.execSQL(createTable);
        }


    /**
     * Metodo llamado cuando se actualiza la version de la base de datos.
     * Maneja los cambios de esquema eliminando y recreando la tabla.
     * @param db         Instancia de la base de datos donde se ejecutara el SQL.
     * @param oldVersion La version anterior de la base de datos.
     * @param newVersion La nueva version de la base de datos.
     */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Eliminamos la tabla existente
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            // Llamar al método onCreate para recrear la tabla con el nuevo esquema
            onCreate(db);

        }
    }
