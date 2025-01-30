package com.example.jimenez_lozano_ruben_imdbapp.utils;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.jimenez_lozano_ruben_imdbapp.database.UsersManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
     * Clase AppLifecycleManager.
     * Gestiona los eventos del ciclo de vida de la aplicación para realizar acciones
     * como registrar el tiempo de logout en la base de datos cuando el usuario está inactivo.
     */
    public class AppLifecycleManager implements Application.ActivityLifecycleCallbacks {
    private static final String PREF_NAME = "MyAppPrefs"; // Nombre de las SharedPreferences
    private static final String PREF_IS_LOGGED_IN = "isLoggedIn"; // Clave para el estado de sesión
    private static final long LOGOUT_DELAY = 3000; // Tiempo de espera antes de registrar el logout (en milisegundos)

    private boolean isInBackground = false; // Indica si la aplicación está en segundo plano
    private boolean isAppClosed = false; // Indica si la aplicación está completamente cerrada
    private int activityReferences = 0; // Cuenta el número de actividades activas
    private final Handler logoutHandler = new Handler(); // Handler para gestionar el logout retrasado
    private final Runnable logoutRunnable; // Runnable que ejecuta el logout
    private final Context context; // Contexto de la aplicación

    /**
     * Constructor que inicializa la clase AppLifecycleManager.
     *
     * @param context Contexto de la aplicación para acceder a recursos.
     */
    public AppLifecycleManager(Context context) {
        this.context = context;

        // Inicializar el Runnable que registra el logout tras un retraso
        logoutRunnable = () -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                registerUserLogout(currentUser);
                Log.d("AppLifecycleManager", "Logout registrado tras inactividad.");
            }
        };
    }

    /**
     * Registra el tiempo de logout del usuario en la base de datos y SharedPreferences.
     *
     * @param user Usuario actual autenticado en Firebase.
     */
    private void registerUserLogout(FirebaseUser user) {
        // Guardar el tiempo de logout en la base de datos local
        UsersManager usersManager = new UsersManager(context); // Usamos UsersManager
        String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        usersManager.updateLogoutTime(user.getUid(), logoutTime); // Actualizar logout en la base de datos

        // Actualizar el estado de sesión en SharedPreferences
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_IS_LOGGED_IN, false);
        editor.apply();

        Log.d("AppLifecycleManager", "Logout registrado en la base de datos y SharedPreferences.");
    }

    /**
     * Evento llamado cuando una actividad entra en primer plano.
     */
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        isInBackground = false; // La aplicación ya no está en segundo plano
        logoutHandler.removeCallbacks(logoutRunnable); // Cancelar logout retrasado

        // Actualizar estado de sesión en SharedPreferences
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_IS_LOGGED_IN, true);
        editor.apply();

        Log.d("AppLifecycleManager", "Usuario activo.");
    }

    /**
     * Evento llamado cuando una actividad entra en pausa.
     */
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        isInBackground = true; // La aplicación está en segundo plano
        logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY); // Programar logout
    }

    /**
     * Evento llamado cuando una actividad es iniciada.
     */
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!activity.isChangingConfigurations()) {
            activityReferences++; // Incrementar actividades activas
        }
    }

    /**
     * Evento llamado cuando una actividad es detenida.
     */
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (!activity.isChangingConfigurations()) {
            activityReferences--; // Decrementar actividades activas
            if (activityReferences == 0) {
                isAppClosed = true; // Marcar aplicación como cerrada
                logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY); // Programar logout
            }
        }
    }

    /**
     * Evento llamado cuando una actividad es destruida.
     */
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // Este método puede permanecer vacío
    }

    /**
     * Evento llamado cuando el sistema detecta memoria baja.
     */
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) { // Si la interfaz de usuario está oculta
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registerUserLogout(user); // Registrar logout
                Log.d("AppLifecycleManager", "Logout registrado al minimizar la aplicación.");
            }
        }
    }

    /**
     * Evento llamado cuando una actividad es creada.
     */
    @Override
    public void onActivityCreated(@NonNull Activity activity, android.os.Bundle savedInstanceState) {
        // Este método puede permanecer vacío
    }

    /**
     * Evento llamado para guardar el estado de una actividad.
     */
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull android.os.Bundle outState) {
        // Este método puede permanecer vacío
    }
}
