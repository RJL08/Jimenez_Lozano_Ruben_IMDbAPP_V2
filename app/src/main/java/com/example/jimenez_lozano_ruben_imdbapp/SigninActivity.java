package com.example.jimenez_lozano_ruben_imdbapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.example.jimenez_lozano_ruben_imdbapp.database.UsersManager;
import com.example.jimenez_lozano_ruben_imdbapp.sync.FavoritesSync;
import com.example.jimenez_lozano_ruben_imdbapp.sync.UsersSync;
import com.example.jimenez_lozano_ruben_imdbapp.utils.AppLifecycleManager;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;

import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import com.facebook.FacebookSdk;
import org.json.JSONObject;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

@SuppressWarnings("deprecation")


/**
 *  Actividad de inicio de sesion utilizando Google Sign-In y Firebase Authentication.
 */
public class SigninActivity extends AppCompatActivity {

    // Declaramos las variables
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<Intent> signInLauncher;
    // Para manejar los callbacks de Facebook Login
    private CallbackManager callbackManager;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private Context context;
    private AppLifecycleManager appLifecycleManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);

        // Inicializar Facebook SDK

        new Thread(() -> {
            FacebookSdk.sdkInitialize(getApplicationContext());
            AppEventsLogger.activateApp(this.getApplication());
        }).start();


        appLifecycleManager = new AppLifecycleManager(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(appLifecycleManager);
        }

            // Sincronizar usuarios
            UsersSync usersSync = new UsersSync();
            usersSync.syncLocalToFirestore(this, new FavoritesDatabaseHelper(this));
            usersSync.syncUsersFromFirestore(this);





        // Comprobamos si el usuario ya está registrado
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn ) {
            // Navegamos directamente al MainActivity
            navigateToMainActivity(
                    prefs.getString("userName", ""),
                    prefs.getString("userEmail", ""),
                    prefs.getString("userPhoto", ""),
                    prefs.getString("provider", ""),
                    prefs.getString("userId", "")///**************************
            );

            return;
        }

        // Configuramos el layout
        setContentView(R.layout.activity_signin);

        // Configuramos el Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicializamos las vistas de la actividad de inicio de sesion con Firebase
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        // Configuramos el Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sign In");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Deshabilitar botón de volver
        }

        // Configuramos Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Configuramos el ActivityResultLauncher para el resultado del SignIn de Google
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleSignInResult(result.getData());
                    } else {
                        Toast.makeText(this, "Sign-In Canceled", Toast.LENGTH_SHORT).show();
                    }
                });

        // Configuramos el botón de Google Sign-In y su texto
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        setGoogleSignInButtonText(signInButton, "Sign in with Google");
        signInButton.setOnClickListener(v -> {
            Intent signInIntent = getGoogleSignInClient().getSignInIntent();
            signInLauncher.launch(signInIntent);
        });

        // Configuracion del Facebook Login
        callbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = findViewById(R.id.login_button);
        loginButton.setPermissions(Arrays.asList("email", "public_profile"));


        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("SigninActivity", "Facebook Login: Inicio de sesión exitoso.");
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d("SigninActivity", "Facebook Login: Inicio de sesión cancelado.");
                Toast.makeText(SigninActivity.this, "Facebook Sign-In Canceled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Log.e("SigninActivity", "Facebook Login: Error: " + error.getMessage(), error);
                Toast.makeText(SigninActivity.this, "Facebook Sign-In Failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Verificamos si se solicitó un logout**************
        //if (getIntent().getBooleanExtra("logout", false)) {

        configureLoginButton();
        configureRegisterButton();
    }

    private void checkAndSyncUserData(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid(); // Obtener el ID del usuario autenticado

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        //  1. Verificamos si el usuario EXISTE en la colección "users"
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        //  2. Si el usuario existe, obtenemos sus datos de la nube
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("image");
                        String address = documentSnapshot.getString("address");
                        String phone = documentSnapshot.getString("phone");

                        //  3. Guardamos los datos en SharedPreferences
                        saveUserDataToPreferences(name, email, photoUrl, "firebase.com", userId);

                        //  4. Guardamos los datos en SQLite
                        UsersManager usersManager = new UsersManager(this);
                        boolean registered = usersManager.registerUserOnSignIn(
                                userId, name, email, photoUrl, null, address, phone
                        );

                        if (!registered) {
                            Log.e("SyncUserData", "Error al registrar el usuario en la base de datos local.");
                        }

                        //  5. Sincronizamos la tabla de favoritos
                        syncFavoritesFromFirestore(userId);

                    } else {
                        //  Si el usuario NO existe en Firestore, mostramos un mensaje y evitamos el acceso
                        Log.e("SyncUserData", "El usuario no está registrado en Firestore.");
                        Toast.makeText(this, "Error: No estás registrado. Por favor, regístrate primero.", Toast.LENGTH_LONG).show();

                        // Opcional: Cerrar sesión automáticamente si el usuario no está en Firestore
                        FirebaseAuth.getInstance().signOut();
                    }
                })
                .addOnFailureListener(e -> Log.e("SyncUserData", "Error al verificar usuario en Firestore: ", e));
    }

    private void syncFavoritesFromFirestore(String userId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);

        //  Verificar si hay favoritos en Firestore para este usuario
        firestore.collection("favorites").document(userId).collection("movies").get()
                .addOnSuccessListener(querySnapshot -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    //  Borrar los favoritos locales antes de sincronizar
                    db.execSQL("DELETE FROM " + FavoritesDatabaseHelper.TABLE_FAVORITES +
                            " WHERE " + FavoritesDatabaseHelper.COLUMN_USER_ID + "='" + userId + "'");

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        ContentValues values = new ContentValues();
                        values.put(FavoritesDatabaseHelper.COLUMN_FAVORITE_ID, document.getId());
                        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
                        values.put(FavoritesDatabaseHelper.COLUMN_USER_EMAIL, document.getString("userEmail"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_TITLE, document.getString("movieTitle"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_IMAGE, document.getString("movieImage"));
                        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, document.getString("releaseDate"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_RATING, document.getString("movieRating"));
                        values.put(FavoritesDatabaseHelper.COLUMN_MOVIE_OVERVIEW, document.getString("overview"));

                        db.insert(FavoritesDatabaseHelper.TABLE_FAVORITES, null, values);
                    }

                    db.close();
                    Log.d("SyncFavorites", "Favoritos sincronizados desde Firestore a SQLite.");
                })
                .addOnFailureListener(e -> Log.e("SyncFavorites", "Error al sincronizar favoritos: ", e));
    }


    private void registerUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser newUser = firebaseAuth.getCurrentUser();
                        if (newUser != null) {
                            // Obtener los datos del usuario
                            String userId = newUser.getUid();
                            String userEmail = newUser.getEmail();
                            String userName = newUser.getDisplayName() != null ? newUser.getDisplayName() : "unknown";

                            // Aquí puedes asignar valores predeterminados para los nuevos campos
                            String address = "";  // Asignamos vacío como valor predeterminado para la dirección
                            String phone = "";    // Asignamos vacío como valor predeterminado para el teléfono
                            String image = "";    // Asignamos vacío como valor predeterminado para la imagen (puedes actualizar esto más adelante)

                            // Log para verificar los datos
                            Log.d("RegisterUser", "UID: " + userId + ", Email: " + userEmail);

                            // Validar los datos antes de guardar
                            if (userId == null || userId.isEmpty() || userEmail == null || userEmail.isEmpty()) {
                                Log.e("RegisterUser", "Error: Datos del usuario inválidos.");
                                return;
                            }

                            // Guardar en la base de datos local
                            UsersManager usersManager = new UsersManager(this);
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                            boolean userAdded = usersManager.addOrUpdateUser(
                                    userId,
                                    userName,
                                    userEmail,
                                    FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                                    loginTime,
                                    image, // Imagen
                                    address, // Dirección
                                    phone  // Teléfono
                            );

                            if (!userAdded) {
                                Log.e("RegisterUser", "Error al guardar el usuario en la base de datos local.");
                            }

                            // Guardar datos en SharedPreferences****************
                            saveUserDataToPreferences(userName, userEmail, null, null, userId);

                            // Navegar a la pantalla principal
                            navigateToMainActivity(userName, userEmail, null, "email", userId);
                        }
                    } else {
                        Toast.makeText(SigninActivity.this, "Error al registrarse: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    /**
     * Configuramos el botón de registro para validar y registrar al usuario.
     */
    private void configureRegisterButton() {
        registerButton.setOnClickListener(v -> validateAndRegister());
    }

    /**
     * Comprueba si el correo ya está registrado en Firebase.
     * Si ya está registrado, solicita iniciar sesión. De lo contrario, registra al usuario.
     * @param email Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     */
    private void checkEmailAndRegister(String email, String password) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isEmailRegistered = task.getResult().getSignInMethods() != null &&
                                !task.getResult().getSignInMethods().isEmpty();

                        if (isEmailRegistered) {
                            // Correo ya registrado
                            Toast.makeText(SigninActivity.this, "Email already registered. Please log in.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Registrar al usuario
                            registerUser(email, password);
                        }
                    } else {
                        Toast.makeText(SigninActivity.this, "Error validating email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Validamos los datos para el registro. Si los datos son válidos, intentamos registrar al usuario.
     * Si el correo ya está registrado, pedimos al usuario que inicie sesión.
     */
    private void validateAndRegister() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validar si los campos están vacíos
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(SigninActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(SigninActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar si el email tiene formato válido
        if (!isValidEmail(email)) {
            Toast.makeText(SigninActivity.this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar si la contraseña cumple con la longitud mínima
        if (password.length() < 8) {
            Toast.makeText(SigninActivity.this, "Password must be at least 8 characters long.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar si el email ya está registrado en Firebase
        checkEmailAndRegister(email, password);
    }

    /**
     * Configuramos el boton de inicio de sesion.
     */
    private void configureLoginButton() {
        loginButton.setOnClickListener(v -> validateAndLogin());
    }

    /**
     * Validamos los datos de inicio de sesion. Si los datos son validos, iniciamos sesion. De lo contrario, mostramos un mensaje de error.
     */
    private void validateAndLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validar si los campos están vacíos
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(SigninActivity.this, "Por favor, introduzca su email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(SigninActivity.this, "Por favor, introduzca su contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar si el email tiene formato válido
        if (!isValidEmail(email)) {
            Toast.makeText(SigninActivity.this, "Por favor, introduzca un email válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar si la contraseña cumple con la longitud mínima
        if (password.length() < 8) {
            Toast.makeText(SigninActivity.this, "La contraseña debe tener al menos 8 caracteres y no más de 12.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Intentar iniciar sesión
        loginOrRegisterUser(email, password);
    }

    /**
     * Validamos si el email es valido. Si no lo es, mostramos un mensaje de error.
     * @param email
     * @return
     */
    private boolean isValidEmail(String email) {
        if (email == null) return false;

        // Expresión regular para validar el formato del correo.
        String regexCorreo = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.(com|es)$";
        return email.matches(regexCorreo);
    }

    /**
     * Iniciamos sesion o registramos al usuario si no está registrado.
     * @param email
     * @param password
     */
    private void loginOrRegisterUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            checkAndSyncUserData(user);
                            // Obtener los datos del usuario
                            String userId = user.getUid();
                            String userEmail = user.getEmail();
                            String userName = user.getDisplayName(); // Obtener el nombre de Firebase

                            // Validar los datos antes de guardarlos
                            if (userId == null || userId.isEmpty() || userEmail == null || userEmail.isEmpty()) {
                                Log.e("LoginUser", "Error: Datos del usuario inválidos.");
                                return;
                            }


                            // Guardar en la base de datos local
                            UsersManager usersManager = new UsersManager(this);
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                            // Recuperar los datos existentes del usuario
                            Cursor cursor = usersManager.getUserData(userId);

                            String image = null;
                            String address = null;
                            String phone = null;

                            if (cursor != null && cursor.moveToFirst()) {
                                // Cargar los datos del usuario
                                int imageIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_IMAGE);
                                int addressIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_ADDRESS);
                                int phoneIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_PHONE);

                                image = cursor.getString(imageIndex);
                                address = cursor.getString(addressIndex);
                                phone = cursor.getString(phoneIndex);

                                cursor.close(); // Cerrar el cursor
                            }

                            // Actualizar el usuario en la base de datos local
                            boolean userAdded = usersManager.addOrUpdateUser(
                                    userId,
                                    userName,
                                    userEmail,
                                    FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                                    loginTime,
                                    image, // Usar la imagen existente
                                    address, // Usar la dirección existente
                                    phone  // Usar el teléfono existente
                            );

                            if (!userAdded) {
                                Log.e("LoginUser", "Error al guardar el usuario en la base de datos local.");
                            }

                            // Guardar los datos en SharedPreferences
                            saveUserDataToPreferences(userName, userEmail, image, null, userId);

                            // Navegar a la pantalla principal
                            navigateToMainActivity(userName, userEmail, image, null, userId);
                        }
                    } else {
                        // Verifica el error del login
                        Exception exception = task.getException();
                        if (exception != null) {
                            Log.e("LoginUser", "Error al iniciar sesión: " + exception.getMessage());
                        }
                        // Si el usuario no está registrado, intentamos registrarlo
                        registerUser(email, password);
                    }
                });
    }



    /**
     * Manejar el token de acceso de Facebook para autenticar con Firebase.
     *
     * @param token El token de acceso de Facebook.
     */
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase autenticado correctamente
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        if (currentUser != null) {
                            // Llamada a la API de Facebook para obtener datos del perfil
                            fetchFacebookUserData(token ,currentUser);

                        }
                    } else {
                        Toast.makeText(SigninActivity.this, "Autenticación fallida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Llamada a la API de Facebook para obtener datos del perfil.
     * @param token
     * @param firebaseUser
     */
    private void fetchFacebookUserData(AccessToken token, FirebaseUser  firebaseUser) {
        GraphRequest request = GraphRequest.newMeRequest(token, (object, response) -> {
            try {
                if (object != null) {


                    // Llamada para sincronizar datos del usuario
                    checkAndSyncUserData(firebaseUser);
                    // Obtener datos del perfil
                    String name = object.optString("name"); // Nombre del usuario
                    String email = object.optString("email"); // Correo electrónico del usuario (puede no estar disponible)
                    String facebookId = object.optString("id"); // ID único del usuario en Facebook

                    String photoUrl = null;
                    // Acceder a la URL de la foto de perfil desde el campo "picture"
                    JSONObject pictureObject = object.optJSONObject("picture");

                    if (pictureObject != null) {
                        JSONObject dataObject = pictureObject.optJSONObject("data");
                        if (dataObject != null) {
                            photoUrl = dataObject.optString("url"); // URL de la foto de perfil
                        }
                    }

                    // Si no se encuentra la URL de la foto, construimos una manualmente
                    if (photoUrl == null) {
                        photoUrl = "https://graph.facebook.com/" + facebookId + "/picture?type=large";
                    }

                    // Asignar valores predeterminados para los nuevos campos
                    String address = "";  // Asignamos vacío como valor predeterminado para la dirección
                    String phone = "";    // Asignamos vacío como valor predeterminado para el teléfono


                    // Guardar datos en SharedPreferences
                    saveUserDataToPreferences(name, email, photoUrl, "facebook.com", firebaseUser.getUid());//************************

                    // Obtener la fecha y hora actual como login_time
                    String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    // Registra el usuario al iniciar sesión en la base de datos local y sincroniza con la nube
                    UsersManager usersManager = new UsersManager(this);
                    boolean registered = usersManager.registerUserOnSignIn(
                            firebaseUser.getUid(),
                            name,
                            email,
                            photoUrl,
                            loginTime,
                            address,
                            phone
                    );


                    if (!registered) {
                        Log.e("fetchFacebookUserData", "Error al registrar el usuario en la base de datos local.");
                    }
                    UsersSync usersSync = new UsersSync();
                    FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);
                    FavoritesSync favoritesSync = new FavoritesSync();

                    usersSync.syncFirestoreToLocal(this); // Sincronizar usuarios
                    favoritesSync.syncFirestoreToLocal(this, dbHelper); // Sincronizar favoritos

                    // Navegar a MainActivity con los datos completos
                    navigateToMainActivity(name, email, photoUrl, "facebook.com", firebaseUser.getUid());
                } else {
                    Log.e("FacebookAPI", "El objeto devuelto por la API es nulo.");
                    Toast.makeText(this, "No se pudieron obtener los datos del perfil", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("FacebookAPI", "Error al procesar los datos del perfil: ", e);
                Toast.makeText(this, "Error al procesar los datos del perfil", Toast.LENGTH_SHORT).show();
            }
        });

        // Especificar los campos que queremos recuperar
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,picture.type(large)");
        request.setParameters(parameters);

        // Ejecutar la solicitud
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Delegamos el resultado al CallbackManager de Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Manejar el resultado del SignIn de Google. Si el resultado es exitoso, autenticar con Firebase.
     * De lo contrario, mostramos un mensaje de error.
     * @param data intent que contiene los datos del resultado
     */
    private void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                Log.d("SigninActivity", "handleSignInResult: Credenciales de Google obtenidas.");
                AuthCredential googleCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                // Verifica si hay un usuario autenticado en Firebase antes de vincular
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    currentUser.linkWithCredential(googleCredential)
                            .addOnCompleteListener(linkTask -> {
                                if (linkTask.isSuccessful()) {
                                    Log.d("SigninActivity", "handleSignInResult: Cuenta vinculada exitosamente.");
                                    FirebaseUser user = linkTask.getResult().getUser();
                                    if (user != null) {
                                        String providerId = "google.com";
                                        String photoUrl = (user.getPhotoUrl() != null)
                                                ? user.getPhotoUrl().toString()
                                                : "https://lh3.googleusercontent.com/a/default-user";

                                        // Guarda los datos del usuario en SharedPreferences
                                        saveUserDataToPreferences(user.getDisplayName(), user.getEmail(), photoUrl, "google.com", user.getUid()
                                        );

                                        // Navega a MainActivity
                                        navigateToMainActivity(user.getDisplayName(), user.getEmail(), photoUrl, providerId, user.getUid()
                                        );
                                    }
                                } else {
                                    // Manejo de la excepción de colisión de credenciales
                                    Exception ex = linkTask.getException();
                                    if (ex instanceof FirebaseAuthUserCollisionException) {
                                        Log.e("SigninActivity", "handleSignInResult: Credenciales ya vinculadas a otra cuenta. Iniciando sesión.", ex);
                                        firebaseAuth.signInWithCredential(googleCredential)
                                                .addOnCompleteListener(signInTask -> {
                                                    if (signInTask.isSuccessful()) {
                                                        Log.d("SigninActivity", "handleSignInResult: Iniciado sesión con credenciales existentes.");
                                                        FirebaseUser user = signInTask.getResult().getUser();
                                                        if (user != null) {
                                                            String providerId = "google.com";
                                                            String photoUrl = (user.getPhotoUrl() != null)
                                                                    ? user.getPhotoUrl().toString()
                                                                    : "https://lh3.googleusercontent.com/a/default-user";

                                                            // Guarda los datos del usuario en SharedPreferences
                                                            saveUserDataToPreferences(user.getDisplayName(), user.getEmail(), photoUrl,
                                                                    "google.com", user.getUid()
                                                            );

                                                            // Navega a MainActivity
                                                            navigateToMainActivity(user.getDisplayName(), user.getEmail(), photoUrl, providerId, user.getUid()
                                                            );
                                                        }
                                                    } else {
                                                        Log.e("SigninActivity", "handleSignInResult: Error al iniciar sesión con credenciales existentes.", signInTask.getException());
                                                        Toast.makeText(SigninActivity.this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Log.e("SigninActivity", "handleSignInResult: Error al vincular cuenta.", ex);
                                        Toast.makeText(SigninActivity.this, "Error al vincular cuenta", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    // Si no hay un usuario autenticado, iniciar sesión normalmente
                    firebaseAuthWithGoogle(account);
                }
            }
        } catch (ApiException e) {
            Log.e("SigninActivity", "handleSignInResult: Error al autenticar con Google.", e);
            Toast.makeText(SigninActivity.this, "Error al autenticar con Google", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Autenticamos al usuario en firebase utilizando su cuenta de google.
     * Si la autenticación es exitosa, guardamos los datos del usuario y navegamos a MainActivity.
     *
     * @param account cuenta de google obtenida tras el sign-in
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {

                            checkAndSyncUserData(user);
                            // Sincronizar usuarios y favoritos desde Firestore
                            UsersSync usersSync = new UsersSync();
                            FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);
                            usersSync.syncFirestoreToLocal(this); // Sincronizar usuarios
                            FavoritesSync favoritesSync = new FavoritesSync();
                            favoritesSync.syncFirestoreToLocal(this, dbHelper);

                            String providerId = "google.com";
                            String userId = user.getUid();
                            String email = user.getEmail();
                            String name = user.getDisplayName();
                            // Construir URL de foto de perfil para Google
                            String photoUrl = user.getPhotoUrl() != null
                                    ? user.getPhotoUrl().toString()
                                    : "https://lh3.googleusercontent.com/a/default-user";


                            // Asignar valores predeterminados para los nuevos campos
                            String address = "";  // Asignamos vacío como valor predeterminado para la dirección
                            String phone = "";    // Asignamos vacío como valor predeterminado para el teléfono
                            // Obtener la fecha y hora actual formateada como login time
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                            // Guardar datos del usuario en SharedPreferences
                            saveUserDataToPreferences(user.getDisplayName(), user.getEmail(), photoUrl, providerId, user.getUid()//********
                            );


                            // Registrar usuario y sincronizar
                            UsersManager usersManager = new UsersManager(this);
                            boolean registered = usersManager.registerUserOnSignIn(userId, name, email, photoUrl, loginTime, address, phone
                            );
                            if (!registered) {
                                Log.e("firebaseAuthWithGoogle", "Error al registrar el usuario en la base de datos local.");
                            }

                            // Opcional: Sincronizar la base de datos local con Firestore
                            new UsersSync().syncLocalToFirestore(this, new FavoritesDatabaseHelper(this));

                            // Navegar a MainActivity
                            navigateToMainActivity(user.getDisplayName(), user.getEmail(), photoUrl, "google.com", user.getUid()

                            );
                        }
                    } else {
                        Log.e("SigninActivity", "firebaseAuthWithGoogle: Error al autenticar con Google.", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Guardamos los datos del usuario en las preferencias compartidas.
     * Esto incluye el nombre de usuario, el correo electronico y la URL de la foto de perfil.
     *
     * @param
     */
    public void saveUserDataToPreferences(String name, String email, String photoUrl, String provider, String userId) {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        Log.d("SaveUserData", "Guardando: userId=" + userId + ", userEmail=" + email);
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userName", name);
        editor.putString("userEmail", email);
        editor.putString("userPhoto", photoUrl);
        editor.putString("provider", provider);
        editor.putString("userId", userId);//*******
        editor.apply();
    }




    /**
     * Configuramos y devolvemos el cliente de google sign-in.
     *
     * @return googlesigninclient configurado
     */
    private GoogleSignInClient getGoogleSignInClient() {
        // Configuramos el cliente de Google Sign-In con las opciones de autenticacion de Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(this, gso);
    }

    /**
     * Cambiamos el texto predeterminado del boton de google sign-in.
     * De eta manera personalizamos el texto del botn
     * @param signInButton boton de google sign-in
     * @param buttonText texto que queremos mostrar en el boton
     */
    private void setGoogleSignInButtonText(SignInButton signInButton, String buttonText) {
        // Recorremos los hijos del boton para encontrar el TextView que contiene el texto
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            // Si encontramos un TextView, lo cambiamos al texto personalizado
            if (signInButton.getChildAt(i) instanceof TextView) {
                ((TextView) signInButton.getChildAt(i)).setText(buttonText);
                return;
            }
        }
    }

    /**
     * Navegamos a la actividad principal (mainactivity) tras el inicio de sesion.
     * @param userName  nombre del usuario
     * @param userEmail correo electronico del usuario
     * @param userPhoto url de la foto del usuario
     */
    private void navigateToMainActivity(String userName, String userEmail, String userPhoto, String provider, String userId) {
        // Validar si userPhoto es nulo o vacío
        if (userPhoto == null || userPhoto.trim().isEmpty()) {
            userPhoto = "https://example.com/default-profile-image.png"; // URL de imagen predeterminada
        }
        Intent intent = new Intent(SigninActivity.this, MainActivity.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_photo", userPhoto);
        intent.putExtra("provider", provider);
        intent.putExtra("user_id", userId);
        startActivity(intent);
        finish();
    }



}
