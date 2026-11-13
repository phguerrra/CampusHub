package com.example.application;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";
    private TextView titleText;
    private TextInputLayout nameLayout;
    private TextInputLayout categoryLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout timeLayout;
    private TextInputLayout locationLayout;
    private TextInputLayout slotsLayout;

    private TextInputEditText nameInput;
    private AutoCompleteTextView categoryInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText dateInput;
    private TextInputEditText timeInput;
    private TextInputEditText locationInput;
    private TextInputEditText slotsInput;
    private TextView errorText;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private ProgressBar progress;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private Calendar selectedDate;
    private String selectedTime;
    private String eventIdToEdit = null;
    private String existingCreatedBy = null;

    private ImageView imagePreview;
    private MaterialButton selectImageButton;
    private String selectedCoverPath = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String savedPath = copyUriToLocalDirectory(uri, "covers", "cover_", ".jpg");
                        if (savedPath != null) {
                            selectedCoverPath = savedPath;
                            displayCoverImage(savedPath);
                        }
                    }
                }
        );

        bindViews();
        setupCategoryDropdown();
        setupActions();

        if (FirebaseApp.getApps(this).isEmpty()) {
            showError(getString(R.string.firebase_not_configured));
            saveButton.setEnabled(false);
            return;
        }
        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        if (getIntent() != null && getIntent().hasExtra(EventsActivity.EXTRA_EVENT_ID)) {
            eventIdToEdit = getIntent().getStringExtra(EventsActivity.EXTRA_EVENT_ID);
            if (eventIdToEdit != null && !eventIdToEdit.trim().isEmpty()) {
                titleText.setText(R.string.edit_event_title);
                loadEventForEdit();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth != null && auth.getCurrentUser() == null) {
            openLogin();
        }
    }

    private void bindViews() {
        titleText = findViewById(R.id.create_event_title_text);
        imagePreview = findViewById(R.id.create_event_image_preview);
        selectImageButton = findViewById(R.id.select_cover_image_button);

        nameLayout = findViewById(R.id.create_event_name_layout);
        categoryLayout = findViewById(R.id.create_event_category_layout);
        descriptionLayout = findViewById(R.id.create_event_description_layout);
        dateLayout = findViewById(R.id.create_event_date_layout);
        timeLayout = findViewById(R.id.create_event_time_layout);
        locationLayout = findViewById(R.id.create_event_location_layout);
        slotsLayout = findViewById(R.id.create_event_slots_layout);

        nameInput = findViewById(R.id.create_event_name_input);
        categoryInput = findViewById(R.id.create_event_category_input);
        descriptionInput = findViewById(R.id.create_event_description_input);
        dateInput = findViewById(R.id.create_event_date_input);
        timeInput = findViewById(R.id.create_event_time_input);
        locationInput = findViewById(R.id.create_event_location_input);
        slotsInput = findViewById(R.id.create_event_slots_input);

        errorText = findViewById(R.id.create_event_error);
        saveButton = findViewById(R.id.save_event_button);
        cancelButton = findViewById(R.id.cancel_create_event_button);
        progress = findViewById(R.id.create_event_progress);
    }

    private String copyUriToLocalDirectory(Uri uri, String subDir, String prefix, String extension) {
        try {
            File dir = new File(getFilesDir(), subDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File outFile = new File(dir, prefix + System.currentTimeMillis() + extension);
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                if (is == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao copiar imagem de capa local", e);
            return null;
        }
    }

    private void displayCoverImage(String path) {
        if (path != null && imagePreview != null) {
            try {
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    // Reservado para URLs remotas
                } else {
                    imagePreview.setImageURI(Uri.fromFile(new File(path)));
                    if (selectImageButton != null) {
                        selectImageButton.setText(R.string.change_cover_image);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao exibir imagem de capa", e);
            }
        }
    }

    private void loadEventForEdit() {
        if (database == null || eventIdToEdit == null) return;

        setLoading(true);
        database.collection("events")
                .document(eventIdToEdit)
                .get()
                .addOnSuccessListener(document -> {
                    setLoading(false);
                    if (!document.exists()) {
                        Toast.makeText(this, R.string.event_details_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Event event = document.toObject(Event.class);
                    if (event != null) {
                        existingCreatedBy = event.getCreatedBy();
                        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

                        if (!Objects.equals(existingCreatedBy, currentUserId)) {
                            Toast.makeText(this, R.string.organizer_only_edit, Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        nameInput.setText(event.getName());
                        categoryInput.setText(event.getCategory(), false);
                        descriptionInput.setText(event.getDescription());
                        locationInput.setText(event.getLocation());
                        slotsInput.setText(String.format(Locale.getDefault(), "%d", event.getAvailableSlots()));

                        if (event.getDate() != null) {
                            selectedDate = Calendar.getInstance();
                            selectedDate.setTime(event.getDate().toDate());
                            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            dateInput.setText(formatter.format(selectedDate.getTime()));
                        }

                        if (event.getTime() != null) {
                            selectedTime = event.getTime();
                            timeInput.setText(selectedTime);
                        }

                        if (event.getImageUrl() != null && !event.getImageUrl().trim().isEmpty()) {
                            selectedCoverPath = event.getImageUrl().trim();
                            displayCoverImage(selectedCoverPath);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(getString(R.string.event_details_load_error));
                });
    }

    private void setupCategoryDropdown() {
        String[] categories = new String[]{
                getString(R.string.category_academic),
                getString(R.string.category_sports),
                getString(R.string.category_tech),
                getString(R.string.category_cultural),
                getString(R.string.category_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        categoryInput.setAdapter(adapter);
    }

    private void setupActions() {
        if (selectImageButton != null) {
            selectImageButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }
        dateInput.setOnClickListener(view -> showDatePicker());
        timeInput.setOnClickListener(view -> showTimePicker());
        saveButton.setOnClickListener(view -> saveEvent());
        cancelButton.setOnClickListener(view -> finish());
    }

    private void showDatePicker() {
        Calendar initial = selectedDate == null ? Calendar.getInstance() : selectedDate;
        new DatePickerDialog(
                this,
                (picker, year, month, day) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.clear();
                    selectedDate.set(year, month, day, 0, 0, 0);
                    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    dateInput.setText(formatter.format(selectedDate.getTime()));
                    dateLayout.setError(null);
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(
                this,
                (picker, hour, minute) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    timeInput.setText(selectedTime);
                    timeLayout.setError(null);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void saveEvent() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        clearErrors();
        String name = textOf(nameInput).trim();
        String category = categoryInput.getText() != null ? categoryInput.getText().toString().trim() : "";
        String description = textOf(descriptionInput).trim();
        String location = textOf(locationInput).trim();
        String slotsText = textOf(slotsInput).trim();

        boolean valid = true;
        if (name.length() < 2) {
            nameLayout.setError(getString(R.string.event_name_required));
            valid = false;
        }
        if (category.isEmpty()) {
            categoryLayout.setError(getString(R.string.event_category_required));
            valid = false;
        }
        if (description.length() < 10) {
            descriptionLayout.setError(getString(R.string.event_description_required));
            valid = false;
        }
        if (selectedDate == null) {
            String dateStr = textOf(dateInput).trim();
            if (!dateStr.isEmpty()) {
                try {
                    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    java.util.Date parsed = formatter.parse(dateStr);
                    if (parsed != null) {
                        selectedDate = Calendar.getInstance();
                        selectedDate.setTime(parsed);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (selectedTime == null) {
            String timeStr = textOf(timeInput).trim();
            if (!timeStr.isEmpty()) {
                selectedTime = timeStr;
            }
        }

        if (selectedDate == null) {
            dateLayout.setError(getString(R.string.event_date_required));
            valid = false;
        }
        if (selectedTime == null) {
            timeLayout.setError(getString(R.string.event_time_required));
            valid = false;
        }
        if (location.length() < 2) {
            locationLayout.setError(getString(R.string.event_location_required));
            valid = false;
        }

        long availableSlots = parsePositiveLong(slotsText);
        if (availableSlots <= 0) {
            slotsLayout.setError(getString(R.string.event_slots_required));
            valid = false;
        }

        if (!valid) {
            return;
        }

        geocodeAndSaveEvent(name, category, description, location, availableSlots);
    }

    private void geocodeAndSaveEvent(String name, String category, String description,
                                     String location, long availableSlots) {
        setLoading(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            double lat = 0.0;
            double lng = 0.0;
            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(CreateEventActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(location, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        lat = address.getLatitude();
                        lng = address.getLongitude();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao geocodificar endereço: " + location, e);
            }

            final double finalLat = lat;
            final double finalLng = lng;

            runOnUiThread(() -> {
                if (auth == null || auth.getCurrentUser() == null) {
                    setLoading(false);
                    openLogin();
                    return;
                }

                Map<String, Object> event = new HashMap<>();
                event.put("name", name);
                event.put("category", category);
                event.put("description", description);
                event.put("date", new Timestamp(selectedDate.getTime()));
                event.put("time", selectedTime);
                event.put("location", location);
                event.put("latitude", finalLat);
                event.put("longitude", finalLng);
                event.put("availableSlots", availableSlots);
                event.put("maxCapacity", availableSlots);
                if (selectedCoverPath != null && !selectedCoverPath.trim().isEmpty()) {
                    event.put("imageUrl", selectedCoverPath.trim());
                }

                if (eventIdToEdit != null) {
                    database.collection("events")
                            .document(eventIdToEdit)
                            .set(event, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                Toast.makeText(CreateEventActivity.this, R.string.event_updated, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(exception -> {
                                setLoading(false);
                                Log.e(TAG, "Falha ao atualizar evento no Firestore", exception);
                                showError(firestoreErrorMessage(exception));
                            });
                } else {
                    event.put("createdBy", auth.getCurrentUser().getUid());
                    event.put("status", Event.STATUS_PUBLISHED);
                    event.put("createdAt", FieldValue.serverTimestamp());

                    database.collection("events")
                            .add(event)
                            .addOnSuccessListener(document -> {
                                setLoading(false);
                                Toast.makeText(CreateEventActivity.this, R.string.event_created, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(exception -> {
                                setLoading(false);
                                Log.e(TAG, "Falha ao salvar evento no Firestore", exception);
                                showError(firestoreErrorMessage(exception));
                            });
                }
            });
            executor.shutdown();
        });
    }

    private String firestoreErrorMessage(Exception exception) {
        if (!(exception instanceof FirebaseFirestoreException)) {
            String msg = exception.getMessage();
            return msg != null ? msg : getString(R.string.event_create_error);
        }

        FirebaseFirestoreException firestoreException =
                (FirebaseFirestoreException) exception;
        switch (firestoreException.getCode()) {
            case PERMISSION_DENIED:
            case UNAUTHENTICATED:
                return getString(R.string.firestore_permission_denied);
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
                return getString(R.string.firestore_unavailable);
            default:
                String details = firestoreException.getMessage();
                return getString(
                        R.string.firestore_error_with_code,
                        firestoreException.getCode().name()
                ) + (details != null ? " (" + details + ")" : "");
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
        nameInput.setEnabled(!loading);
        categoryInput.setEnabled(!loading);
        descriptionInput.setEnabled(!loading);
        dateInput.setEnabled(!loading);
        timeInput.setEnabled(!loading);
        locationInput.setEnabled(!loading);
        slotsInput.setEnabled(!loading);
    }

    private void clearErrors() {
        errorText.setVisibility(View.GONE);
        nameLayout.setError(null);
        categoryLayout.setError(null);
        descriptionLayout.setError(null);
        dateLayout.setError(null);
        timeLayout.setError(null);
        locationLayout.setError(null);
        slotsLayout.setError(null);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static long parsePositiveLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
