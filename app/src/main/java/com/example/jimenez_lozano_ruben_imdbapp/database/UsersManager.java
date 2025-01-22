package com.example.jimenez_lozano_ruben_imdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class UsersManager {

    private UserDataBaseHelper dbHelper;

    public UsersManager(Context context) {
        dbHelper = new UserDataBaseHelper(context);
    }

    /**
     * Inserta un nuevo usuario en la tabla users.
     *
     * @param userId     ID único del usuario.
     * @param name       Nombre del usuario.
     * @param email      Correo electrónico.
     * @param loginTime  Fecha/hora de inicio de sesión.
     * @param image      URL de la imagen del usuario.
     * @return True si la inserción fue exitosa, False de lo contrario.
     */
    public boolean addUser(String userId, String name, String email, String loginTime, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d("UsersManager", "Base de datos abierta: " + db.isOpen());
        ContentValues values = new ContentValues();
        values.put(UserDataBaseHelper.COLUMN_USER_ID, userId);
        values.put(UserDataBaseHelper.COLUMN_NAME, name);
        values.put(UserDataBaseHelper.COLUMN_EMAIL, email);
        values.put(UserDataBaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(UserDataBaseHelper.COLUMN_IMAGE, image);

        long result = db.insert(UserDataBaseHelper.TABLE_NAME, null, values);
        //db.close();
        Log.d("UsersManager", "Usuario insertado: " + (result != -1));
        return result != -1;
    }

    public boolean addOrUpdateUser(String userId, String name, String email, String loginTime, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Intentar actualizar primero
        ContentValues values = new ContentValues();
        values.put(UserDataBaseHelper.COLUMN_NAME, name);
        values.put(UserDataBaseHelper.COLUMN_EMAIL, email);
        values.put(UserDataBaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(UserDataBaseHelper.COLUMN_IMAGE, image);

        // Actualizamos el usuario si ya existe
        int rowsUpdated = db.update(
                UserDataBaseHelper.TABLE_NAME,
                values,
                UserDataBaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );

        if (rowsUpdated == 0) {
            // Si no se actualizó, insertamos el nuevo usuario
            values.put(UserDataBaseHelper.COLUMN_USER_ID, userId);
            long result = db.insert(UserDataBaseHelper.TABLE_NAME, null, values);
            //db.close();
            return result != -1;
        }

        //db.close();
        return true;
    }
}
