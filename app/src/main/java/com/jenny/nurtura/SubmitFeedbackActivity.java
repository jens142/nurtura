package com.jenny.nurtura;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SubmitFeedbackActivity
        extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextInputLayout messageInputLayout;
    private TextInputEditText messageEditText;
    private MaterialButton submitButton;

    private String userName = "";
    private String userEmail = "";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd MMM yyyy, h:mm a",
                    Locale.getDefault()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_submit_feedback
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

        if (currentUser.getEmail() != null) {
            userEmail = currentUser.getEmail();
            userName = currentUser.getEmail();
        }

        messageInputLayout =
                findViewById(
                        R.id.feedbackMessageInputLayout
                );

        messageEditText =
                findViewById(
                        R.id.feedbackMessageEditText
                );

        submitButton =
                findViewById(
                        R.id.submitFeedbackButton
                );

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        submitButton.setOnClickListener(
                view -> submitFeedback()
        );

        loadUserProfile();
    }

    private void loadUserProfile() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedName =
                            documentSnapshot.getString("name");

                    String savedEmail =
                            documentSnapshot.getString("email");

                    if (!TextUtils.isEmpty(savedName)) {
                        userName = savedName;
                    }

                    if (!TextUtils.isEmpty(savedEmail)) {
                        userEmail = savedEmail;
                    }
                });
    }

    private void submitFeedback() {
        String message =
                getMessageText();

        messageInputLayout.setError(null);

        if (TextUtils.isEmpty(message)) {
            messageInputLayout.setError(
                    getString(
                            R.string.feedback_message_required
                    )
            );

            messageEditText.requestFocus();
            return;
        }

        setSubmitting(true);

        long createdAtMillis =
                System.currentTimeMillis();

        Map<String, Object> feedback =
                new HashMap<>();

        feedback.put(
                "userId",
                currentUser.getUid()
        );

        feedback.put(
                "userName",
                userName
        );

        feedback.put(
                "userEmail",
                userEmail
        );

        feedback.put(
                "message",
                message
        );

        feedback.put(
                "status",
                "New"
        );

        feedback.put(
                "submittedDate",
                dateFormat.format(
                        new Date(createdAtMillis)
                )
        );

        feedback.put(
                "createdAtMillis",
                createdAtMillis
        );

        feedback.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection("feedback")
                .add(feedback)
                .addOnSuccessListener(
                        documentReference -> {
                            Toast.makeText(
                                    SubmitFeedbackActivity.this,
                                    R.string.feedback_submitted,
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(exception -> {
                    setSubmitting(false);

                    Toast.makeText(
                            SubmitFeedbackActivity.this,
                            R.string.unable_to_submit_feedback,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private String getMessageText() {
        if (messageEditText.getText() == null) {
            return "";
        }

        return messageEditText.getText()
                .toString()
                .trim();
    }

    private void setSubmitting(
            boolean submitting
    ) {
        submitButton.setEnabled(!submitting);
        messageEditText.setEnabled(!submitting);
    }

    private void openLoginActivity() {
        Intent intent =
                new Intent(
                        SubmitFeedbackActivity.this,
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