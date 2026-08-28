package com.jenny.nurtura;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ReviewFeedbackActivity
        extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private LinearLayout feedbackContainer;
    private TextView noFeedbackTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_review_feedback
        );

        firestore =
                FirebaseFirestore.getInstance();

        currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        feedbackContainer =
                findViewById(
                        R.id.feedbackContainer
                );

        noFeedbackTextView =
                findViewById(
                        R.id.noFeedbackTextView
                );

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        verifyAdminAccess();
    }

    private void verifyAdminAccess() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role =
                            documentSnapshot.getString("role");

                    if ("admin".equalsIgnoreCase(role)) {
                        loadFeedback();
                    } else {
                        Toast.makeText(
                                ReviewFeedbackActivity.this,
                                R.string.admin_access_denied,
                                Toast.LENGTH_SHORT
                        ).show();

                        openMainActivity();
                    }
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(
                            ReviewFeedbackActivity.this,
                            R.string.unable_to_verify_admin,
                            Toast.LENGTH_SHORT
                    ).show();

                    openMainActivity();
                });
    }

    private void loadFeedback() {
        firestore.collection("feedback")
                .orderBy(
                        "createdAtMillis",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    feedbackContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        noFeedbackTextView.setVisibility(
                                View.VISIBLE
                        );

                        return;
                    }

                    noFeedbackTextView.setVisibility(
                            View.GONE
                    );

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        addFeedbackCard(document);
                    }
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                ReviewFeedbackActivity.this,
                                R.string.unable_to_load_feedback,
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void addFeedbackCard(
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

        LinearLayout contentLayout =
                new LinearLayout(this);

        contentLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        contentLayout.setPadding(
                dpToPixels(18),
                dpToPixels(16),
                dpToPixels(18),
                dpToPixels(16)
        );

        String userName =
                getDocumentString(
                        document,
                        "userName"
                );

        String userEmail =
                getDocumentString(
                        document,
                        "userEmail"
                );

        String submittedDate =
                getDocumentString(
                        document,
                        "submittedDate"
                );

        String message =
                getDocumentString(
                        document,
                        "message"
                );

        String status =
                getDocumentString(
                        document,
                        "status"
                );

        if (userName.isEmpty()) {
            userName = userEmail;
        }

        if (userName.isEmpty()) {
            userName =
                    getString(R.string.not_recorded);
        }

        if (status.isEmpty()) {
            status =
                    getString(
                            R.string.feedback_status_new
                    );
        }

        TextView nameText =
                createTextView(
                        getString(
                                R.string.feedback_from_value,
                                userName
                        ),
                        17,
                        true
                );

        contentLayout.addView(nameText);

        if (!userEmail.isEmpty()) {
            TextView emailText =
                    createTextView(
                            getString(
                                    R.string.feedback_email_value,
                                    userEmail
                            ),
                            13,
                            false
                    );

            addTextWithTopMargin(
                    contentLayout,
                    emailText,
                    5
            );
        }

        TextView dateText =
                createTextView(
                        getString(
                                R.string.feedback_date_value,
                                submittedDate
                        ),
                        13,
                        false
                );

        addTextWithTopMargin(
                contentLayout,
                dateText,
                5
        );

        TextView messageText =
                createTextView(
                        message,
                        15,
                        false
                );

        addTextWithTopMargin(
                contentLayout,
                messageText,
                14
        );

        TextView statusText =
                createTextView(
                        getString(
                                R.string.feedback_status_value,
                                status
                        ),
                        13,
                        true
                );

        addTextWithTopMargin(
                contentLayout,
                statusText,
                12
        );

        LinearLayout buttonRow =
                new LinearLayout(this);

        buttonRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        buttonRow.setGravity(Gravity.END);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.topMargin =
                dpToPixels(10);

        buttonRow.setLayoutParams(rowParams);

        if ("New".equalsIgnoreCase(status)) {
            MaterialButton reviewedButton =
                    new MaterialButton(this);

            reviewedButton.setText(
                    R.string.mark_feedback_reviewed
            );

            LinearLayout.LayoutParams reviewedParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            reviewedParams.setMarginEnd(
                    dpToPixels(8)
            );

            reviewedButton.setLayoutParams(
                    reviewedParams
            );

            reviewedButton.setOnClickListener(
                    view -> markFeedbackReviewed(
                            document.getId()
                    )
            );

            buttonRow.addView(reviewedButton);
        }

        MaterialButton deleteButton =
                new MaterialButton(this);

        deleteButton.setText(
                R.string.delete_feedback
        );

        deleteButton.setOnClickListener(
                view -> confirmDeleteFeedback(
                        document.getId()
                )
        );

        buttonRow.addView(deleteButton);

        contentLayout.addView(buttonRow);
        card.addView(contentLayout);
        feedbackContainer.addView(card);
    }

    private void markFeedbackReviewed(
            String feedbackId
    ) {
        firestore.collection("feedback")
                .document(feedbackId)
                .update(
                        "status",
                        getString(
                                R.string.feedback_status_reviewed
                        )
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            ReviewFeedbackActivity.this,
                            R.string.feedback_marked_reviewed,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadFeedback();
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                ReviewFeedbackActivity.this,
                                R.string.unable_to_update_feedback,
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void confirmDeleteFeedback(
            String feedbackId
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.delete_feedback_title
                )
                .setMessage(
                        R.string.delete_feedback_message
                )
                .setPositiveButton(
                        R.string.delete_feedback,
                        (dialog, which) ->
                                deleteFeedback(feedbackId)
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void deleteFeedback(
            String feedbackId
    ) {
        firestore.collection("feedback")
                .document(feedbackId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            ReviewFeedbackActivity.this,
                            R.string.feedback_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadFeedback();
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                ReviewFeedbackActivity.this,
                                R.string.unable_to_delete_feedback,
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private String getDocumentString(
            DocumentSnapshot document,
            String fieldName
    ) {
        String value =
                document.getString(fieldName);

        return value == null ? "" : value;
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

    private void addTextWithTopMargin(
            LinearLayout container,
            TextView textView,
            int marginDp
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin =
                dpToPixels(marginDp);

        textView.setLayoutParams(params);
        container.addView(textView);
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void openMainActivity() {
        Intent intent =
                new Intent(
                        ReviewFeedbackActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void openLoginActivity() {
        Intent intent =
                new Intent(
                        ReviewFeedbackActivity.this,
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