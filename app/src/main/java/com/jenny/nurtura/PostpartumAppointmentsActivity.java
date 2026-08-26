package com.jenny.nurtura;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostpartumAppointmentsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextInputEditText typeEditText;
    private TextInputEditText providerEditText;
    private TextInputEditText notesEditText;

    private MaterialButton dateButton;
    private MaterialButton timeButton;

    private LinearLayout appointmentsContainer;
    private TextView noAppointmentsTextView;

    private Calendar appointmentCalendar;
    private boolean dateSelected;
    private boolean timeSelected;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
            );

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_postpartum_appointments
        );

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        appointmentCalendar = Calendar.getInstance();

        typeEditText = findViewById(
                R.id.appointmentTypeEditText
        );

        providerEditText = findViewById(
                R.id.appointmentProviderEditText
        );

        notesEditText = findViewById(
                R.id.appointmentNotesEditText
        );

        dateButton = findViewById(
                R.id.selectDateButton
        );

        timeButton = findViewById(
                R.id.selectTimeButton
        );

        appointmentsContainer = findViewById(
                R.id.appointmentsContainer
        );

        noAppointmentsTextView = findViewById(
                R.id.noAppointmentsTextView
        );

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        dateButton.setOnClickListener(
                view -> showDatePicker()
        );

        timeButton.setOnClickListener(
                view -> showTimePicker()
        );

        findViewById(R.id.saveAppointmentButton)
                .setOnClickListener(
                        view -> saveAppointment()
                );

        loadAppointments();
    }

    private void showDatePicker() {
        Calendar initialDate = dateSelected
                ? appointmentCalendar
                : Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {
                            appointmentCalendar.set(
                                    Calendar.YEAR,
                                    year
                            );

                            appointmentCalendar.set(
                                    Calendar.MONTH,
                                    month
                            );

                            appointmentCalendar.set(
                                    Calendar.DAY_OF_MONTH,
                                    dayOfMonth
                            );

                            dateSelected = true;

                            dateButton.setText(
                                    dateFormat.format(
                                            appointmentCalendar.getTime()
                                    )
                            );
                        },
                        initialDate.get(Calendar.YEAR),
                        initialDate.get(Calendar.MONTH),
                        initialDate.get(Calendar.DAY_OF_MONTH)
                );

        dialog.getDatePicker().setMinDate(
                System.currentTimeMillis() - 1000
        );

        dialog.show();
    }

    private void showTimePicker() {
        Calendar initialTime = timeSelected
                ? appointmentCalendar
                : Calendar.getInstance();

        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    appointmentCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                    );

                    appointmentCalendar.set(
                            Calendar.MINUTE,
                            minute
                    );

                    appointmentCalendar.set(
                            Calendar.SECOND,
                            0
                    );

                    appointmentCalendar.set(
                            Calendar.MILLISECOND,
                            0
                    );

                    timeSelected = true;

                    timeButton.setText(
                            timeFormat.format(
                                    appointmentCalendar.getTime()
                            )
                    );
                },
                initialTime.get(Calendar.HOUR_OF_DAY),
                initialTime.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void saveAppointment() {
        String type = getTextFromField(typeEditText);
        String provider = getTextFromField(providerEditText);
        String notes = getTextFromField(notesEditText);

        if (TextUtils.isEmpty(type)
                || TextUtils.isEmpty(provider)
                || !dateSelected
                || !timeSelected) {
            Toast.makeText(
                    this,
                    R.string.complete_appointment_details,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String appointmentDate =
                dateFormat.format(
                        appointmentCalendar.getTime()
                );

        String appointmentTime =
                timeFormat.format(
                        appointmentCalendar.getTime()
                );

        long appointmentAtMillis =
                appointmentCalendar.getTimeInMillis();

        Map<String, Object> appointment =
                new HashMap<>();

        appointment.put("type", type);
        appointment.put("provider", provider);
        appointment.put("notes", notes);
        appointment.put("appointmentDate", appointmentDate);
        appointment.put("appointmentTime", appointmentTime);
        appointment.put(
                "appointmentAtMillis",
                appointmentAtMillis
        );
        appointment.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumAppointments")
                .add(appointment)
                .addOnSuccessListener(
                        documentReference -> {
                            Toast.makeText(
                                    PostpartumAppointmentsActivity.this,
                                    R.string.postpartum_appointment_saved,
                                    Toast.LENGTH_SHORT
                            ).show();

                            clearForm();
                            loadAppointments();
                        }
                )
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        PostpartumAppointmentsActivity.this,
                                        R.string.unable_to_save_appointment,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void loadAppointments() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumAppointments")
                .orderBy(
                        "appointmentAtMillis",
                        Query.Direction.ASCENDING
                )
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    appointmentsContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        noAppointmentsTextView.setVisibility(
                                View.VISIBLE
                        );

                        return;
                    }

                    noAppointmentsTextView.setVisibility(
                            View.GONE
                    );

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {
                        addAppointmentCard(document);
                    }
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        PostpartumAppointmentsActivity.this,
                                        R.string.unable_to_load_appointments,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void addAppointmentCard(
            DocumentSnapshot document
    ) {
        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                dpToPixels(8),
                0,
                dpToPixels(12)
        );

        card.setLayoutParams(cardParams);
        card.setRadius(dpToPixels(16));
        card.setCardElevation(dpToPixels(2));

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(18),
                dpToPixels(16),
                dpToPixels(18),
                dpToPixels(16)
        );

        String appointmentDate =
                getDocumentString(
                        document,
                        "appointmentDate"
                );

        String appointmentTime =
                getDocumentString(
                        document,
                        "appointmentTime"
                );

        TextView dateTimeText = createTextView(
                getString(
                        R.string.postpartum_appointment_date_time,
                        appointmentDate,
                        appointmentTime
                ),
                17,
                true
        );

        content.addView(dateTimeText);

        addDetailText(
                content,
                getString(
                        R.string.postpartum_appointment_type_value,
                        getDocumentString(document, "type")
                )
        );

        addDetailText(
                content,
                getString(
                        R.string.postpartum_appointment_provider_value,
                        getDocumentString(document, "provider")
                )
        );

        String notes =
                getDocumentString(document, "notes");

        if (!TextUtils.isEmpty(notes)) {
            addDetailText(
                    content,
                    getString(
                            R.string.postpartum_appointment_notes_value,
                            notes
                    )
            );
        }

        MaterialButton deleteButton =
                new MaterialButton(this);

        deleteButton.setText(R.string.delete);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.gravity = Gravity.END;
        buttonParams.topMargin = dpToPixels(8);
        deleteButton.setLayoutParams(buttonParams);

        deleteButton.setOnClickListener(
                view -> confirmDelete(document.getId())
        );

        content.addView(deleteButton);
        card.addView(content);
        appointmentsContainer.addView(card);
    }

    private void addDetailText(
            LinearLayout container,
            String text
    ) {
        TextView textView = createTextView(
                text,
                14,
                false
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = dpToPixels(6);
        textView.setLayoutParams(params);
        container.addView(textView);
    }

    private TextView createTextView(
            String text,
            int textSize,
            boolean bold
    ) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);

        if (bold) {
            textView.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return textView;
    }

    private void confirmDelete(String documentId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) ->
                                deleteAppointment(documentId)
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void deleteAppointment(String documentId) {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumAppointments")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            PostpartumAppointmentsActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadAppointments();
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        PostpartumAppointmentsActivity.this,
                                        R.string.unable_to_delete,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private String getTextFromField(
            TextInputEditText field
    ) {
        if (field.getText() == null) {
            return "";
        }

        return field.getText()
                .toString()
                .trim();
    }

    private String getDocumentString(
            DocumentSnapshot document,
            String fieldName
    ) {
        String value = document.getString(fieldName);
        return value == null ? "" : value;
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void clearForm() {
        typeEditText.setText("");
        providerEditText.setText("");
        notesEditText.setText("");

        dateSelected = false;
        timeSelected = false;
        appointmentCalendar = Calendar.getInstance();

        dateButton.setText(
                R.string.postpartum_select_date
        );

        timeButton.setText(
                R.string.postpartum_select_time
        );
    }

    private void openLoginActivity() {
        Intent intent = new Intent(
                PostpartumAppointmentsActivity.this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}