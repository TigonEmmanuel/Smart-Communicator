package com.example.smartcommunicator.ui.contact;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartcommunicator.R;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactFragment extends Fragment {

    // --- UI Elements ---
    private TextInputEditText editTextName;
    private TextInputEditText editTextPhone;
    private ImageButton buttonScanCamera;
    private Button buttonSave;
    private Button buttonDial;

    // --- Result Launchers for Permissions and Activities ---

    // Launcher for "Write Contacts" permission
    private final ActivityResultLauncher<String> requestWriteContactsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveContact(); // Permission granted, try saving again
                } else {
                    Toast.makeText(getContext(), "Permission to write contacts denied.", Toast.LENGTH_SHORT).show();
                }
            });

    // NEW: Launcher for "Camera" permission
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera(); // Permission granted, launch the camera
                } else {
                    Toast.makeText(getContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    // NEW: Launcher to get the result (picture) from the camera
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    // Image was captured successfully
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            // We have the image, now process it with ML Kit
                            processImageWithMLKit(imageBitmap);
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contact, container, false);
        // Find all views
        editTextName = root.findViewById(R.id.edit_text_name);
        editTextPhone = root.findViewById(R.id.edit_text_phone);
        buttonScanCamera = root.findViewById(R.id.button_scan_camera);
        buttonSave = root.findViewById(R.id.button_save);
        buttonDial = root.findViewById(R.id.button_dial);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Set up all button listeners ---
        buttonSave.setOnClickListener(v -> saveContact());
        buttonDial.setOnClickListener(v -> dialNumber());
        buttonScanCamera.setOnClickListener(v -> handleCameraScanClick()); // Updated listener
    }

    // --- Core Methods for Features ---

    private void saveContact() {
        // (This method remains the same as before)
        String name = editTextName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
            startActivity(intent);
            editTextName.setText("");
            editTextPhone.setText("");
        } else {
            requestWriteContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS);
        }
    }

    private void dialNumber() {
        // (This method remains the same as before)
        String phone = editTextPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            Toast.makeText(getContext(), "Phone number is empty", Toast.LENGTH_SHORT).show();
        } else {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + phone));
            startActivity(dialIntent);
        }
    }

    // NEW: Method called when camera icon is clicked
    private void handleCameraScanClick() {
        // Check if we already have permission to use the camera
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera(); // We have permission, open the camera
        } else {
            // We don't have permission, so request it.
            // The result is handled by 'requestCameraPermissionLauncher'.
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // NEW: Method to open the device's camera app
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // The 'cameraLauncher' will handle the result of this action.
        cameraLauncher.launch(takePictureIntent);
    }

    // NEW: Method to process the captured image using ML Kit
    private void processImageWithMLKit(Bitmap bitmap) {
        Toast.makeText(getContext(), "Analyzing image...", Toast.LENGTH_SHORT).show();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        Task<Text> result = recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // ML Kit processing was successful
                    extractPhoneNumber(visionText.getText());
                })
                .addOnFailureListener(e -> {
                    // ML Kit processing failed
                    Toast.makeText(getContext(), "Text recognition failed.", Toast.LENGTH_SHORT).show();
                });
    }

    // NEW: Method to find a phone number within the recognized text
    private void extractPhoneNumber(String text) {
        // This is a simple regex to find sequences of 7 or more digits, possibly with spaces, dashes, or parentheses.
        Pattern pattern = Pattern.compile("(?:\\+?(\\d{1,3}))?[-.\\s]?\\(?(\\d{3})\\)?[-.\\s]?(\\d{3})[-.\\s]?(\\d{4,})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            // A potential phone number was found
            String phoneNumber = matcher.group(0).replaceAll("[^0-9+]", ""); // Clean up the found number
            editTextPhone.setText(phoneNumber); // Set the number in the EditText
            Toast.makeText(getContext(), "Phone number found!", Toast.LENGTH_SHORT).show();
        } else {
            // No number was found in the text
            Toast.makeText(getContext(), "No phone number detected in the image.", Toast.LENGTH_LONG).show();
        }
    }
}
