package com.jenny.nurtura;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

public class RecoveryCheckInActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private MaterialButton selectDateButton;
    private TextInputEditText weekEditText;
    private TextInputEditText symptomsEditText;
    private TextInputEditText notesEditText;

    private Spinner deliverySpinner;
    private Spinner bleedingSpinner;
    private Spinner painSpinner;
    private Spinner feedingSpinner;
    private Spinner moodSpinner;

    private TextView latestRecoveryTextView;
    private TextView noEntriesTextView;
    private LinearLayout entriesContainer;

    private final Calendar selectedDate = Calendar.getInstance();
    private boolean dateSelected = false;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery_check_in);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        selectDateButton =
                findViewById(R.id.selectRecoveryDateButton);

        weekEditText =
                findViewById(R.id.postpartumWeekEditText);

        symptomsEditText =
                findViewById(R.id.recoverySymptomsEditText);

        notesEditText =
                findViewById(R.id.recoveryNotesEditText);

        deliverySpinner =
                findViewById(R.id.deliveryTypeSpinner);

        bleedingSpinner =
                findViewById(R.id.bleedingLevelSpinner);

        painSpinner =
                findViewById(R.id.painLevelSpinner);

        feedingSpinner =
                findViewById(R.id.feedingMethodSpinner);

        moodSpinner =
                findViewById(R.id.recoveryMoodSpinner);

        latestRecoveryTextView =
                findViewById(R.id.latestRecoveryTextView);

        noEntriesTextView =
                findViewById(R.id.noRecoveryEntriesTextView);

        entriesContainer =
                findViewById(R.id.recoveryEntriesContainer);

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        selectDateButton.setOnClickListener(
                view -> showDatePicker()
        );

        findViewById(R.id.saveRecoveryButton)
                .setOnClickListener(
                        view -> saveRecoveryCheckIn()
                );

        loadRecoveryEntries();
    }

    private void showDatePicker() {
        Calendar today = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (datePicker, year, month, day) -> {
                            selectedDate.set(
                                    year,
                                    month,
                                    day,
                                    0,
                                    0,
                                    0
                            );

                            selectedDate.set(
                                    Calendar.MILLISECOND,
                                    0
                            );

                            dateSelected = true;

                            selectDateButton.setText(
                                    dateFormat.format(
                                            selectedDate.getTime()
                                    )
                            );
                        },
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH)
                );

        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis()
        );

        dialog.show();
    }

    private void saveRecoveryCheckIn() {
        String weekText =
                getTextFromField(weekEditText);

        String symptoms =
                getTextFromField(symptomsEditText);

        String notes =
                getTextFromField(notesEditText);

        if (!dateSelected
                || TextUtils.isEmpty(weekText)
                || deliverySpinner.getSelectedItemPosition() == 0
                || bleedingSpinner.getSelectedItemPosition() == 0
                || painSpinner.getSelectedItemPosition() == 0
                || feedingSpinner.getSelectedItemPosition() == 0
                || moodSpinner.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    R.string.complete_recovery_fields,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int postpartumWeek;

        try {
            postpartumWeek =
                    Integer.parseInt(weekText);
        } catch (NumberFormatException exception) {
            showInvalidWeekMessage();
            return;
        }

        if (postpartumWeek < 1
                || postpartumWeek > 52) {
            showInvalidWeekMessage();
            return;
        }

        String deliveryType =
                deliverySpinner
                        .getSelectedItem()
                        .toString();

        String bleedingLevel =
                bleedingSpinner
                        .getSelectedItem()
                        .toString();

        String painLevel =
                painSpinner
                        .getSelectedItem()
                        .toString();

        String feedingMethod =
                feedingSpinner
                        .getSelectedItem()
                        .toString();

        String mood =
                moodSpinner
                        .getSelectedItem()
                        .toString();

        Map<String, Object> entry =
                new HashMap<>();

        entry.put(
                "checkInDate",
                dateFormat.format(
                        selectedDate.getTime()
                )
        );

        entry.put(
                "checkInDateMillis",
                selectedDate.getTimeInMillis()
        );

        entry.put(
                "postpartumWeek",
                postpartumWeek
        );

        entry.put(
                "deliveryType",
                deliveryType
        );

        entry.put(
                "bleedingLevel",
                bleedingLevel
        );

        entry.put(
                "painLevel",
                painLevel
        );

        entry.put(
                "feedingMethod",
                feedingMethod
        );

        entry.put(
                "mood",
                mood
        );

        entry.put(
                "symptoms",
                symptoms
        );

        entry.put(
                "notes",
                notes
        );

        entry.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        boolean needsSafetyMessage =
                bleedingLevel.equals("Heavy")
                        || painLevel.equals("Severe");

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumEntries")
                .add(entry)
                .addOnSuccessListener(
                        documentReference -> {
                            Toast.makeText(
                                    RecoveryCheckInActivity.this,
                                    R.string.recovery_saved,
                                    Toast.LENGTH_SHORT
                            ).show();

                            clearForm();
                            loadRecoveryEntries();

                            if (needsSafetyMessage) {
                                showSafetyMessage();
                            }
                        }
                )
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        RecoveryCheckInActivity.this,
                                        R.string.unable_to_save_recovery,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void loadRecoveryEntries() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumEntries")
                .orderBy(
                        "checkInDateMillis",
                        Query.Direction.DESCENDING
                )
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    entriesContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        noEntriesTextView.setVisibility(
                                View.VISIBLE
                        );

                        latestRecoveryTextView.setText(
                                R.string.not_recorded
                        );

                        return;
                    }

                    noEntriesTextView.setVisibility(
                            View.GONE
                    );

                    boolean firstEntry = true;

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        if (firstEntry) {
                            latestRecoveryTextView.setText(
                                    createEntrySummary(document)
                            );

                            firstEntry = false;
                        }

                        addEntryCard(document);
                    }
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        RecoveryCheckInActivity.this,
                                        R.string.unable_to_load_recovery,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private String createEntrySummary(
            DocumentSnapshot document
    ) {
        String date =
                getDocumentString(
                        document,
                        "checkInDate"
                );

        int week =
                getDocumentInteger(
                        document,
                        "postpartumWeek"
                );

        String delivery =
                getDocumentString(
                        document,
                        "deliveryType"
                );

        String bleeding =
                getDocumentString(
                        document,
                        "bleedingLevel"
                );

        String pain =
                getDocumentString(
                        document,
                        "painLevel"
                );

        String feeding =
                getDocumentString(
                        document,
                        "feedingMethod"
                );

        String mood =
                getDocumentString(
                        document,
                        "mood"
                );

        StringBuilder summary =
                new StringBuilder();

        summary.append(
                getString(
                        R.string.recovery_date_week_value,
                        date,
                        week
                )
        );

        summary.append("\n")
                .append(
                        getString(
                                R.string.delivery_value,
                                delivery
                        )
                );

        summary.append("\n")
                .append(
                        getString(
                                R.string.bleeding_value,
                                bleeding
                        )
                );

        summary.append("\n")
                .append(
                        getString(
                                R.string.pain_value,
                                pain
                        )
                );

        summary.append("\n")
                .append(
                        getString(
                                R.string.feeding_value,
                                feeding
                        )
                );

        summary.append("\n")
                .append(
                        getString(
                                R.string.mood_value,
                                mood
                        )
                );

        return summary.toString();
    }

    private void addEntryCard(
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

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dpToPixels(18),
                dpToPixels(16),
                dpToPixels(18),
                dpToPixels(16)
        );

        String date =
                getDocumentString(
                        document,
                        "checkInDate"
                );

        int week =
                getDocumentInteger(
                        document,
                        "postpartumWeek"
                );

        TextView heading =
                createTextView(
                        getString(
                                R.string.recovery_date_week_value,
                                date,
                                week
                        ),
                        17,
                        true
                );

        content.addView(heading);

        addDetailText(
                content,
                getString(
                        R.string.delivery_value,
                        getDocumentString(
                                document,
                                "deliveryType"
                        )
                )
        );

        addDetailText(
                content,
                getString(
                        R.string.bleeding_value,
                        getDocumentString(
                                document,
                                "bleedingLevel"
                        )
                )
        );

        addDetailText(
                content,
                getString(
                        R.string.pain_value,
                        getDocumentString(
                                document,
                                "painLevel"
                        )
                )
        );

        addDetailText(
                content,
                getString(
                        R.string.feeding_value,
                        getDocumentString(
                                document,
                                "feedingMethod"
                        )
                )
        );

        addDetailText(
                content,
                getString(
                        R.string.mood_value,
                        getDocumentString(
                                document,
                                "mood"
                        )
                )
        );

        String symptoms =
                getDocumentString(
                        document,
                        "symptoms"
                );

        if (!TextUtils.isEmpty(symptoms)) {
            addDetailText(
                    content,
                    getString(
                            R.string.symptoms_value,
                            symptoms
                    )
            );
        }

        String notes =
                getDocumentString(
                        document,
                        "notes"
                );

        if (!TextUtils.isEmpty(notes)) {
            addDetailText(
                    content,
                    getString(
                            R.string.notes_value,
                            notes
                    )
            );
        }

        MaterialButton deleteButton =
                new MaterialButton(this);

        deleteButton.setText(
                R.string.delete
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.gravity = Gravity.END;
        buttonParams.topMargin = dpToPixels(8);

        deleteButton.setLayoutParams(
                buttonParams
        );

        deleteButton.setOnClickListener(
                view -> confirmDelete(
                        document.getId()
                )
        );

        content.addView(deleteButton);
        card.addView(content);
        entriesContainer.addView(card);
    }

    private void addDetailText(
            LinearLayout container,
            String text
    ) {
        TextView textView =
                createTextView(
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
        TextView textView =
                new TextView(this);

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

    private void confirmDelete(
            String documentId
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.delete_record_title
                )
                .setMessage(
                        R.string.delete_record_message
                )
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) ->
                                deleteEntry(documentId)
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void deleteEntry(
            String documentId
    ) {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumEntries")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            RecoveryCheckInActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadRecoveryEntries();
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        RecoveryCheckInActivity.this,
                                        R.string.unable_to_delete,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void showSafetyMessage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.recovery_safety_title
                )
                .setMessage(
                        R.string.recovery_safety_message
                )
                .setPositiveButton(
                        R.string.understood,
                        null
                )
                .show();
    }

    private void showInvalidWeekMessage() {
        Toast.makeText(
                this,
                R.string.invalid_postpartum_week,
                Toast.LENGTH_SHORT
        ).show();
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
        String value =
                document.getString(fieldName);

        if (value == null) {
            return "";
        }

        return value;
    }

    private int getDocumentInteger(
            DocumentSnapshot document,
            String fieldName
    ) {
        Long value =
                document.getLong(fieldName);

        if (value == null) {
            return 0;
        }

        return value.intValue();
    }

    private int dpToPixels(
            int dp
    ) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void clearForm() {
        dateSelected = false;

        selectDateButton.setText(
                R.string.select_recovery_date
        );

        weekEditText.setText("");
        symptomsEditText.setText("");
        notesEditText.setText("");

        deliverySpinner.setSelection(0);
        bleedingSpinner.setSelection(0);
        painSpinner.setSelection(0);
        feedingSpinner.setSelection(0);
        moodSpinner.setSelection(0);
    }

    private void openLoginActivity() {
        Intent intent =
                new Intent(
                        RecoveryCheckInActivity.this,
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