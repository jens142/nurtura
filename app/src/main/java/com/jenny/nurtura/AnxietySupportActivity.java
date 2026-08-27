package com.jenny.nurtura;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnxietySupportActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private Slider currentSlider;
    private Slider afterSlider;

    private TextView currentRatingTextView;
    private TextView afterRatingTextView;

    private MaterialCheckBox worryCheckBox;
    private MaterialCheckBox restlessnessCheckBox;
    private MaterialCheckBox racingThoughtsCheckBox;
    private MaterialCheckBox overwhelmedCheckBox;

    private String understandNotes = "";
    private LinearLayout afterSupportSection;
    private LinearLayout entriesContainer;
    private TextView noEntriesTextView;

    private String selectedActivity = "";

    private CountDownTimer breathingTimer;
    private AlertDialog breathingDialog;
    private boolean breathingCompleted;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd MMM yyyy, h:mm a",
                    Locale.getDefault()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anxiety_support);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        currentSlider =
                findViewById(R.id.currentAnxietySlider);

        afterSlider =
                findViewById(R.id.afterAnxietySlider);

        currentRatingTextView =
                findViewById(R.id.currentRatingTextView);

        afterRatingTextView =
                findViewById(R.id.afterRatingTextView);

        worryCheckBox =
                findViewById(R.id.worryCheckBox);

        restlessnessCheckBox =
                findViewById(R.id.restlessnessCheckBox);

        racingThoughtsCheckBox =
                findViewById(R.id.racingThoughtsCheckBox);

        overwhelmedCheckBox =
                findViewById(R.id.overwhelmedCheckBox);

        afterSupportSection =
                findViewById(R.id.afterSupportSection);

        entriesContainer =
                findViewById(R.id.anxietyEntriesContainer);

        noEntriesTextView =
                findViewById(R.id.noAnxietyEntriesTextView);

        configureSliders();

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        findViewById(R.id.breatheCard)
                .setOnClickListener(
                        view -> showBreathingExercise()
                );

        findViewById(R.id.groundCard)
                .setOnClickListener(
                        view -> showGroundingExercise()
                );

        findViewById(R.id.understandCard)
                .setOnClickListener(
                        view -> showUnderstandPrompt()
                );

        findViewById(R.id.saveAnxietyButton)
                .setOnClickListener(
                        view -> saveAnxietyCheckIn()
                );

        loadAnxietyEntries();
    }

    private void configureSliders() {
        currentSlider.setValueFrom(1f);
        currentSlider.setValueTo(10f);
        currentSlider.setStepSize(1f);
        currentSlider.setValue(5f);

        afterSlider.setValueFrom(1f);
        afterSlider.setValueTo(10f);
        afterSlider.setStepSize(1f);
        afterSlider.setValue(5f);

        updateCurrentRating(5);
        updateAfterRating(5);

        currentSlider.addOnChangeListener(
                (slider, value, fromUser) ->
                        updateCurrentRating(
                                Math.round(value)
                        )
        );

        afterSlider.addOnChangeListener(
                (slider, value, fromUser) ->
                        updateAfterRating(
                                Math.round(value)
                        )
        );
    }

    private void updateCurrentRating(int rating) {
        currentRatingTextView.setText(
                getString(
                        R.string.anxiety_rating_value,
                        rating
                )
        );
    }

    private void updateAfterRating(int rating) {
        afterRatingTextView.setText(
                getString(
                        R.string.anxiety_rating_value,
                        rating
                )
        );
    }

    private void showBreathingExercise() {
        breathingCompleted = false;

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dpToPixels(24),
                dpToPixels(12),
                dpToPixels(24),
                dpToPixels(12)
        );

        TextView instructions =
                createTextView(
                        getString(
                                R.string.breathing_instructions
                        ),
                        15,
                        false
                );

        TextView phaseText =
                createTextView(
                        getString(
                                R.string.breathe_in
                        ),
                        28,
                        true
                );

        LinearLayout.LayoutParams phaseParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        phaseParams.topMargin = dpToPixels(26);
        phaseText.setGravity(Gravity.CENTER);
        phaseText.setLayoutParams(phaseParams);

        TextView timeText =
                createTextView(
                        getString(
                                R.string.breathing_time_value,
                                2,
                                0
                        ),
                        17,
                        false
                );

        LinearLayout.LayoutParams timeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        timeParams.topMargin = dpToPixels(14);
        timeText.setGravity(Gravity.CENTER);
        timeText.setLayoutParams(timeParams);

        content.addView(instructions);
        content.addView(phaseText);
        content.addView(timeText);

        breathingDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(
                                R.string.breathing_exercise
                        )
                        .setView(content)
                        .setNegativeButton(
                                R.string.finish_breathing,
                                null
                        )
                        .setCancelable(false)
                        .create();

        breathingDialog.setOnShowListener(dialog ->
                breathingDialog
                        .getButton(
                                DialogInterface.BUTTON_NEGATIVE
                        )
                        .setOnClickListener(
                                view -> finishBreathing()
                        )
        );

        breathingDialog.setOnDismissListener(dialog -> {
            if (breathingTimer != null) {
                breathingTimer.cancel();
            }
        });

        breathingDialog.show();

        breathingTimer =
                new CountDownTimer(
                        120000,
                        1000
                ) {
                    @Override
                    public void onTick(
                            long millisecondsRemaining
                    ) {
                        int secondsRemaining =
                                (int) Math.ceil(
                                        millisecondsRemaining
                                                / 1000.0
                                );

                        int elapsedSeconds =
                                120 - secondsRemaining;

                        boolean breathingIn =
                                (elapsedSeconds / 4) % 2 == 0;

                        phaseText.setText(
                                breathingIn
                                        ? R.string.breathe_in
                                        : R.string.breathe_out
                        );

                        int minutes =
                                secondsRemaining / 60;

                        int seconds =
                                secondsRemaining % 60;

                        timeText.setText(
                                getString(
                                        R.string.breathing_time_value,
                                        minutes,
                                        seconds
                                )
                        );
                    }

                    @Override
                    public void onFinish() {
                        finishBreathing();
                    }
                };

        breathingTimer.start();
    }

    private void finishBreathing() {
        if (breathingCompleted) {
            return;
        }

        breathingCompleted = true;

        if (breathingTimer != null) {
            breathingTimer.cancel();
        }

        if (breathingDialog != null
                && breathingDialog.isShowing()) {
            breathingDialog.dismiss();
        }

        completeSupportActivity(
                getString(R.string.breathe)
        );
    }

    private void showGroundingExercise() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.grounding_exercise
                )
                .setMessage(
                        R.string.grounding_instructions
                )
                .setPositiveButton(
                        R.string.finish_grounding,
                        (dialog, which) ->
                                completeSupportActivity(
                                        getString(
                                                R.string.ground
                                        )
                                )
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void showUnderstandPrompt() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(24),
                dpToPixels(8),
                dpToPixels(24),
                dpToPixels(8)
        );

        TextView promptText = createTextView(
                getString(R.string.understand_prompt),
                15,
                false
        );

        content.addView(promptText);

        TextInputLayout inputLayout =
                new TextInputLayout(this);

        inputLayout.setHint(
                R.string.anxiety_cause_hint
        );

        inputLayout.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        inputParams.topMargin = dpToPixels(18);
        inputLayout.setLayoutParams(inputParams);

        TextInputEditText journalField =
                new TextInputEditText(inputLayout.getContext());

        journalField.setMinLines(4);
        journalField.setGravity(Gravity.TOP);
        journalField.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        String existingNotes = understandNotes;

        journalField.setText(existingNotes);
        journalField.setSelection(
                journalField.getText() == null
                        ? 0
                        : journalField.getText().length()
        );

        inputLayout.addView(journalField);
        content.addView(inputLayout);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.understand)
                .setView(content)
                .setPositiveButton(
                        R.string.continue_check_in,
                        (dialog, which) -> {
                            String notes =
                                    getTextFromField(journalField);

                            understandNotes = notes;

                            completeSupportActivity(
                                    getString(R.string.understand)
                            );
                        }
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void completeSupportActivity(
            String activityName
    ) {
        selectedActivity = activityName;

        afterSupportSection.setVisibility(
                View.VISIBLE
        );

        Toast.makeText(
                this,
                getString(
                        R.string.support_activity_completed,
                        activityName
                ),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void saveAnxietyCheckIn() {
        if (TextUtils.isEmpty(selectedActivity)) {
            Toast.makeText(
                    this,
                    R.string.choose_anxiety_activity,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int beforeRating =
                Math.round(
                        currentSlider.getValue()
                );

        int afterRating =
                Math.round(
                        afterSlider.getValue()
                );

        String feelings =
                buildFeelingsText();

        String cause = understandNotes;

        long createdAtMillis =
                System.currentTimeMillis();

        Map<String, Object> entry =
                new HashMap<>();

        entry.put(
                "beforeRating",
                beforeRating
        );

        entry.put(
                "afterRating",
                afterRating
        );

        entry.put(
                "supportActivity",
                selectedActivity
        );

        entry.put(
                "feelings",
                feelings
        );

        entry.put(
                "cause",
                cause
        );

        entry.put(
                "worry",
                worryCheckBox.isChecked()
        );

        entry.put(
                "restlessness",
                restlessnessCheckBox.isChecked()
        );

        entry.put(
                "racingThoughts",
                racingThoughtsCheckBox.isChecked()
        );

        entry.put(
                "overwhelmed",
                overwhelmedCheckBox.isChecked()
        );

        entry.put(
                "entryDate",
                dateFormat.format(
                        new Date(createdAtMillis)
                )
        );

        entry.put(
                "createdAtMillis",
                createdAtMillis
        );

        entry.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("anxietyEntries")
                .add(entry)
                .addOnSuccessListener(
                        documentReference -> {
                            Toast.makeText(
                                    AnxietySupportActivity.this,
                                    R.string.anxiety_check_in_saved,
                                    Toast.LENGTH_SHORT
                            ).show();

                            clearForm();
                            loadAnxietyEntries();
                        }
                )
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        AnxietySupportActivity.this,
                                        R.string.unable_to_save_anxiety,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private String buildFeelingsText() {
        List<String> feelings =
                new ArrayList<>();

        if (worryCheckBox.isChecked()) {
            feelings.add(
                    getString(
                            R.string.feeling_worried
                    )
            );
        }

        if (restlessnessCheckBox.isChecked()) {
            feelings.add(
                    getString(
                            R.string.feeling_restless
                    )
            );
        }

        if (racingThoughtsCheckBox.isChecked()) {
            feelings.add(
                    getString(
                            R.string.racing_thoughts
                    )
            );
        }

        if (overwhelmedCheckBox.isChecked()) {
            feelings.add(
                    getString(
                            R.string.feeling_overwhelmed
                    )
            );
        }

        if (feelings.isEmpty()) {
            return getString(
                    R.string.no_feelings_selected
            );
        }

        return TextUtils.join(
                ", ",
                feelings
        );
    }

    private void loadAnxietyEntries() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("anxietyEntries")
                .orderBy(
                        "createdAtMillis",
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

                        return;
                    }

                    noEntriesTextView.setVisibility(
                            View.GONE
                    );

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {
                        addEntryCard(document);
                    }
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        AnxietySupportActivity.this,
                                        R.string.unable_to_load_anxiety,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
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

        TextView dateText =
                createTextView(
                        getDocumentString(
                                document,
                                "entryDate"
                        ),
                        16,
                        true
                );

        content.addView(dateText);

        int beforeRating =
                getDocumentInteger(
                        document,
                        "beforeRating"
                );

        int afterRating =
                getDocumentInteger(
                        document,
                        "afterRating"
                );

        String activity =
                getDocumentString(
                        document,
                        "supportActivity"
                );

        addDetailText(
                content,
                getString(
                        R.string.anxiety_before_after_value,
                        beforeRating,
                        afterRating,
                        activity
                )
        );

        addDetailText(
                content,
                getString(
                        R.string.anxiety_feelings_value,
                        getDocumentString(
                                document,
                                "feelings"
                        )
                )
        );

        String cause =
                getDocumentString(
                        document,
                        "cause"
                );

        if (!TextUtils.isEmpty(cause)) {
            addDetailText(
                    content,
                    getString(
                            R.string.anxiety_cause_value,
                            cause
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
                .collection("anxietyEntries")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            AnxietySupportActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadAnxietyEntries();
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        AnxietySupportActivity.this,
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
        String value =
                document.getString(fieldName);

        return value == null ? "" : value;
    }

    private int getDocumentInteger(
            DocumentSnapshot document,
            String fieldName
    ) {
        Long value =
                document.getLong(fieldName);

        return value == null
                ? 0
                : value.intValue();
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void clearForm() {
        currentSlider.setValue(5f);
        afterSlider.setValue(5f);

        worryCheckBox.setChecked(false);
        restlessnessCheckBox.setChecked(false);
        racingThoughtsCheckBox.setChecked(false);
        overwhelmedCheckBox.setChecked(false);

        understandNotes = "";

        selectedActivity = "";

        afterSupportSection.setVisibility(
                View.GONE
        );
    }

    private void openLoginActivity() {
        Intent intent =
                new Intent(
                        AnxietySupportActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }

        super.onDestroy();
    }
}