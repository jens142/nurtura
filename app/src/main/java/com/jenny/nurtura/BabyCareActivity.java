package com.jenny.nurtura;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

public class BabyCareActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextView todayRecordsTextView;
    private TextView latestActivityTextView;
    private TextView emptyTextView;
    private LinearLayout entriesContainer;

    private CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baby_care);

        firestore = FirebaseFirestore.getInstance();

        currentUser = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        todayRecordsTextView =
                findViewById(
                        R.id.todayRecordsTextView
                );

        latestActivityTextView =
                findViewById(
                        R.id.latestBabyActivityTextView
                );

        emptyTextView =
                findViewById(
                        R.id.babyEmptyTextView
                );

        entriesContainer =
                findViewById(
                        R.id.babyEntriesContainer
                );

        progressIndicator =
                findViewById(
                        R.id.babyProgressIndicator
                );

        findViewById(R.id.babyBackButton)
                .setOnClickListener(
                        view -> finish()
                );

        findViewById(R.id.feedButton)
                .setOnClickListener(view ->
                        showOptionsDialog(
                                R.string.choose_feeding_type,
                                R.array.feeding_options,
                                "Feed"
                        )
                );

        findViewById(R.id.diaperButton)
                .setOnClickListener(view ->
                        showOptionsDialog(
                                R.string.choose_diaper_type,
                                R.array.diaper_options,
                                "Diaper"
                        )
                );

        findViewById(R.id.sleepButton)
                .setOnClickListener(view ->
                        showOptionsDialog(
                                R.string.choose_sleep_action,
                                R.array.sleep_options,
                                "Sleep"
                        )
                );

        findViewById(R.id.bathButton)
                .setOnClickListener(view ->
                        saveActivity(
                                "Bath",
                                "Bath"
                        )
                );

        loadBabyRecords();
        loadTodayCount();
    }

    private void showOptionsDialog(
            int titleResource,
            int optionsArrayResource,
            String category
    ) {
        String[] options = getResources()
                .getStringArray(
                        optionsArrayResource
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(titleResource)
                .setItems(
                        options,
                        (dialog, selectedIndex) ->
                                saveActivity(
                                        category,
                                        options[
                                                selectedIndex
                                                ]
                                )
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void saveActivity(
            String category,
            String activity
    ) {
        long currentTime =
                System.currentTimeMillis();

        Map<String, Object> babyRecord =
                new HashMap<>();

        babyRecord.put(
                "category",
                category
        );

        babyRecord.put(
                "activity",
                activity
        );

        babyRecord.put(
                "activityTimeMillis",
                currentTime
        );

        babyRecord.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        showLoading(true);
        setQuickButtonsEnabled(false);

        getBabyEntries()
                .add(babyRecord)
                .addOnSuccessListener(document -> {
                    Toast.makeText(
                            BabyCareActivity.this,
                            getString(
                                    R.string.activity_recorded,
                                    activity
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    setQuickButtonsEnabled(true);
                    loadBabyRecords();
                    loadTodayCount();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);
                    setQuickButtonsEnabled(true);

                    Toast.makeText(
                            BabyCareActivity.this,
                            "Unable to record activity",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadBabyRecords() {
        showLoading(true);

        getBabyEntries()
                .orderBy(
                        "activityTimeMillis",
                        Query.Direction.DESCENDING
                )
                .limit(15)
                .get()
                .addOnSuccessListener(snapshot -> {
                    entriesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        emptyTextView.setVisibility(
                                View.VISIBLE
                        );

                        latestActivityTextView.setText(
                                R.string.not_recorded
                        );
                    } else {
                        emptyTextView.setVisibility(
                                View.GONE
                        );

                        boolean firstRecord = true;

                        for (QueryDocumentSnapshot document
                                : snapshot) {

                            String activity =
                                    document.getString(
                                            "activity"
                                    );

                            Long activityTime =
                                    document.getLong(
                                            "activityTimeMillis"
                                    );

                            if (activity == null
                                    || activityTime == null) {
                                continue;
                            }

                            if (firstRecord) {
                                latestActivityTextView
                                        .setText(
                                                getString(
                                                        R.string
                                                                .activity_date_time_value,
                                                        activity,
                                                        formatTime(
                                                                activityTime
                                                        )
                                                )
                                        );

                                firstRecord = false;
                            }

                            addRecordCard(
                                    document.getId(),
                                    activity,
                                    activityTime
                            );
                        }
                    }

                    showLoading(false);
                    setQuickButtonsEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);
                    setQuickButtonsEnabled(true);

                    Toast.makeText(
                            BabyCareActivity.this,
                            "Unable to load care records",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadTodayCount() {
        long startOfToday =
                getStartOfTodayMillis();

        getBabyEntries()
                .whereGreaterThanOrEqualTo(
                        "activityTimeMillis",
                        startOfToday
                )
                .get()
                .addOnSuccessListener(snapshot ->
                        todayRecordsTextView.setText(
                                getString(
                                        R.string.records_count_value,
                                        snapshot.size()
                                )
                        )
                )
                .addOnFailureListener(exception ->
                        todayRecordsTextView.setText(
                                getString(
                                        R.string.records_count_value,
                                        0
                                )
                        )
                );
    }

    private void addRecordCard(
            String documentId,
            String activity,
            long activityTime
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

        cardParams.bottomMargin =
                dpToPixels(10);

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

        card.setStrokeWidth(
                dpToPixels(1)
        );

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

        TextView activityText =
                new TextView(this);

        activityText.setText(activity);
        activityText.setTextSize(17);

        activityText.setTypeface(
                null,
                Typeface.BOLD
        );

        activityText.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_text
                )
        );

        content.addView(activityText);

        TextView timeText =
                new TextView(this);

        timeText.setText(
                getString(
                        R.string.activity_date_time_value,
                        formatDate(activityTime),
                        formatTime(activityTime)
                )
        );

        timeText.setTextSize(14);

        timeText.setPadding(
                0,
                dpToPixels(5),
                0,
                0
        );

        timeText.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_text_secondary
                )
        );

        content.addView(timeText);

        MaterialButton deleteButton =
                new MaterialButton(this);

        deleteButton.setText(
                R.string.delete
        );

        deleteButton.setAllCaps(false);

        deleteButton.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.nurtura_error
                )
        );

        deleteButton.setBackgroundTintList(
                android.content.res.ColorStateList
                        .valueOf(
                                android.graphics.Color
                                        .TRANSPARENT
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
                view ->
                        showDeleteRecordDialog(
                                documentId
                        )
        );

        content.addView(deleteButton);

        card.addView(content);
        entriesContainer.addView(card);
    }

    private void showDeleteRecordDialog(
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
                                deleteBabyRecord(
                                        documentId
                                )
                )
                .show();
    }

    private void deleteBabyRecord(
            String documentId
    ) {
        showLoading(true);
        setQuickButtonsEnabled(false);

        getBabyEntries()
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            BabyCareActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadBabyRecords();
                    loadTodayCount();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);
                    setQuickButtonsEnabled(true);

                    Toast.makeText(
                            BabyCareActivity.this,
                            R.string.unable_to_delete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private CollectionReference getBabyEntries() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("babyEntries");
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

    private String formatDate(
            long timeMillis
    ) {
        SimpleDateFormat format =
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                );

        return format.format(
                new Date(timeMillis)
        );
    }

    private String formatTime(
            long timeMillis
    ) {
        SimpleDateFormat format =
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                );

        return format.format(
                new Date(timeMillis)
        );
    }

    private void showLoading(
            boolean loading
    ) {
        progressIndicator.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void setQuickButtonsEnabled(
            boolean enabled
    ) {
        findViewById(R.id.feedButton)
                .setEnabled(enabled);

        findViewById(R.id.diaperButton)
                .setEnabled(enabled);

        findViewById(R.id.sleepButton)
                .setEnabled(enabled);

        findViewById(R.id.bathButton)
                .setEnabled(enabled);
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}