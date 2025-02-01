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
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private boolean checkReadStoragePermission() {
        int readStoragePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        return readStoragePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_READ_STORAGE_REQUEST_CODE
        );
    }

    private boolean checkCameraPermission() {
        // Verifica si el permiso para la cámara está otorgado
        // y si se requiere también permiso para escribir/almacenar (depende de tu uso)
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        // Si quieres asegurarte de poder guardar la foto en almacenamiento externo,
        // también revisa WRITE_EXTERNAL_STORAGE en versiones anteriores a Android Q
        int writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Dependiendo de tu caso de uso, puede que solo requieras CAMERA
        // Retorna true si ambos permisos están concedidos
        return cameraPermission == PackageManager.PERMISSION_GRANTED
                && writeStoragePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        // Solicita el permiso de cámara y almacenamiento (si lo necesitas)
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_CAMERA_REQUEST_CODE
        );
    }

    private void showImagePickerDialog() {
        // Opciones del diálogo
        String[] options = {"Hacer foto", "Elegir de la galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Imagen");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Hacer foto
                    if (checkCameraPermission()) {
                        openCamera();
                    } else {
                        requestCameraPermission();
                    }
                } else if (which == 1) {
                    // Elegir de la galería
                    if (checkReadStoragePermission()) {
                        openGallery();
                    } else {
                        requestReadStoragePermission();
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

    private void loadUserData() {
        // Obtener el userId de Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Si no hay usuario conectado, mostrar mensaje de error
            Toast.makeText(this, "No hay usuario conectado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el userId de Firebase
        String userId = currentUser.getUid();

        if (userId == null || userId.isEmpty()) {
            // Si el userId es nulo o vacío, mostrar error
            Toast.makeText(this, "ID de usuario no válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Realizar la consulta en la base de datos local usando el userId
        SQLiteDatabase db = new FavoritesDatabaseHelper(this).getReadableDatabase();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                new String[]{
                        FavoritesDatabaseHelper.COLUMN_USER_ID,
                        FavoritesDatabaseHelper.COLUMN_NAME,
                        FavoritesDatabaseHelper.COLUMN_EMAIL,
                        FavoritesDatabaseHelper.COLUMN_ADDRESS,
                        FavoritesDatabaseHelper.COLUMN_PHONE,
                        FavoritesDatabaseHelper.COLUMN_IMAGE
                },
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null, null, null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_NAME);
                    int emailIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_EMAIL);
                    int addressIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_ADDRESS);
                    int phoneIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_PHONE);
                    int imageIndex = cursor.getColumnIndex(FavoritesDatabaseHelper.COLUMN_IMAGE);

                    String name = cursor.getString(nameIndex);
                    String email = cursor.getString(emailIndex);
                    String address = cursor.getString(addressIndex);
                    String phone = cursor.getString(phoneIndex);
                    String image = cursor.getString(imageIndex);

                    // Cargar los datos en los campos
                    etName.setText(name != null ? name : "");
                    etEmail.setText(email != null ? email : "");
                    etAddress.setText(address != null ? address : "");
                    etPhone.setText(phone != null ? phone : "");

                    if (image != null && !image.isEmpty()) {
                        Picasso.get().load(image)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(ivProfileImage);
                    } else {
                        ivProfileImage.setImageResource(R.drawable.ic_launcher_foreground);
                    }
                } else {
                    Toast.makeText(this, "No se encontró el usuario en la base de datos.", Toast.LENGTH_SHORT).show();
                }
            } finally {
                cursor.close(); // Asegúrate de cerrar el cursor
            }
        }
    }


    private void saveUserDetails() {

        // 1) Leer datos de los EditText
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        Bitmap bitmap = ((BitmapDrawable) ivProfileImage.getDrawable()).getBitmap();
        String image = saveImageToInternalStorage(bitmap, "profile_image.png");

        // 2) Validaciones básicas
        if (email.isEmpty()) {
            Toast.makeText(this, "Debe indicar un email.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3) Obtener el userId de Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado en Firebase.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // 4) Obtener el nombre actual desde Firebase
        String currentUserName = currentUser.getDisplayName();

        // 5) Verificar si el nombre ha cambiado, si no ha cambiado no actualizamos en Firebase
        if (currentUserName != null && currentUserName.equals(name)) {
            // Si el nombre no cambia, no actualizamos el nombre en Firebase.
            currentUserName = null;  // No actualizamos el nombre
        }

        // 6) Construir los valores que queremos actualizar
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);



        // 7) Realizar UPDATE
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rowsUpdated = db.update(
                FavoritesDatabaseHelper.TABLE_USERS,
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );

        // 8) Si no existe todavía la fila para este userId, la insertamos
        if (rowsUpdated == 0) {
            values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
            long insertedId = db.insert(FavoritesDatabaseHelper.TABLE_USERS, null, values);
            if (insertedId != -1) {
                Toast.makeText(this, "Usuario insertado exitosamente.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al insertar el usuario.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Usuario actualizado exitosamente.", Toast.LENGTH_SHORT).show();
        }

        db.close();

        // Si el nombre cambió, lo actualizamos en Firebase también
        if (currentUserName != null) {
            updateUserNameInFirebase(name);
        }

    }


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

    // Método para actualizar el nombre en Firebase si ha cambiado
    private void updateUserNameInFirebase(String newName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            currentUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("SaveUserDetails", "Nombre actualizado en Firebase");
                        } else {
                            Log.e("SaveUserDetails", "Error al actualizar el nombre en Firebase");
                        }
                    });
        }
    }


}
