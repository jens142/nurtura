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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MenstrualActivity extends AppCompatActivity {

    private static final long DAY_IN_MILLIS =
            24L * 60L * 60L * 1000L;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private MaterialButton dateButton;
    private MaterialButton saveEntryButton;
    private Spinner flowSpinner;
    private EditText symptomsEditText;
    private EditText notesEditText;
    private CircularProgressIndicator progressIndicator;

    private TextView lastPeriodTextView;
    private TextView averageCycleTextView;
    private TextView nextPeriodTextView;
    private TextView ovulationDateTextView;
    private TextView fertileWindowTextView;
    private TextView emptyEntriesTextView;
    private LinearLayout recentEntriesContainer;

    private long selectedDateMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menstrual);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        dateButton = findViewById(R.id.dateButton);
        saveEntryButton = findViewById(R.id.saveEntryButton);
        flowSpinner = findViewById(R.id.flowSpinner);
        symptomsEditText = findViewById(R.id.symptomsEditText);
        notesEditText = findViewById(R.id.notesEditText);
        progressIndicator = findViewById(R.id.progressIndicator);

        lastPeriodTextView =
                findViewById(R.id.lastPeriodTextView);
        averageCycleTextView =
                findViewById(R.id.averageCycleTextView);
        nextPeriodTextView =
                findViewById(R.id.nextPeriodTextView);
        ovulationDateTextView =
                findViewById(R.id.ovulationDateTextView);
        fertileWindowTextView =
                findViewById(R.id.fertileWindowTextView);
        emptyEntriesTextView =
                findViewById(R.id.emptyEntriesTextView);
        recentEntriesContainer =
                findViewById(R.id.recentEntriesContainer);

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        dateButton.setOnClickListener(
                view -> showDatePicker()
        );

        saveEntryButton.setOnClickListener(
                view -> savePeriodEntry()
        );

        loadPeriodEntries();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        if (selectedDateMillis != -1) {
            calendar.setTimeInMillis(selectedDateMillis);
        }

        DatePickerDialog datePickerDialog =
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
                                    selectedDate.getTimeInMillis();

                            dateButton.setText(
                                    formatDate(selectedDateMillis)
                            );
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.getDatePicker()
                .setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void savePeriodEntry() {
        if (selectedDateMillis == -1) {
            Toast.makeText(
                    this,
                    "Select the period start date",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (flowSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(
                    this,
                    "Select the flow level",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String flow =
                flowSpinner.getSelectedItem().toString();

        String symptoms =
                symptomsEditText.getText()
                        .toString()
                        .trim();

        String notes =
                notesEditText.getText()
                        .toString()
                        .trim();

        Map<String, Object> periodEntry =
                new HashMap<>();

        periodEntry.put(
                "startDateMillis",
                selectedDateMillis
        );

        periodEntry.put("flow", flow);
        periodEntry.put("symptoms", symptoms);
        periodEntry.put("notes", notes);

        periodEntry.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        showLoading(true);

        getPeriodEntries()
                .add(periodEntry)
                .addOnSuccessListener(document -> {
                    Toast.makeText(
                            MenstrualActivity.this,
                            "Period entry saved",
                            Toast.LENGTH_SHORT
                    ).show();

                    clearForm();
                    loadPeriodEntries();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            MenstrualActivity.this,
                            "Unable to save entry",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadPeriodEntries() {
        showLoading(true);

        getPeriodEntries()
                .orderBy(
                        "startDateMillis",
                        Query.Direction.DESCENDING
                )
                .limit(6)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recentEntriesContainer.removeAllViews();

                    List<Long> periodDates =
                            new ArrayList<>();

                    if (querySnapshot.isEmpty()) {
                        emptyEntriesTextView.setVisibility(
                                View.VISIBLE
                        );

                        showEmptySummary();
                    } else {
                        emptyEntriesTextView.setVisibility(
                                View.GONE
                        );

                        for (QueryDocumentSnapshot document
                                : querySnapshot) {

                            Long startDate =
                                    document.getLong(
                                            "startDateMillis"
                                    );

                            if (startDate == null) {
                                continue;
                            }

                            periodDates.add(startDate);

                            String flow =
                                    document.getString("flow");
                            String symptoms =
                                    document.getString(
                                            "symptoms"
                                    );
                            String notes =
                                    document.getString("notes");

                            addRecentEntryCard(
                                    document.getId(),
                                    startDate,
                                    flow,
                                    symptoms,
                                    notes
                            );
                        }

                        updateCycleSummary(periodDates);
                    }

                    showLoading(false);
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            MenstrualActivity.this,
                            "Unable to load entries",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void updateCycleSummary(
            List<Long> periodDates
    ) {
        if (periodDates.isEmpty()) {
            showEmptySummary();
            return;
        }

        long latestDate = periodDates.get(0);

        lastPeriodTextView.setText(
                formatDate(latestDate)
        );

        int predictedCycleLength = 28;
        boolean cycleEstimateAvailable = false;

        if (periodDates.size() >= 2) {
            long totalDays = 0;
            int intervalCount = 0;

            for (int index = 1;
                 index < periodDates.size();
                 index++) {

                long newerDate =
                        periodDates.get(index - 1);

                long olderDate =
                        periodDates.get(index);

                long difference =
                        newerDate - olderDate;

                long days =
                        Math.round(
                                difference
                                        / (double) DAY_IN_MILLIS
                        );

                if (days > 0) {
                    totalDays += days;
                    intervalCount++;
                }
            }

            if (intervalCount > 0) {
                predictedCycleLength =
                        Math.round(
                                totalDays
                                        / (float) intervalCount
                        );

                averageCycleTextView.setText(
                        predictedCycleLength + " days"
                );

                cycleEstimateAvailable = true;
            } else {
                averageCycleTextView.setText(
                        R.string.not_enough_data
                );
            }
        } else {
            averageCycleTextView.setText(
                    R.string.not_enough_data
            );
        }

        long predictedDate =
                addDays(
                        latestDate,
                        predictedCycleLength
                );

        nextPeriodTextView.setText(
                formatDate(predictedDate)
        );

        if (cycleEstimateAvailable) {
            updateOvulationEstimate(predictedDate);
        } else {
            showUnavailableOvulationEstimate();
        }
    }

    private void updateOvulationEstimate(
            long predictedNextPeriod
    ) {
        long ovulationDate =
                addDays(
                        predictedNextPeriod,
                        -14
                );

        long fertileWindowStart =
                addDays(
                        ovulationDate,
                        -5
                );

        long fertileWindowEnd =
                addDays(
                        ovulationDate,
                        1
                );

        ovulationDateTextView.setText(
                formatDate(ovulationDate)
        );

        fertileWindowTextView.setText(
                getString(
                        R.string.fertile_window_date_value,
                        formatDate(fertileWindowStart),
                        formatDate(fertileWindowEnd)
                )
        );
    }

    private void showUnavailableOvulationEstimate() {
        ovulationDateTextView.setText(
                R.string.not_enough_data
        );

        fertileWindowTextView.setText(
                R.string.not_enough_data
        );
    }

    private void showEmptySummary() {
        lastPeriodTextView.setText(
                R.string.not_recorded
        );

        averageCycleTextView.setText(
                R.string.not_enough_data
        );

        nextPeriodTextView.setText(
                R.string.not_enough_data
        );

        showUnavailableOvulationEstimate();
    }

    private long addDays(
            long dateMillis,
            int numberOfDays
    ) {
        Calendar calendar =
                Calendar.getInstance();

        calendar.setTimeInMillis(dateMillis);

        calendar.add(
                Calendar.DAY_OF_MONTH,
                numberOfDays
        );

        return calendar.getTimeInMillis();
    }
    private void addRecentEntryCard(
            String documentId,
            long startDate,
            String flow,
            String symptoms,
            String notes
    ) {
        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
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

        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(16),
                dpToPixels(14),
                dpToPixels(16),
                dpToPixels(14)
        );

        TextView dateTextView =
                new TextView(this);

        dateTextView.setText(formatDate(startDate));
        dateTextView.setTextSize(17);
        dateTextView.setTypeface(
                null,
                Typeface.BOLD
        );

        dateTextView.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_text
                )
        );

        content.addView(dateTextView);

        TextView flowTextView =
                new TextView(this);

        flowTextView.setText(
                "Flow: "
                        + (flow == null ? "Not recorded" : flow)
        );

        flowTextView.setTextSize(14);
        flowTextView.setPadding(
                0,
                dpToPixels(5),
                0,
                0
        );

        flowTextView.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_text_secondary
                )
        );

        content.addView(flowTextView);

        if (symptoms != null && !symptoms.isEmpty()) {
            TextView symptomsTextView =
                    new TextView(this);

            symptomsTextView.setText(
                    "Symptoms: " + symptoms
            );

            symptomsTextView.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.nurtura_text_secondary
                    )
            );

            content.addView(symptomsTextView);
        }

        if (notes != null && !notes.isEmpty()) {
            TextView notesTextView =
                    new TextView(this);

            notesTextView.setText(
                    "Notes: " + notes
            );

            notesTextView.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.nurtura_text_secondary
                    )
            );

            content.addView(notesTextView);
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
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        deleteParams.gravity =
                android.view.Gravity.END;

        deleteButton.setLayoutParams(deleteParams);

        deleteButton.setOnClickListener(
                view -> showDeleteDialog(documentId)
        );

        content.addView(deleteButton);

        card.addView(content);
        recentEntriesContainer.addView(card);
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
                                deletePeriodEntry(
                                        documentId
                                )
                )
                .show();
    }

    private void deletePeriodEntry(
            String documentId
    ) {
        showLoading(true);

        getPeriodEntries()
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            MenstrualActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadPeriodEntries();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            MenstrualActivity.this,
                            R.string.unable_to_delete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private CollectionReference getPeriodEntries() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("periodEntries");
    }

    private String formatDate(long dateMillis) {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                );

        return dateFormat.format(
                new Date(dateMillis)
        );
    }

    private void clearForm() {
        selectedDateMillis = -1;

        dateButton.setText(
                R.string.select_start_date
        );

        flowSpinner.setSelection(0);
        symptomsEditText.setText("");
        notesEditText.setText("");
    }

    private void showLoading(boolean loading) {
        progressIndicator.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        saveEntryButton.setEnabled(!loading);
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}