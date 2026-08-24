package com.jenny.nurtura;

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
import com.google.android.material.slider.Slider;
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

public class WellnessActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private Spinner moodSpinner;
    private Spinner energySpinner;

    private Slider sleepSlider;
    private Slider waterSlider;

    private TextView sleepValueTextView;
    private TextView waterValueTextView;

    private TextView averageSleepTextView;
    private TextView averageWaterTextView;
    private TextView commonMoodTextView;
    private TextView insightTextView;

    private TextView emptyTextView;
    private LinearLayout entriesContainer;

    private EditText notesEditText;
    private MaterialButton saveButton;
    private CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellness);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        moodSpinner =
                findViewById(R.id.wellnessMoodSpinner);

        energySpinner =
                findViewById(R.id.energySpinner);

        sleepSlider =
                findViewById(R.id.sleepSlider);

        waterSlider =
                findViewById(R.id.waterSlider);

        sleepValueTextView =
                findViewById(R.id.sleepValueTextView);

        waterValueTextView =
                findViewById(R.id.waterValueTextView);

        averageSleepTextView =
                findViewById(R.id.averageSleepTextView);

        averageWaterTextView =
                findViewById(R.id.averageWaterTextView);

        commonMoodTextView =
                findViewById(R.id.commonMoodTextView);

        insightTextView =
                findViewById(R.id.wellnessInsightTextView);

        notesEditText =
                findViewById(R.id.wellnessNotesEditText);

        saveButton =
                findViewById(R.id.saveWellnessButton);

        progressIndicator =
                findViewById(
                        R.id.wellnessProgressIndicator
                );

        emptyTextView =
                findViewById(
                        R.id.wellnessEmptyTextView
                );

        entriesContainer =
                findViewById(
                        R.id.wellnessEntriesContainer
                );

        sleepSlider.setValueFrom(0f);
        sleepSlider.setValueTo(12f);
        sleepSlider.setStepSize(0.5f);
        sleepSlider.setValue(7f);

        waterSlider.setValueFrom(0f);
        waterSlider.setValueTo(15f);
        waterSlider.setStepSize(1f);
        waterSlider.setValue(6f);

        updateSleepValue(7f);
        updateWaterValue(6f);

        sleepSlider.addOnChangeListener(
                (slider, value, fromUser) ->
                        updateSleepValue(value)
        );

        waterSlider.addOnChangeListener(
                (slider, value, fromUser) ->
                        updateWaterValue(value)
        );

        findViewById(R.id.wellnessBackButton)
                .setOnClickListener(view -> finish());

        saveButton.setOnClickListener(
                view -> saveTodayCheckIn()
        );

        loadWellnessEntries();
    }

    private void updateSleepValue(float value) {
        sleepValueTextView.setText(
                getString(
                        R.string.sleep_hours_value,
                        value
                )
        );
    }

    private void updateWaterValue(float value) {
        waterValueTextView.setText(
                getString(
                        R.string.water_glasses_value,
                        Math.round(value)
                )
        );
    }

    private void saveTodayCheckIn() {
        if (moodSpinner
                .getSelectedItemPosition() == 0) {

            showMessage("Select your mood");
            return;
        }

        if (energySpinner
                .getSelectedItemPosition() == 0) {

            showMessage("Select your energy level");
            return;
        }

        String mood =
                moodSpinner
                        .getSelectedItem()
                        .toString();

        String energy =
                energySpinner
                        .getSelectedItem()
                        .toString();

        double sleepHours =
                sleepSlider.getValue();

        int waterGlasses =
                Math.round(waterSlider.getValue());

        String notes =
                notesEditText
                        .getText()
                        .toString()
                        .trim();

        long todayMillis =
                getStartOfTodayMillis();

        String dateKey =
                createDateKey(todayMillis);

        Map<String, Object> checkIn =
                new HashMap<>();

        checkIn.put("dateMillis", todayMillis);
        checkIn.put("mood", mood);
        checkIn.put("energy", energy);
        checkIn.put("sleepHours", sleepHours);
        checkIn.put("waterGlasses", waterGlasses);
        checkIn.put("notes", notes);

        checkIn.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        showLoading(true);

        getWellnessEntries()
                .document(dateKey)
                .set(checkIn)
                .addOnSuccessListener(unused -> {
                    showMessage(
                            getString(
                                    R.string.wellness_saved
                            )
                    );

                    notesEditText.setText("");
                    loadWellnessEntries();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    showMessage(
                            "Unable to save check-in"
                    );
                });
    }

    private void loadWellnessEntries() {
        showLoading(true);

        getWellnessEntries()
                .orderBy(
                        "dateMillis",
                        Query.Direction.DESCENDING
                )
                .limit(7)
                .get()
                .addOnSuccessListener(snapshot -> {
                    entriesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        showEmptySummary();

                        emptyTextView.setVisibility(
                                View.VISIBLE
                        );

                        showLoading(false);
                        return;
                    }

                    emptyTextView.setVisibility(
                            View.GONE
                    );

                    double totalSleep = 0;
                    double totalWater = 0;
                    int validEntryCount = 0;

                    Map<String, Integer> moodCounts =
                            new HashMap<>();

                    for (QueryDocumentSnapshot document
                            : snapshot) {

                        Long dateMillis =
                                document.getLong(
                                        "dateMillis"
                                );

                        Double sleepHours =
                                document.getDouble(
                                        "sleepHours"
                                );

                        Long waterGlasses =
                                document.getLong(
                                        "waterGlasses"
                                );

                        String mood =
                                document.getString("mood");

                        String energy =
                                document.getString(
                                        "energy"
                                );

                        String notes =
                                document.getString(
                                        "notes"
                                );

                        if (dateMillis == null
                                || sleepHours == null
                                || waterGlasses == null
                                || mood == null
                                || energy == null) {
                            continue;
                        }

                        totalSleep += sleepHours;
                        totalWater += waterGlasses;
                        validEntryCount++;

                        int moodCount =
                                moodCounts.containsKey(mood)
                                        ? moodCounts.get(mood)
                                        : 0;

                        moodCounts.put(
                                mood,
                                moodCount + 1
                        );

                        addWellnessEntryCard(
                                dateMillis,
                                mood,
                                energy,
                                sleepHours,
                                waterGlasses.intValue(),
                                notes
                        );
                    }

                    if (validEntryCount > 0) {
                        double averageSleep =
                                totalSleep
                                        / validEntryCount;

                        double averageWater =
                                totalWater
                                        / validEntryCount;

                        String commonMood =
                                findCommonMood(moodCounts);

                        updateSummary(
                                averageSleep,
                                averageWater,
                                commonMood
                        );
                    } else {
                        showEmptySummary();
                    }

                    showLoading(false);
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    showMessage(
                            "Unable to load wellness data"
                    );
                });
    }

    private void updateSummary(
            double averageSleep,
            double averageWater,
            String commonMood
    ) {
        averageSleepTextView.setText(
                getString(
                        R.string.average_sleep_value,
                        averageSleep
                )
        );

        averageWaterTextView.setText(
                getString(
                        R.string.average_water_value,
                        averageWater
                )
        );

        commonMoodTextView.setText(commonMood);

        if (averageSleep < 7) {
            insightTextView.setText(
                    R.string.insight_sleep
            );
        } else if (averageWater < 6) {
            insightTextView.setText(
                    R.string.insight_water
            );
        } else if (commonMood.equals("Low")
                || commonMood.equals("Very low")) {

            insightTextView.setText(
                    R.string.insight_mood
            );
        } else {
            insightTextView.setText(
                    R.string.insight_balanced
            );
        }
    }

    private String findCommonMood(
            Map<String, Integer> moodCounts
    ) {
        String commonMood = "Not recorded";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry
                : moodCounts.entrySet()) {

            if (entry.getValue() > highestCount) {
                commonMood = entry.getKey();
                highestCount = entry.getValue();
            }
        }

        return commonMood;
    }

    private void showEmptySummary() {
        averageSleepTextView.setText(
                R.string.no_wellness_data
        );

        averageWaterTextView.setText(
                R.string.no_wellness_data
        );

        commonMoodTextView.setText(
                R.string.no_wellness_data
        );

        insightTextView.setText(
                R.string.no_wellness_data
        );
    }

    private void addWellnessEntryCard(
            long dateMillis,
            String mood,
            String energy,
            double sleepHours,
            int waterGlasses,
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

        content.addView(
                createEntryText(
                        getString(
                                R.string.mood_value,
                                mood
                        ),
                        14,
                        false
                )
        );

        content.addView(
                createEntryText(
                        getString(
                                R.string.energy_value,
                                energy
                        ),
                        14,
                        false
                )
        );

        content.addView(
                createEntryText(
                        getString(
                                R.string.wellness_habits_value,
                                sleepHours,
                                waterGlasses
                        ),
                        14,
                        false
                )
        );

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

    private CollectionReference getWellnessEntries() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wellnessEntries");
    }

    private long getStartOfTodayMillis() {
        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        return calendar.getTimeInMillis();
    }

    private String createDateKey(long dateMillis) {
        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        return format.format(
                new Date(dateMillis)
        );
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

    private void showLoading(boolean loading) {
        progressIndicator.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        saveButton.setEnabled(!loading);
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