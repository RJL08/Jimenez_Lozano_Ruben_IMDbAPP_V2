package com.example.jimenez_lozano_ruben_imdbapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
@SuppressWarnings("deprecation")


/**
 *  Actividad de inicio de sesion utilizando Google Sign-In y Firebase Authentication.
 */
public class SigninActivity extends AppCompatActivity {

    // Declaramos las variables
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Comprobamos si el usuario ya está registrado
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // Navegamos directamente al MainActivity
            navigateToMainActivity(
                    prefs.getString("userName", ""),
                    prefs.getString("userEmail", ""),
                    prefs.getString("userPhoto", "https://lh3.googleusercontent.com/a/default-user")
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
    }

    /**
     * Manejar el resultado del SignIn de Google. Si el resultado es exitoso, autenticar con Firebase.
     * De lo contrario, mostramos un mensaje de error.
     * @param data intent que contiene los datos del resultado
     */
    private void handleSignInResult(Intent data) {
        // Manejamos el resultado del SignIn de Google
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            // Autenticamos con Firebase
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            Log.w("GoogleSignIn", "Sign-In Failed", e);
            Toast.makeText(this, "Sign-In Failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Autenticamos al usuario en firebase utilizando su cuenta de google.
     * Si la autenticación es exitosa, guardamos los datos del usuario y navegamos a MainActivity.
     *
     * @param account cuenta de google obtenida tras el sign-in
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        // Autenticamos con Firebase utilizando la cuenta de Google obtenida
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                // Si la autenticacion es exitosa, guardamos los datos del usuario y navegamos a MainActivity
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                       // Guardamos los datos del usuario en SharedPreferences
                        if (user != null) {
                            saveUserDataToPreferences(user);
                            navigateToMainActivity(
                                    user.getDisplayName(),
                                    user.getEmail(),
                                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "https://lh3.googleusercontent.com/a/default-user"
                            );
                        }
                    } else {
                        Log.w("FirebaseAuth", "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Guardamos los datos del usuario en las preferencias compartidas.
     * Esto incluye el nombre de usuario, el correo electronico y la URL de la foto de perfil.
     *
     * @param user usuario autenticado en firebase
     */
    private void saveUserDataToPreferences(FirebaseUser user) {
        // Guardamos los datos del usuario en SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userName", user.getDisplayName());
        editor.putString("userEmail", user.getEmail());
        // Si no hay foto de perfil, usamos una URL por defecto para evitar errores
        editor.putString("userPhoto", user.getPhotoUrl() != null
                ? user.getPhotoUrl().toString()
                : "https://lh3.googleusercontent.com/a/default-user");
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
    private void navigateToMainActivity(String userName, String userEmail, String userPhoto) {
        Intent intent = new Intent(SigninActivity.this, MainActivity.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_photo", userPhoto);
        startActivity(intent);
        finish();
    }
}