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
    private static final int DATABASE_VERSION = 11;  // Mantén la versión en 6

    // Nombre de la tabla de favoritos
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_FAVORITE_ID = "id";
    public static final String COLUMN_USERS_ID = "user_id";  // Este es el campo que hace referencia al usuario
    public static final String COLUMN_USER_EMAIL = "user_email";
    public static final String COLUMN_MOVIE_TITLE = "movie_title";
    public static final String COLUMN_MOVIE_IMAGE = "movie_image";
    public static final String COLUMN_RELEASE_DATE = "release_date";
    public static final String COLUMN_MOVIE_RATING = "movie_rating";
    public static final String COLUMN_MOVIE_OVERVIEW = "overview";

    // Nombre de la tabla de usuarios
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_LOGIN_TIME = "login_time";
    public static final String COLUMN_LOGOUT_TIME = "logout_time";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_IMAGE = "image";

    // Constructor
    public FavoritesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Método llamado cuando se crea la base de datos por primera vez.
     * Define el esquema inicial de la tabla de favoritos y usuarios.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear la tabla de usuarios
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT , " +
                COLUMN_EMAIL + " TEXT NOT NULL, " +
                COLUMN_LOGIN_TIME + " TEXT, " +
                COLUMN_LOGOUT_TIME + " TEXT, " +
                COLUMN_ADDRESS + " TEXT, " +
                COLUMN_PHONE + " TEXT, " +
                COLUMN_IMAGE + " TEXT);";

        db.execSQL(createUsersTable);

        // Crear la tabla de favoritos
        String createFavoritesTable = "CREATE TABLE " + TABLE_FAVORITES + " (" +
                COLUMN_FAVORITE_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER_ID + " TEXT NOT NULL, " +
                COLUMN_USER_EMAIL + " TEXT NOT NULL, " +
                COLUMN_MOVIE_TITLE + " TEXT NOT NULL, " +
                COLUMN_MOVIE_IMAGE + " TEXT NOT NULL, " +
                COLUMN_RELEASE_DATE + " TEXT NOT NULL, " +
                COLUMN_MOVIE_RATING + " TEXT NOT NULL, " +
                COLUMN_MOVIE_OVERVIEW + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "));";

        db.execSQL(createFavoritesTable);
    }

    /**
     * Método llamado cuando se actualiza la versión de la base de datos.
     * Maneja los cambios de esquema eliminando y recreando las tablas.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Eliminar las tablas si existen
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Llamar al método onCreate para recrear las tablas con el nuevo esquema
        onCreate(db);
    }
    }
