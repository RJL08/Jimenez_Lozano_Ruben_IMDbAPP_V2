package com.example.jimenez_lozano_ruben_imdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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
        ContentValues values = new ContentValues();
        values.put(UserDataBaseHelper.COLUMN_USER_ID, userId);
        values.put(UserDataBaseHelper.COLUMN_NAME, name);
        values.put(UserDataBaseHelper.COLUMN_EMAIL, email);
        values.put(UserDataBaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(UserDataBaseHelper.COLUMN_IMAGE, image);

        long result = db.insert(UserDataBaseHelper.TABLE_NAME, null, values);
        db.close();
        return result != -1;
    }
}
