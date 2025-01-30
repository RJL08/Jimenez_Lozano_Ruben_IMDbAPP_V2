package com.example.jimenez_lozano_ruben_imdbapp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.example.jimenez_lozano_ruben_imdbapp.utils.AppLifecycleManager;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import androidx.navigation.NavController;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.jimenez_lozano_ruben_imdbapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


@SuppressWarnings("deprecation")




/**
 * Actividad principal que configura el drawer layout, navigation view,
 * y muestra la informacion del usuario autenticado.
 */
public class MainActivity extends AppCompatActivity {

    // Declaracion de variables
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private AppLifecycleManager appLifecycleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa el SDK de Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());

        //llamada a la clase AppLifecycleManager
        // Inicializar AppLifecycleManager
        appLifecycleManager = new AppLifecycleManager(this);

        // Registrar AppLifecycleManager para escuchar eventos del ciclo de vida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(appLifecycleManager);
        }

        // Inflamos el layout principal
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtenemos los datos del usuario pasados desde SigninActivity
        Intent intent = getIntent();
        String userName = intent.getStringExtra("user_name");
        String userEmail = intent.getStringExtra("user_email");
        String userPhotoUrl = intent.getStringExtra("user_photo");
        String providerId = intent.getStringExtra("provider");

        mostrarDatosUsuario(userName, userEmail, userPhotoUrl);

        // Configuramos el NavigationView
        NavigationView navigationView = binding.navView;
        // Accedemos al encabezado del NavigationView
        View headerView = navigationView.getHeaderView(0);
        LinearLayout headerLayout = headerView.findViewById(R.id.header_container);



        // Obtenemos la altura del notch (si existe)
        int statusBarHeight = 0;
        @SuppressLint("DiscouragedApi") int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // Aplicamos margen superior dinámico al LinearLayout del encabezado
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
        // Ajuste adicional
        params.topMargin = statusBarHeight + 32;
        headerLayout.setLayoutParams(params);

        // Inicializamos las vistas del encabezado (nav_header_main)
        ImageView profileImageView = headerView.findViewById(R.id.imageView);
        TextView nameTextView = headerView.findViewById(R.id.user_name);
        TextView emailTextView = headerView.findViewById(R.id.user_email);
        Button logoutButton = headerView.findViewById(R.id.logout_button);

        if (userName != null) {
            nameTextView.setText(userName);
        }
        if (userEmail != null) {
            emailTextView.setText(userEmail);
        }
        if (userPhotoUrl != null) {
            Picasso.get()
                    .load(userPhotoUrl)
                    .placeholder(R.drawable.ic_launcher_background) // Imagen por defecto
                    .error(R.drawable.ic_launcher_foreground) // Imagen de error
                    .into(profileImageView);
        }

        // Configuramos el boton de logout para cerrar sesion
        logoutButton.setOnClickListener(v -> {
            // Obtener el proveedor (Google o Facebook) desde SharedPreferences
            // Obtener el proveedor (Google o Facebook) desde SharedPreferences
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            String logoutProviderId = prefs.getString("provider", ""); // Renombrar la variable

            // Llamar al método realizarLogout con el proveedor correspondiente
            realizarLogout(logoutProviderId);
                    });

        // Configuramos la barra de herramientas
        setSupportActionBar(binding.appBarMain.toolbar);

        // Configuramos el DrawerLayout y Navigation Controller
        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        // Configuramos el Navigation Controller y el Navigation View para la navegacion entre fragmentos
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Manejamos el clic en el menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Navegamoos al fragmento Top 10
                navController.navigate(R.id.nav_home);
                // Cerramos el menú lateral
                drawer.closeDrawers();
                return true;
            } else if (id == R.id.nav_slideshow) {
                // Navegamos al fragmento Buscar Películas
                navController.navigate(R.id.nav_slideshow);
                drawer.closeDrawers(); // Cerrar el menú lateral
                return true;
            } else if (id == R.id.nav_gallery) {
                // Navegamos al fragmento Favoritos
                navController.navigate(R.id.nav_gallery);
                drawer.closeDrawers(); // Cerrar el menú lateral
                return true;
            }

            return false;
        });
    }

    /**
     * Recupera los datos del usuario desde SharedPreferences.
     */
    private void mostrarDatosUsuario(String userName, String userEmail, String userPhotoUrl) {
        // Configuramos el NavigationView
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        // Inicializamos las vistas del encabezado
        ImageView profileImageView = headerView.findViewById(R.id.imageView);
        TextView nameTextView = headerView.findViewById(R.id.user_name);
        TextView emailTextView = headerView.findViewById(R.id.user_email);

        // Mostrar nombre y correo
        nameTextView.setText(userName != null ? userName : "Nombre no disponible");
        emailTextView.setText(userEmail != null ? userEmail : "Correo no disponible");

        // Mostrar foto de perfil usando Picasso
        Picasso.get()
                .load(userPhotoUrl)
                .placeholder(R.drawable.ic_launcher_background) // Imagen por defecto
                .error(R.drawable.ic_launcher_foreground) // Imagen de error
                .into(profileImageView);
    }



    private void finalizarSesion() {

        // Obtener el user_id del usuario actual desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        // Obtener el tiempo de logout actual
        String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Actualizar la base de datos con el tiempo de logout
        if (!userId.isEmpty()) {
            updateLogoutTimeInDatabase(userId, logoutTime);
        }

        // Cerrar sesión de Firebase
        FirebaseAuth.getInstance().signOut();


        // Limpiar SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Regresar a la pantalla de inicio de sesión
        Intent intent = new Intent(MainActivity.this, SigninActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finalizamos la actividad actual
    }

    /**
     * Actualiza el tiempo de logout en la base de datos.
     * @param userId
     * @param logoutTime
     */
    private void updateLogoutTimeInDatabase(String userId, String logoutTime) {
        // Instancia del helper para la base de datos de usuarios (ahora en favorites_db)
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Crear valores para actualizar
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime); // Actualizamos logout_time en la tabla de usuarios

        // Actualizar el tiempo de logout para el user_id correspondiente en la nueva base de datos
        int rowsUpdated = db.update(
                FavoritesDatabaseHelper.TABLE_USERS, // Tabla de usuarios en la nueva base de datos
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );
        if (rowsUpdated > 0) {
            Log.d("Logout", "Logout time actualizado correctamente para user_id: " + userId);
        } else {
            Log.e("Logout", "Error al actualizar el logout time para user_id: " + userId);
        }

        // Cerrar la base de datos
        db.close();
    }




    /**
     * Realiza un logout efectivo basado en el proveedor de inicio de sesión.************
     */

        private void realizarLogout(String providerId) {
            if ("google.com".equals(providerId)) {
                // Obtener el userId desde SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String userId = prefs.getString("userId", "");
                String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Registrar el logout_time en la base de datos
                if (!userId.isEmpty()) {
                    updateLogoutTimeInDatabase(userId, logoutTime);
                }
                // Cerrar sesión de Google
                GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .revokeAccess()
                        .addOnCompleteListener(task -> finalizarSesion());
                // Limpiar cookies asociadas al navegador (para sesiones web)
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookies(null);
                cookieManager.flush();
            } else if ("facebook.com".equals(providerId)) {
                // Cerrar sesión de Facebook
                logoutFacebook();
            } else {
                // Otros proveedores o caso por defecto
                finalizarSesion();
            }
        }

    private void logoutFacebook() {

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null || accessToken.isExpired()) {
            Log.e("FacebookLogout", "El token de acceso de Facebook es inválido o ha caducado.");
            finalizarSesion();
            return;
        }
        if (AccessToken.getCurrentAccessToken() != null) {

            // Revocar permisos del usuario mediante GraphRequest
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "/me/permissions/",
                    null,

                    HttpMethod.DELETE,
                    response -> {
                        if (response != null && response.getError() == null) {
                            // Cerrar sesión del SDK de Facebook
                            LoginManager.getInstance().logOut();

                            // Limpiar cookies asociadas al navegador (para sesiones web)
                            CookieManager cookieManager = CookieManager.getInstance();
                            cookieManager.removeAllCookies(null);
                            cookieManager.flush();

                            // Redirigir al usuario al inicio de sesión
                            finalizarSesion();
                        } else {
                            // Manejar errores en la revocación de permisos
                            // Puedes mostrar un mensaje o loguear el error
                            Log.e("FacebookLogout", "Error al revocar permisos: " + response.getError());
                        }
                    }
            ).executeAsync();
        } else {
            // Si no hay sesión activa, simplemente finaliza la sesión
            finalizarSesion();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Verifica si se seleccionó "Edit User" desde el menú
        if (item.getItemId() == R.id.action_edit_user) {
            // Crear un Intent para abrir EditUserActivity
            Intent intent = new Intent(MainActivity.this, EditUserActivity.class);

            // Obtener los datos del usuario desde SharedPreferences (o de donde los tengas almacenados)
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            String userName = prefs.getString("user_name", "");
            String userEmail = prefs.getString("user_email", "");
            String userAddress = prefs.getString("user_address", "");
            String userPhone = prefs.getString("user_phone", "");
            String userProfileImageUrl = prefs.getString("user_profile_image_url", "");

            // Pasar los datos al Intent
            intent.putExtra("user_name", userName);
            intent.putExtra("user_email", userEmail);
            intent.putExtra("user_address", userAddress);
            intent.putExtra("user_phone", userPhone);
            intent.putExtra("user_profile_image_url", userProfileImageUrl);
            // Iniciar la actividad EditUserActivity
            startActivity(intent);


            return true; // Indica que el ítem ha sido manejado
        }

        return super.onOptionsItemSelected(item); // Llamada por defecto para otros ítems
    }

    /**
     * Inflamos el menu de opciones en la barra de herramientas.
     * @param menu menu que se va a inflar
     * @return true si el menu se infla correctamente
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    /**
     * Manejamos la navegacion hacia arriba cuando se usa el drawerlayout.
     *@return true si la navegacion hacia arriba se realiza correctamente
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}