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
        if (selectedDateMillis == -1) {
            showMessage(
                    getString(R.string.select_pregnancy_check_in_date)
            );
            return;
        }

        String weekText = weekEditText
                .getText()
                .toString()
                .trim();

        if (weekText.isEmpty()) {
            showMessage(
                    getString(R.string.enter_pregnancy_week)
            );
            return;
        }

        int weekNumber;

        try {
            weekNumber = Integer.parseInt(weekText);
        } catch (NumberFormatException exception) {
            showMessage(
                    getString(R.string.invalid_pregnancy_week)
            );
            return;
        }

        if (weekNumber < 1 || weekNumber > 42) {
            showMessage(
                    getString(R.string.invalid_pregnancy_week)
            );
            return;
        }

        if (moodSpinner
                .getSelectedItemPosition() == 0) {

            showMessage(
                    getString(R.string.select_pregnancy_mood)
            );
            return;
        }

        String stage = "Pregnancy";

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
                    showMessage(
                            getString(R.string.pregnancy_check_in_saved)
                    );

                    showPregnancySizeInsight(weekNumber);

                    clearForm();
                    loadCheckIns();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    showMessage(
                            getString(
                                    R.string.unable_to_save_pregnancy_check_in
                            )
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
                .limit(30)
                .get()
                .addOnSuccessListener(snapshot -> {
                    entriesContainer.removeAllViews();

                    boolean firstEntry = true;
                    int displayedEntries = 0;

                    for (QueryDocumentSnapshot document
                            : snapshot) {

                        String stage = document.getString("stage");

                        if (!"Pregnancy".equalsIgnoreCase(stage)) {
                            continue;
                        }

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
                                    .setText(
                                            getString(
                                                    R.string.week_value,
                                                    weekNumber
                                            )
                                    );

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
                                weekNumber,
                                mood,
                                symptoms,
                                notes
                        );

                        displayedEntries++;

                        if (displayedEntries == 6) {
                            break;
                        }
                    }

                    if (firstEntry) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        maternalStageTextView.setText(R.string.not_recorded);
                        latestCheckInTextView.setText(R.string.not_recorded);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                    }

                    showLoading(false);
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    showMessage(
                            getString(
                                    R.string.unable_to_load_pregnancy_check_ins
                            )
                    );
                });
    }

    private void addCheckInCard(
            String documentId,
            long dateMillis,
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
                        R.color.maternal_light
                )
        );

        card.setStrokeColor(
                ContextCompat.getColor(
                        this,
                        R.color.maternal_accent
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

        TextView weekText =
                createEntryText(
                        getString(
                                R.string.week_value,
                                weekNumber
                        ),
                        14,
                        false
                );

        content.addView(weekText);

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

        moodSpinner.setSelection(0);

        checkInDateButton.setText(
                R.string.select_check_in_date
        );

        weekEditText.setText("");
        symptomsEditText.setText("");
        notesEditText.setText("");
    }

    private void showPregnancySizeInsight(int weekNumber) {
        String insight;
        String illustration;

        if (weekNumber <= 3) {
            illustration = "🌱";
            insight = getString(
                    R.string.pregnancy_early_week_insight
            );
        } else {
            String[] sizeComparisons = getResources().getStringArray(
                    R.array.pregnancy_baby_size_comparisons
            );

            String[] sizeIllustrations = getResources().getStringArray(
                    R.array.pregnancy_baby_size_illustrations
            );

            int comparisonWeek = Math.min(weekNumber, 40);
            int comparisonIndex = comparisonWeek - 4;

            illustration = sizeIllustrations[comparisonIndex];

            insight = getString(
                    R.string.pregnancy_baby_size_message,
                    sizeComparisons[comparisonIndex]
            );
        }

        String message = insight
                + "\n\n"
                + getString(R.string.pregnancy_growth_notice);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(24),
                dpToPixels(8),
                dpToPixels(24),
                0
        );

        TextView illustrationTextView = new TextView(this);
        illustrationTextView.setText(illustration);
        illustrationTextView.setTextSize(64);
        illustrationTextView.setGravity(android.view.Gravity.CENTER);
        content.addView(illustrationTextView);

        TextView messageTextView = new TextView(this);
        messageTextView.setText(message);
        messageTextView.setTextSize(15);
        messageTextView.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams messageParameters =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParameters.topMargin = dpToPixels(10);
        messageTextView.setLayoutParams(messageParameters);
        content.addView(messageTextView);

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        getString(
                                R.string.pregnancy_week_insight_title,
                                weekNumber
                        )
                )
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
