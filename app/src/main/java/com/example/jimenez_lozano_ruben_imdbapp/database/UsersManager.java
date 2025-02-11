package com.example.jimenez_lozano_ruben_imdbapp.database;



import android.content.ContentValues;
import android.content.Context;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.jimenez_lozano_ruben_imdbapp.sync.UsersSync;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Clase que gestiona las operaciones relacionadas con la tabla users.
 */
public class UsersManager {

    private FavoritesDatabaseHelper dbHelper;
    private Context context;


    public UsersManager(Context context) {
        this.context = context;
        dbHelper = new FavoritesDatabaseHelper(context);
    }



    /**
     * Registra (o inserta) al usuario en la base de datos local y sincroniza con Firestore.
     * Este método se utiliza al iniciar sesión por primera vez.
     *
     * @param userId    El ID único del usuario (Firebase UID).
     * @param name      El nombre del usuario.
     * @param email     El correo electrónico del usuario.
     * @param photoUrl  La URL de la foto de perfil (obtenida de Google o Facebook).
     * @param loginTime La hora actual formateada (por ejemplo, "yyyy-MM-dd HH:mm:ss").
     * @param address   Dirección (puede estar vacía al inicio).
     * @param phone     Teléfono (puede estar vacío al inicio).
     * @return true si se registró o actualizó correctamente, false en caso contrario.
     */
    public boolean registerUserOnSignIn(String userId, String name, String email, String photoUrl,
                                        String loginTime, String address, String phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Asignamos los valores a actualizar
        if (name != null && !name.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        }
        if (email != null) {
            values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        }
        if (photoUrl != null && !photoUrl.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, photoUrl);
        }
        if (address != null && !address.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        }
        if (phone != null && !phone.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        }
        if (loginTime != null) {
            values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        }

        // Intentamos actualizar la fila (en caso de que ya exista)
        int rowsUpdated = db.update(
                FavoritesDatabaseHelper.TABLE_USERS,
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );

        boolean success;
        if (rowsUpdated == 0) {
            // Si no se actualizó, insertamos un nuevo registro
            values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
            long result = db.insert(FavoritesDatabaseHelper.TABLE_USERS, null, values);
            success = (result != -1);
        } else {
            success = true;
        }
        db.close();

        // Sincronizamos la base de datos local con Firestore (esto actualizará también la nube)
        new UsersSync().syncLocalToFirestore(context, dbHelper);

        return success;
    }


    /**
     * Actualiza un usuario existente en la tabla users.
     * @param userId     ID único del usuario.
     * @param name      nombre del usuario.
     * @param email      Correo electrónico.
     * @param timeField  Campo de tiempo a actualizar.
     * @param timeValue  Valor del campo de tiempo.
     * @param image      URL de la imagen del usuario.
     * @return
     */
    public boolean addOrUpdateUser(String userId, String name, String email, String timeField, String timeValue,
                                   String image, String address, String phone) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Si el userId es nulo, no hacemos nada
        if (userId == null) {
            return false;
        }

        // Si el nombre es nulo, asignamos "No Name"
        if (name != null && name.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);

        }

        // Si el correo electrónico no es nulo, lo asignamos
        if (email != null) {
            values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        }

        // Si la imagen no es nula, la asignamos a los valores a actualizar
        if (image != null && !image.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);
        }

        // Si el campo es login_time, actualizamos el login_time
        if (timeField != null && timeField.equals(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME)) {
            if (timeValue != null) {
                values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, timeValue);
            }
        }

        // Si el campo es logout_time, actualizamos el logout_time. Si es null, ponemos un valor indicativo
        if (timeField != null && timeField.equals(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME)) {
            if (timeValue != null) {
                values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, timeValue);
            } else {
                // Si no se ha hecho logout, asignamos "No disconnected"
                values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, "No disconnected");
            }
        }

        if (address != null && !address.isEmpty()) {
            values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        }

        // Si el teléfono no es nulo o vacío, lo asignamos
        if (phone != null && !phone.equals("")) {
            values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        }

        // Actualizamos la fila si existe, si no la insertamos
        int rowsUpdated = db.update(
                FavoritesDatabaseHelper.TABLE_USERS,
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );

        if (rowsUpdated == 0) {
            // Si no se actualizó ninguna fila, intentamos insertar un nuevo usuario
            values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
            long result = db.insert(FavoritesDatabaseHelper.TABLE_USERS, null, values);
            db.close();

            if (result != -1) {
                // Si la inserción fue exitosa, sincronizamos con Firestore
                new UsersSync().syncLocalToFirestore(context, dbHelper);
            }
            return result != -1;
        }

        db.close();
        // Si ya existía el usuario, sincronizamos con Firestore
        new UsersSync().syncLocalToFirestore(context, dbHelper);
        return true;
    }






    public void updateLogoutTime(String userId, String logoutTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);

        int rowsUpdated = db.update(
                FavoritesDatabaseHelper.TABLE_USERS,
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );

        if (rowsUpdated > 0) {
            Log.d("UserDataBaseHelper", "Logout time actualizado para userId: " + userId);
        } else {
            Log.e("UserDataBaseHelper", "Error al actualizar logout time para userId: " + userId);
        }

        db.close();
    }

    /**
     * Obtiene los datos del usuario a partir de su ID de usuario.
     * @param userId ID único del usuario.
     * @return
     */
    public Cursor getUserData(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                FavoritesDatabaseHelper.COLUMN_NAME,
                FavoritesDatabaseHelper.COLUMN_EMAIL,
                FavoritesDatabaseHelper.COLUMN_ADDRESS,
                FavoritesDatabaseHelper.COLUMN_PHONE,
                FavoritesDatabaseHelper.COLUMN_IMAGE
        };

        String selection = FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = { userId };

        return db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
    }
}