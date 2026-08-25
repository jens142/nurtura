package com.jenny.nurtura;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MaternalActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private MaterialButton checkInDateButton;
    private MaterialButton saveCheckInButton;

    private Spinner maternalStageSpinner;
    private Spinner moodSpinner;

    private EditText weekEditText;
    private EditText symptomsEditText;
    private EditText notesEditText;

    private CircularProgressIndicator progressIndicator;

    private TextView maternalStageTextView;
    private TextView latestCheckInTextView;
    private TextView emptyTextView;
    private LinearLayout entriesContainer;

    private long selectedDateMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maternal);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        checkInDateButton =
                findViewById(R.id.checkInDateButton);

        saveCheckInButton =
                findViewById(R.id.saveCheckInButton);

        maternalStageSpinner =
                findViewById(R.id.maternalStageSpinner);

        moodSpinner =
                findViewById(R.id.moodSpinner);

        weekEditText =
                findViewById(R.id.weekEditText);

        symptomsEditText =
                findViewById(
                        R.id.maternalSymptomsEditText
                );

        notesEditText =
                findViewById(
                        R.id.maternalNotesEditText
                );

        progressIndicator =
                findViewById(
                        R.id.maternalProgressIndicator
                );

        maternalStageTextView =
                findViewById(
                        R.id.maternalStageTextView
                );

        latestCheckInTextView =
                findViewById(
                        R.id.latestCheckInTextView
                );

        emptyTextView =
                findViewById(
                        R.id.maternalEmptyTextView
                );

        entriesContainer =
                findViewById(
                        R.id.maternalEntriesContainer
                );

        findViewById(R.id.maternalBackButton)
                .setOnClickListener(view -> finish());

        checkInDateButton.setOnClickListener(
                view -> showDatePicker()
        );

        saveCheckInButton.setOnClickListener(
                view -> saveCheckIn()
        );

        loadCheckIns();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        if (selectedDateMillis != -1) {
            calendar.setTimeInMillis(
                    selectedDateMillis
            );
        }

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (datePicker, year, month, day) -> {
                            Calendar selectedDate =
                                    Calendar.getInstance();

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

                            selectedDateMillis =
                                    selectedDate
                                            .getTimeInMillis();

                            checkInDateButton.setText(
                                    formatDate(
                                            selectedDateMillis
                                    )
                            );
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis()
        );

        dialog.show();
    }

    private void saveCheckIn() {
        if (maternalStageSpinner
                .getSelectedItemPosition() == 0) {

            showMessage("Select the care stage");
            return;
        }

        if (selectedDateMillis == -1) {
            showMessage("Select the check-in date");
            return;
        }

        String weekText = weekEditText
                .getText()
                .toString()
                .trim();

        if (weekText.isEmpty()) {
            showMessage("Enter the week number");
            return;
        }

        int weekNumber;

        try {
            weekNumber = Integer.parseInt(weekText);
        } catch (NumberFormatException exception) {
            showMessage("Enter a valid week number");
            return;
        }

        if (weekNumber < 1 || weekNumber > 52) {
            showMessage(
                    "Week number must be between 1 and 52"
            );
            return;
        }

        if (moodSpinner
                .getSelectedItemPosition() == 0) {

            showMessage("Select your mood");
            return;
        }

        String stage =
                maternalStageSpinner
                        .getSelectedItem()
                        .toString();

        String mood =
                moodSpinner
                        .getSelectedItem()
                        .toString();

        String symptoms =
                symptomsEditText
                        .getText()
                        .toString()
                        .trim();

        String notes =
                notesEditText
                        .getText()
                        .toString()
                        .trim();

        Map<String, Object> checkIn =
                new HashMap<>();

        checkIn.put("stage", stage);
        checkIn.put(
                "checkInDateMillis",
                selectedDateMillis
        );
        checkIn.put("weekNumber", weekNumber);
        checkIn.put("mood", mood);
        checkIn.put("symptoms", symptoms);
        checkIn.put("notes", notes);

        checkIn.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        showLoading(true);

        getMaternalEntries()
                .add(checkIn)
                .addOnSuccessListener(document -> {
                    showMessage("Check-in saved");
                    clearForm();
                    loadCheckIns();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    showMessage(
                            "Unable to save check-in"
                    );
                });
    }

    private void loadCheckIns() {
        showLoading(true);

        getMaternalEntries()
                .orderBy(
                        "checkInDateMillis",
                        Query.Direction.DESCENDING
                )
                .limit(6)
                .get()
                .addOnSuccessListener(snapshot -> {
                    entriesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        emptyTextView.setVisibility(
                                View.VISIBLE
                        );

                        maternalStageTextView.setText(
                                R.string.not_recorded
                        );

                        latestCheckInTextView.setText(
                                R.string.not_recorded
                        );
                    } else {
                        emptyTextView.setVisibility(
                                View.GONE
                        );

                        boolean firstEntry = true;

                        for (QueryDocumentSnapshot document
                                : snapshot) {

                            Long dateMillis =
                                    document.getLong(
                                            "checkInDateMillis"
                                    );

                            Long weekValue =
                                    document.getLong(
                                            "weekNumber"
                                    );

                            if (dateMillis == null
                                    || weekValue == null) {
                                continue;
                            }

                            int weekNumber =
                                    weekValue.intValue();

                            String stage =
                                    valueOrDefault(
                                            document.getString(
                                                    "stage"
                                            )
                                    );

                            String mood =
                                    valueOrDefault(
                                            document.getString(
                                                    "mood"
                                            )
                                    );

                            String symptoms =
                                    document.getString(
                                            "symptoms"
                                    );

                            String notes =
                                    document.getString(
                                            "notes"
                                    );

                            if (firstEntry) {
                                maternalStageTextView
                                        .setText(stage);

                                latestCheckInTextView
                                        .setText(
                                                getString(
                                                        R.string
                                                                .latest_check_in_value,
                                                        formatDate(
                                                                dateMillis
                                                        ),
                                                        weekNumber
                                                )
                                        );

                                firstEntry = false;
                            }

                            addCheckInCard(
                                    document.getId(),
                                    dateMillis,
                                    stage,
                                    weekNumber,
                                    mood,
                                    symptoms,
                                    notes
                            );
                        }
                    }

                    showLoading(false);
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    showMessage(
                            "Unable to load check-ins"
                    );
                });
    }

    private void addCheckInCard(
            String documentId,
            long dateMillis,
            String stage,
            int weekNumber,
            String mood,
            String symptoms,
            String notes
    ) {
        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .MATCH_PARENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT
                );

        cardParams.bottomMargin = dpToPixels(10);
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPixels(14));

        card.setCardBackgroundColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_surface
                )
        );

        card.setStrokeColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_border
                )
        );

        card.setStrokeWidth(dpToPixels(1));

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dpToPixels(16),
                dpToPixels(14),
                dpToPixels(16),
                dpToPixels(14)
        );

        TextView dateText =
                createEntryText(
                        formatDate(dateMillis),
                        17,
                        true
                );

        content.addView(dateText);

        TextView stageText =
                createEntryText(
                        getString(
                                R.string.stage_week_value,
                                stage,
                                weekNumber
                        ),
                        14,
                        false
                );

        content.addView(stageText);

        TextView moodText =
                createEntryText(
                        getString(
                                R.string.mood_value,
                                mood
                        ),
                        14,
                        false
                );

        content.addView(moodText);

        if (symptoms != null
                && !symptoms.isEmpty()) {

            content.addView(
                    createEntryText(
                            getString(
                                    R.string.symptoms_value,
                                    symptoms
                            ),
                            14,
                            false
                    )
            );
        }

        if (notes != null && !notes.isEmpty()) {
            content.addView(
                    createEntryText(
                            getString(
                                    R.string.notes_value,
                                    notes
                            ),
                            14,
                            false
                    )
            );
        }

        MaterialButton deleteButton =
                new MaterialButton(this);

        deleteButton.setText(R.string.delete);
        deleteButton.setAllCaps(false);

        deleteButton.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_error
                )
        );

        deleteButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.TRANSPARENT
                )
        );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT
                );

        deleteParams.gravity =
                android.view.Gravity.END;

        deleteButton.setLayoutParams(
                deleteParams
        );

        deleteButton.setOnClickListener(
                view -> showDeleteDialog(
                        documentId
                )
        );

        content.addView(deleteButton);

        card.addView(content);
        entriesContainer.addView(card);
    }

    private TextView createEntryText(
            String text,
            int textSize,
            boolean bold
    ) {
        TextView textView =
                new TextView(this);

        textView.setText(text);
        textView.setTextSize(textSize);

        textView.setPadding(
                0,
                dpToPixels(4),
                0,
                0
        );

        textView.setTextColor(
                ContextCompat.getColor(
                        this,
                        bold
                                ? R.color.nurtura_text
                                : R.color
                                .nurtura_text_secondary
                )
        );

        if (bold) {
            textView.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        return textView;
    }

    private void showDeleteDialog(
            String documentId
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.delete_record_title
                )
                .setMessage(
                        R.string.delete_record_message
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.delete,
                        (dialog, button) ->
                                deleteMaternalEntry(
                                        documentId
                                )
                )
                .show();
    }

    private void deleteMaternalEntry(
            String documentId
    ) {
        showLoading(true);

        getMaternalEntries()
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            MaternalActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadCheckIns();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            MaternalActivity.this,
                            R.string.unable_to_delete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private CollectionReference getMaternalEntries() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("maternalEntries");
    }

    private String formatDate(long dateMillis) {
        SimpleDateFormat format =
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                );

        return format.format(
                new Date(dateMillis)
        );
    }

    private String valueOrDefault(String value) {
        if (value == null || value.isEmpty()) {
            return getString(
                    R.string.not_recorded
            );
        }

        return value;
    }

    private void clearForm() {
        selectedDateMillis = -1;

        maternalStageSpinner.setSelection(0);
        moodSpinner.setSelection(0);

        checkInDateButton.setText(
                R.string.select_check_in_date
        );

        weekEditText.setText("");
        symptomsEditText.setText("");
        notesEditText.setText("");
    }

    private void showLoading(boolean loading) {
        progressIndicator.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        saveCheckInButton.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}