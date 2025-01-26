package com.example.jimenez_lozano_ruben_imdbapp.database;

import android.content.ContentValues;
import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.jimenez_lozano_ruben_imdbapp.sync.UsersSync;

public class UsersManager {

    private UserDataBaseHelper dbHelper;
    private Context context;


    public UsersManager(Context context) {
        this.context = context;
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
        db.close();
        Log.d("UsersManager", "Usuario insertado: " + (result != -1));
        // Sincronizar con Firestore
        if (result != -1) {
            new UsersSync().syncLocalToFirestore(context, dbHelper);
        }
        return result != -1;
    }




    public boolean addOrUpdateUser(String userId, String name, String email, String  timeField, String timeValue, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserDataBaseHelper.COLUMN_NAME, name);
        values.put(UserDataBaseHelper.COLUMN_EMAIL, email);
        values.put(UserDataBaseHelper.COLUMN_IMAGE, image);

        if (timeField.equals(UserDataBaseHelper.COLUMN_LOGIN_TIME)) {
            values.put(UserDataBaseHelper.COLUMN_LOGIN_TIME, timeValue);
        } else if (timeField.equals(UserDataBaseHelper.COLUMN_LOGOUT_TIME)) {
            values.put(UserDataBaseHelper.COLUMN_LOGOUT_TIME, timeValue);
        }

        int rowsUpdated = db.update(
                UserDataBaseHelper.TABLE_NAME,
                values,
                UserDataBaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );

        if (rowsUpdated == 0) {
            values.put(UserDataBaseHelper.COLUMN_USER_ID, userId);
            long result = db.insert(UserDataBaseHelper.TABLE_NAME, null, values);
            db.close();

            if (result != -1) {
                new UsersSync().syncLocalToFirestore(context, dbHelper); // Sincronizar con Firestore
            }
            return result != -1;
        }

        db.close();
        new UsersSync().syncLocalToFirestore(context, dbHelper); // Sincronizar con Firestore
        return true;
    }

}
