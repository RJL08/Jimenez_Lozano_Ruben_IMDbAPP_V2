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
import com.example.jimenez_lozano_ruben_imdbapp.sync.UsersSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


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

            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) {
                Log.e("EditUser", "No hay usuario logueado.");
                return;
            }

            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String address = documentSnapshot.getString("address");
                            String phone = documentSnapshot.getString("phone");
                            String image = documentSnapshot.getString("image");

                            if (name != null) etName.setText(name);
                            if (email != null) etEmail.setText(email);
                            if (address != null) etAddress.setText(address);
                            if (phone != null) etPhone.setText(phone);
                            if (image != null && !image.isEmpty()) {
                                Picasso.get().load(image)
                                        .placeholder(R.drawable.ic_launcher_foreground) // Recurso placeholder
                                        .error(R.drawable.ic_launcher_foreground)             // Recurso en caso de error
                                        .into(ivProfileImage);
                            }
                        } else {
                            Log.d("EditUser", "El usuario no tiene un documento en Firestore.");
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("EditUser", "Error al cargar datos de usuario: " + e.getMessage(), e));
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

        // Crear un Map con los datos a guardar en Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("address", address);
        data.put("phone", phone);
        if (imagePath != null && !imagePath.isEmpty()) {
            data.put("image", imagePath);
        }

        // Actualizar (o crear) el documento de usuario en Firestore con merge para conservar datos existentes
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Datos Actualizados con Éxito", Toast.LENGTH_SHORT).show();
                    Log.d("EditUser", "Usuario actualizado en Firestore");
                    new UsersSync().syncFirestoreToLocal(this);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al actualizar datos", Toast.LENGTH_SHORT).show();
                    Log.e("EditUser", "Fallo al actualizar Firestore: " + e.getMessage(), e);
                });
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




}
