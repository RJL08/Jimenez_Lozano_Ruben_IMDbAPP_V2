package com.example.jimenez_lozano_ruben_imdbapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.facebook.FacebookSdk;
import com.google.firebase.auth.UserInfo;

import org.json.JSONObject;


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


        // Comprobamos si el usuario ya está registrado
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // Navegamos directamente al MainActivity
            navigateToMainActivity(
                    prefs.getString("userName", ""),
                    prefs.getString("userEmail", ""),
                    prefs.getString("userPhoto", ""),
                    prefs.getString("provider", "")
            );

            return;
        }

        // Configuramos el layout
        setContentView(R.layout.activity_signin);

        // Configuramos el Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        if (getIntent().getBooleanExtra("logout", false)) {

        }
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
                            fetchFacebookUserData(token);
                        }
                    } else {
                        Toast.makeText(SigninActivity.this, "Autenticación fallida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchFacebookUserData(AccessToken token) {
        GraphRequest request = GraphRequest.newMeRequest(token, (object, response) -> {
            try {
                if (object != null) {
                    // Obtener datos del perfil
                    String name = object.optString("name"); // Nombre del usuario
                    String email = object.optString("email"); // Correo electrónico del usuario (puede no estar disponible)
                    String facebookId = object.optString("id"); // ID único del usuario en Facebook

                    // Acceder a la URL de la foto de perfil desde el campo "picture"
                    JSONObject pictureObject = object.optJSONObject("picture");
                    String photoUrl = null;
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

                    // Guardar datos en SharedPreferences
                    saveUserDataToPreferences(name, email, photoUrl, "facebook.com");

                    // Navegar a MainActivity
                    navigateToMainActivity(name, email, photoUrl, "facebook.com");
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
                                        // Obtén el providerId (en este caso, "google.com")
                                        String providerId = "google.com";
                                        // Asegúrate de manejar null para la foto de perfil
                                        String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "https://lh3.googleusercontent.com/a/default-user";

                                        // Guarda los datos del usuario en SharedPreferences
                                        saveUserDataToPreferences(
                                                user.getDisplayName(),
                                                user.getEmail(),
                                                photoUrl,
                                                "google.com"
                                        );

                                        // Navega a MainActivity
                                        navigateToMainActivity(
                                                user.getDisplayName(),
                                                user.getEmail(),
                                                photoUrl,
                                                providerId


                                        );
                                    }
                                } else {
                                    Log.e("SigninActivity", "handleSignInResult: Error al vincular cuenta.", linkTask.getException());
                                }
                            });
                } else {
                    // Si no hay un usuario autenticado, iniciar sesión normalmente
                    firebaseAuthWithGoogle(account);
                }
            }
        } catch (ApiException e) {
            Log.e("SigninActivity", "handleSignInResult: Error al autenticar con Google.", e);
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
                            String providerId = "google.com";
                            // Construir URL de foto de perfil para Google
                            String photoUrl = user.getPhotoUrl() != null
                                    ? user.getPhotoUrl().toString()
                                    : "https://lh3.googleusercontent.com/a/default-user";

                            // Guardar datos del usuario en SharedPreferences
                            saveUserDataToPreferences(
                                    user.getDisplayName(),
                                    user.getEmail(),
                                    photoUrl,
                                    providerId
                            );

                            // Navegar a MainActivity
                            navigateToMainActivity(
                                    user.getDisplayName(),
                                    user.getEmail(),
                                    photoUrl,
                                    "google.com"

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
    private void saveUserDataToPreferences(String name, String email, String photoUrl, String provider) {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userName", name);
        editor.putString("userEmail", email);
        editor.putString("userPhoto", photoUrl);
        editor.putString("provider", provider);
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
    private void navigateToMainActivity(String userName, String userEmail, String userPhoto, String provider) {
        Intent intent = new Intent(SigninActivity.this, MainActivity.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_photo", userPhoto);
        intent.putExtra("provider", provider);
        startActivity(intent);
        finish();
    }
}