package com.example.jimenez_lozano_ruben_imdbapp;


import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.jimenez_lozano_ruben_imdbapp.database.UserDataBaseHelper;
import com.example.jimenez_lozano_ruben_imdbapp.database.UsersManager;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EditUserActivity extends AppCompatActivity {
    private EditText etName, etEmail, etAddress, etPhone;
    private Button btnSelectAddress, btnSelectImage, btnSave;
    private ImageView ivProfileImage;

    // Definir los launchers para seleccionar dirección e imagen
    private ActivityResultLauncher<Intent> googleMapsLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);




        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);  // Añadido para dirección
        etPhone = findViewById(R.id.etPhone);  // Añadido para teléfono
        btnSelectAddress = findViewById(R.id.btnSelectAddress);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);
        ivProfileImage = findViewById(R.id.ivProfileImage);

        loadUserData();


        // Configurar el Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Habilitar la flecha de retroceso
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Inicializar los ActivityResultLaunchers
        googleMapsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Aquí puedes manejar la dirección seleccionada de Google Maps
                        Uri selectedLocation = result.getData().getData();
                        // Actualizar la dirección en el UI, si lo necesitas
                    }
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Maneja la imagen seleccionada
                        Uri imageUri = result.getData().getData();
                        ivProfileImage.setImageURI(imageUri);
                    }
                });

        // Configurar los botones
        btnSelectAddress.setOnClickListener(v -> openGoogleMaps());
        btnSelectImage.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> saveUserDetails());
    }

    private void loadUserData() {
        // Obtener los datos enviados desde MainActivity a través del Intent
        Intent intent = getIntent();
        String userName = intent.getStringExtra("user_name");
        String userEmail = intent.getStringExtra("user_email");
        String userAddress = intent.getStringExtra("user_address");
        String userPhone = intent.getStringExtra("user_phone");
        String userProfileImageUrl = intent.getStringExtra("user_profile_image_url"); // URL de la imagen de perfil


        // Cargar los datos en los campos correspondientes
        etName.setText(userName); // Nombre
        etEmail.setText(userEmail); // Correo electrónico
        etAddress.setText(userAddress); // Dirección
        etPhone.setText(userPhone); // Teléfono

        // Cargar la imagen de perfil si existe
        if (userProfileImageUrl != null && !userProfileImageUrl.isEmpty()) {
            // Si la URL no está vacía, cargamos la imagen con Picasso
            Picasso.get().load(userProfileImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)  // Imagen por defecto mientras carga
                    .error(R.drawable.ic_launcher_foreground)       // Imagen de error en caso de fallo
                    .into(ivProfileImage);                          // Cargar la imagen en el ImageView
        } else {
            // Si no hay URL de imagen, podemos mostrar una imagen por defecto
            ivProfileImage.setImageResource(R.drawable.ic_launcher_foreground); // Imagen por defecto
        }
    }

    private void openGoogleMaps() {
        // Abre Google Maps para que el usuario seleccione una dirección
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=location"));
        googleMapsLauncher.launch(intent);  // Usa el launcher para iniciar la actividad
    }

    private void openImagePicker() {
        // Abre un selector de imágenes
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);  // Usa el launcher para seleccionar la imagen
    }

    private void saveUserDetails() {
        // Obtener los datos ingresados por el usuario
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String address = etAddress.getText().toString();  // Dirección
        String phone = etPhone.getText().toString();  // Teléfono

        // Validar los campos antes de guardar
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese nombre y correo electrónico.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener la fecha/hora actual para login_time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String loginTime = sdf.format(new Date());

        // Convertir la imagen seleccionada en una URL
        Uri imageUri = (Uri) ivProfileImage.getTag();  // Usamos el tag para almacenar la URI de la imagen
        String imageUrl = imageUri != null ? imageUri.toString() : "";

        // Obtener un ID único para el usuario (en este caso, el correo electrónico)
        String userId = email;

        // Crear una instancia de UsersManager para guardar los datos
        UsersManager usersManager = new UsersManager(this);

        // Guardar o actualizar los detalles del usuario en la base de datos
        boolean success = usersManager.addOrUpdateUser(userId, name, email, UserDataBaseHelper.COLUMN_LOGIN_TIME, loginTime, imageUrl);

        if (success) {
            // Guardar la dirección y teléfono en la base de datos solo si el usuario ya existe
            SQLiteDatabase db = new UserDataBaseHelper(this).getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(UserDataBaseHelper.COLUMN_ADDRESS, address);
            values.put(UserDataBaseHelper.COLUMN_PHONE, phone);
            values.put(UserDataBaseHelper.COLUMN_IMAGE, imageUrl);

            // Actualizar los valores de la dirección y teléfono en la base de datos
            int rowsUpdated = db.update(
                    UserDataBaseHelper.TABLE_NAME,
                    values,
                    UserDataBaseHelper.COLUMN_USER_ID + " = ?",
                    new String[]{userId}
            );

            if (rowsUpdated > 0) {
                // Si los datos se actualizaron correctamente en la base de datos, mostrar mensaje
                Toast.makeText(this, "Datos del usuario guardados exitosamente.", Toast.LENGTH_SHORT).show();
                finish();  // Regresar a la actividad anterior o hacer lo que desees
            } else {
                Toast.makeText(this, "Hubo un error al guardar los datos adicionales.", Toast.LENGTH_SHORT).show();
            }

            db.close();
        } else {
            Toast.makeText(this, "Hubo un error al guardar los datos.", Toast.LENGTH_SHORT).show();
        }
    }
}
