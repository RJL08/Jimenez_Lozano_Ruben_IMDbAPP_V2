package com.example.jimenez_lozano_ruben_imdbapp;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.jimenez_lozano_ruben_imdbapp.database.FavoritesDatabaseHelper;
import com.example.jimenez_lozano_ruben_imdbapp.database.UsersManager;
import com.example.jimenez_lozano_ruben_imdbapp.sync.UsersSync;
import com.example.jimenez_lozano_ruben_imdbapp.utils.KeystoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.hbb20.CountryCodePicker;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;



public class EditUserActivity extends AppCompatActivity {
    private EditText etName, etEmail, etAddress, etPhone;
    private Button btnSelectAddress, btnSelectImage, btnSave;
    private ImageView ivProfileImage;

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 200;
    private static final int PERMISSION_READ_STORAGE_REQUEST_CODE = 201;
    private ActivityResultLauncher<Intent> selectAddressLauncher;
    // Launcher para abrir la galería (obtiene un Uri)
    private ActivityResultLauncher<String> galleryLauncher;
    // Launcher para solicitar el permiso de lectura de imágenes
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;
    private Spinner spinnerCountry;
    private CountryCodePicker ccp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        ccp = findViewById(R.id.ccp);
        etPhone = findViewById(R.id.etPhone);
        btnSelectAddress = findViewById(R.id.btnSelectAddress);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);
        ivProfileImage = findViewById(R.id.ivProfileImage);


        // Configurar toolbar y otros elementos
        setupToolbar();
        loadUserData();

        // Configuración del botón de selección de imagen

        btnSave.setOnClickListener(v -> saveUserDetails());

        // Listener para el botón "Select Image"
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickerDialog();
            }
        });

        // Configurar el botón para seleccionar dirección
        btnSelectAddress.setOnClickListener(v -> {
            Intent intent = new Intent(EditUserActivity.this, SelectAddressActivity.class);
            selectAddressLauncher.launch(intent); // Usar el launcher en lugar de startActivityForResult
        });


        // Inicializa el ActivityResultLauncher
        selectAddressLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String address = data.getStringExtra("address");
                            etAddress.setText(address); // Coloca la dirección en el EditText
                        }
                    }
                }
        );

        // Registrar el launcher para abrir la galería usando GetContent
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            // Muestra la imagen seleccionada en el ImageView
                            ivProfileImage.setImageURI(uri);
                        }
                    }
                }
        );

        // Registrar el launcher para solicitar el permiso de lectura de imágenes
        requestGalleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean isGranted) {
                        if (isGranted) {
                            // Si el permiso es concedido, abre la galería
                            openGallery();
                        } else {
                            Toast.makeText(EditUserActivity.this, "Permiso de lectura de imágenes denegado", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Opcional: puedes sincronizar el número telefónico con el código del país seleccionado
        ccp.setOnCountryChangeListener(() -> {
            String dialCode = ccp.getSelectedCountryCodeWithPlus();
            String currentPhone = etPhone.getText().toString().trim();
            if (!currentPhone.startsWith(dialCode)) {
                etPhone.setText(dialCode + " ");
                etPhone.setSelection(etPhone.getText().length());
            }
        });

    }

    // Método para validar que el número de teléfono empiece con el código seleccionado
    private boolean validatePhoneNumber() {
        String dialCode = ccp.getSelectedCountryCodeWithPlus();
        String phoneInput = etPhone.getText().toString().trim();
        if (!phoneInput.startsWith(dialCode)) {
            Toast.makeText(this, "El número de teléfono debe comenzar con " + dialCode, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            // Verifica si la solicitud de la cámara fue concedida
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, abre la cámara
                openCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == PERMISSION_READ_STORAGE_REQUEST_CODE) {
            // Verifica si la solicitud de acceso al almacenamiento fue concedida
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, abre la galería
                openGallery();
            } else {
                Toast.makeText(this, "Permiso de lectura de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Se ha tomado una foto (thumbnail)
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data"); // Contiene la imagen en baja resolución
                if (imageBitmap != null) {
                    ivProfileImage.setImageBitmap(imageBitmap);
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Se ha seleccionado imagen de la galería
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    ivProfileImage.setImageURI(imageUri);
                }
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Nota: Con este método (sin EXTRA_OUTPUT) obtendrás un "thumbnail" de la foto (baja resolución).
        // Si quieres la foto completa, debes crear un Uri para guardar la imagen y pasarlo en EXTRA_OUTPUT.
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    // Método para abrir la galería usando el launcher
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    // Comprueba el permiso adecuado para leer imágenes: en Android 13+ se usa READ_MEDIA_IMAGES, en versiones anteriores se usa READ_EXTERNAL_STORAGE
    private boolean checkReadImagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Solicita el permiso adecuado para leer imágenes
    private void requestReadImagesPermission() {
        String permissionToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{permissionToRequest},
                PERMISSION_READ_STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        // Verifica si el permiso para la cámara está otorgado
        // y si se requiere también permiso para escribir/almacenar (depende de tu uso)
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        // Verifica si el permiso para escribir/almacenar está otorgado (solo en Android 13+)
        int writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Dependiendo de tu caso de uso, puede que solo requieras CAMERA
        // Retorna true si ambos permisos están concedidos
        return cameraPermission == PackageManager.PERMISSION_GRANTED
                && writeStoragePermission == PackageManager.PERMISSION_GRANTED;
    }
    // Solicita el permiso para la cámara (depende de tu caso de uso)
    private void requestCameraPermission() {
        // Solicita el permiso de cámara y almacenamiento (si lo necesitas)
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_CAMERA_REQUEST_CODE
        );
    }
    // Muestra un diálogo para seleccionar imagen (foto o de la galería)
    private void showImagePickerDialog() {
        // Opciones del diálogo
        String[] options = {"Hacer foto", "Elegir de la galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Imagen");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Hacer foto (se mantiene el código existente para cámara)
                    if (checkCameraPermission()) {
                        openCamera();
                    } else {
                        requestCameraPermission();
                    }
                } else if (which == 1) {
                    // Elegir de la galería
                    if (checkReadImagesPermission()) {
                        openGallery();
                    } else {
                        requestReadImagesPermission();
                    }
                }
            }
        });
        builder.create().show();
    }


    // Métodos para el resto de tu actividad (guardar datos, cargar usuario, etc.)
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // Carga los datos del usuario desde la base de datos local
    private void loadUserData() {
        // Obtener el UID del usuario autenticado
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.e("EditUser", "No hay usuario logueado.");
            return;
        }

        // Obtener el Cursor con los datos del usuario desde la base de datos local
        UsersManager usersManager = new UsersManager(this);
        Cursor cursor = usersManager.getUserData(uid);

        if (cursor != null && cursor.moveToFirst()) {
            // Obtener índices de las columnas
            int nameIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_NAME);
            int emailIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_EMAIL);
            int addressIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_ADDRESS);
            int phoneIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_PHONE);
            int imageIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_IMAGE);

            // Leer los valores desde el Cursor
            String name = cursor.getString(nameIndex);
            String email = cursor.getString(emailIndex);
            String encryptedAddress = cursor.getString(addressIndex);
            String encryptedPhone = cursor.getString(phoneIndex);
            String image = cursor.getString(imageIndex);

            // Cerrar el Cursor
            cursor.close();

            // Asignar nombre y correo (estos campos no se cifran)
            if (name != null) {
                etName.setText(name);
            }
            if (email != null) {
                etEmail.setText(email);
            }

            // Descifrar dirección y teléfono
            try {
                KeystoreManager keystoreManager = new KeystoreManager();
                if (encryptedAddress != null && !encryptedAddress.isEmpty()) {
                    String decryptedAddress = keystoreManager.decryptData(encryptedAddress);
                    etAddress.setText(decryptedAddress);
                } else {
                    etAddress.setText("");
                }
                if (encryptedPhone != null && !encryptedPhone.isEmpty()) {
                    String decryptedPhone = keystoreManager.decryptData(encryptedPhone);
                    etPhone.setText(decryptedPhone);
                } else {
                    etPhone.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al descifrar datos", Toast.LENGTH_SHORT).show();
            }

            // Cargar la imagen en el ImageView
            if (image != null && !image.isEmpty()) {
                if (image.startsWith("http://") || image.startsWith("https://")) {
                    // Se asume que es una URL remota
                    Picasso.get()
                            .load(image)
                            .placeholder(R.drawable.esperando)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(ivProfileImage);
                } else {
                    // Se asume que es una ruta local
                    File imageFile = new File(image);
                    if (imageFile.exists()) {
                        Picasso.get()
                                .load(imageFile)
                                .placeholder(R.drawable.esperando)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(ivProfileImage);
                    } else {
                        // Si el archivo no existe, se intenta cargar anteponiendo "file://"
                        String imageUri = "file://" + image;
                        Picasso.get()
                                .load(imageUri)
                                .placeholder(R.drawable.esperando)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(ivProfileImage);
                    }
                }
            } else {
                ivProfileImage.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            Log.d("EditUser", "El usuario no se encontró en la base de datos local.");
        }
    }


    private void saveUserDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Leer los valores de los EditText
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Validaciones mínimas
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        // Puedes agregar validaciones adicionales (por ejemplo, formato de email, teléfono numérico, etc.)

        // Obtener el Bitmap del ImageView de perfil
        Bitmap bitmap = ((BitmapDrawable) ivProfileImage.getDrawable()).getBitmap();
        // Guardar la imagen y obtener la ruta
        String imagePath = saveImageToInternalStorage(bitmap, "profile_image.png");
        try {
            // Instanciar el KeystoreManager para cifrar los datos sensibles
            KeystoreManager keystoreManager = new KeystoreManager();
            // Encriptar la dirección y el teléfono
            String encryptedAddress = keystoreManager.encryptData(address);
            String encryptedPhone = keystoreManager.encryptData(phone);


            UsersManager usersManager = new UsersManager(this);
            boolean updated = usersManager.registerUserOnSignIn(
                    uid,
                    name,
                    email,
                    imagePath,
                    null,              // No se actualiza login_time al editar el perfil
                    encryptedAddress,
                    encryptedPhone
            );
                        if (!updated) {
                            Log.e("EditUser", "Error al actualizar el usuario en la base de datos local.");
                            Toast.makeText(this, "Error al guardar datos localmente", Toast.LENGTH_SHORT).show();
                            return;
                        }

                       new UsersSync().syncLocalToFirestore(this, new FavoritesDatabaseHelper(this));

                        // Actualizar el perfil de FirebaseUser con el nuevo nombre y la nueva foto
                        updateFirebaseUserProfile(name, imagePath);
                        Toast.makeText(this, "Datos guardados y sincronizados correctamente", Toast.LENGTH_SHORT).show();



        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al encriptar los datos", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Metodo por el que obtenemos la direccion de la imagen seleccionada de la galeria o de la foto realizada
     * @param bitmap
     * @param fileName
     * @return
     */
    private String saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        // Obtener el directorio interno de la aplicación
        File directory = getFilesDir();
        File imageFile = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return imageFile.getAbsolutePath(); // Retornar la ruta del archivo
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Actualiza el perfil de FirebaseUser con el nuevo nombre y la nueva foto (si es necesario)
    private void updateFirebaseUserProfile(String newName, String newPhotoUrl) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .setPhotoUri(Uri.parse(newPhotoUrl))
                    .build();

            currentUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("EditUser", "Perfil de usuario actualizado (nombre y foto) en FirebaseUser");
                        } else {
                            Log.e("EditUser", "Error al actualizar el perfil de FirebaseUser", task.getException());
                        }
                    });
        }
    }


}
