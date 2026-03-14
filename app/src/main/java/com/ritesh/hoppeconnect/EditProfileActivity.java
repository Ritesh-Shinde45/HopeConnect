package com.ritesh.hoppeconnect;

import android.Manifest;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.ritesh.hoppeconnect.databinding.ActivityEditProfileBinding;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.appwrite.models.Document;
import io.appwrite.models.File;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";
    private static final String PREFS = "hoppe_prefs";

    private ActivityEditProfileBinding binding;
    private String currentUserId;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    private Uri cameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        currentUserId = prefs.getString("logged_in_user_id", null);

        if (currentUserId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        registerLaunchers();

        binding.ivBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveProfile());

        binding.ivProfile.setOnClickListener(v -> pickImage());

        loadProfile();
    }

    private void registerLaunchers() {

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadImage(uri);
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraUri != null) uploadImage(cameraUri);
                }
        );
    }

    private void pickImage() {

        String[] options = {"Camera", "Gallery"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Profile Photo")
                .setItems(options, (d, i) -> {

                    if (i == 0) {

                        if (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {

                            requestPermissions(
                                    new String[]{Manifest.permission.CAMERA}, 1);

                        } else {

                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.TITLE, "profile");

                            cameraUri = getContentResolver().insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    values);

                            cameraLauncher.launch(cameraUri);
                        }

                    } else {

                        galleryLauncher.launch("image/*");

                    }

                }).show();
    }

    private void uploadImage(Uri uri) {

        new Thread(() -> {

            try {

                InputStream is = getContentResolver().openInputStream(uri);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                byte[] buffer = new byte[8192];
                int read;

                while ((read = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }

                is.close();

                byte[] bytes = bos.toByteArray();

                Storage storage = AppwriteService.getStorage();

                String fileId = UUID.randomUUID().toString();

                File file = AppwriteHelper.uploadFileBlocking(
                        storage,
                        AppwriteService.USERS_BUCKET_ID,
                        fileId,
                        bytes,
                        "profile.jpg",
                        "image/jpeg"
                );

                Map<String, Object> update = new HashMap<>();
                update.put("photoId", file.getId());

                AppwriteHelper.updateDocument(
                        AppwriteService.getDatabases(),
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        currentUserId,
                        update
                );

                runOnUiThread(() -> {

                    String url = AppwriteService.ENDPOINT
                            + "/storage/buckets/"
                            + AppwriteService.USERS_BUCKET_ID
                            + "/files/"
                            + file.getId()
                            + "/view?project="
                            + AppwriteService.PROJECT_ID;

                    Glide.with(this)
                            .load(url)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ivProfile);

                    Toast.makeText(this,
                            "Photo Updated", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {

                Log.e(TAG, "upload failed", e);

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Upload failed",
                                Toast.LENGTH_LONG).show());
            }

        }).start();
    }

    private void loadProfile() {

        new Thread(() -> {

            try {

                Databases db = AppwriteService.getDatabases();

                Document<?> doc = AppwriteHelper.getDocument(
                        db,
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        currentUserId
                );

                Map<String, Object> data =
                        (Map<String, Object>) doc.getData();

                runOnUiThread(() -> {

                    setText(binding.etFullName, data, "name");
                    setText(binding.etEmail, data, "email");
                    setText(binding.etPhone, data, "mobile");
                    setText(binding.etLocation, data, "address");
                    setText(binding.etBio, data, "bio");

                    Object photoId = data.get("photoId");

                    if (photoId != null) {

                        String url = AppwriteService.ENDPOINT
                                + "/storage/buckets/"
                                + AppwriteService.USERS_BUCKET_ID
                                + "/files/"
                                + photoId
                                + "/view?project="
                                + AppwriteService.PROJECT_ID;

                        Glide.with(this)
                                .load(url)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(binding.ivProfile);
                    }

                });

            } catch (Exception e) {

                Log.e(TAG, "load error", e);

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Load failed",
                                Toast.LENGTH_LONG).show());
            }

        }).start();
    }

    private void saveProfile() {

        String name = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();
        String bio = binding.etBio.getText().toString().trim();

        Map<String, Object> update = new HashMap<>();

        update.put("name", name);
        update.put("email", email);
        update.put("mobile", phone);
        update.put("address", location);
        update.put("bio", bio);

        new Thread(() -> {

            try {

                AppwriteHelper.updateDocument(
                        AppwriteService.getDatabases(),
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        currentUserId,
                        update
                );

                runOnUiThread(() -> {

                    Toast.makeText(this,
                            "Profile updated",
                            Toast.LENGTH_SHORT).show();

                    finish();

                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_LONG).show());

            }

        }).start();
    }

    private void setText(
            com.google.android.material.textfield.TextInputEditText view,
            Map<String, Object> data,
            String key
    ) {

        Object val = data.get(key);

        if (val != null) view.setText(val.toString());
    }
}