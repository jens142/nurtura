package com.jenny.nurtura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity
        extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextView totalUsersTextView;
    private TextView totalFeedbackTextView;
    private TextView newFeedbackTextView;

    private MaterialCardView reviewFeedbackCard;

    private boolean adminVerified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_admin_dashboard
        );

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        totalUsersTextView =
                findViewById(
                        R.id.totalUsersTextView
                );

        totalFeedbackTextView =
                findViewById(
                        R.id.totalFeedbackTextView
                );

        newFeedbackTextView =
                findViewById(
                        R.id.newFeedbackTextView
                );

        reviewFeedbackCard =
                findViewById(
                        R.id.reviewFeedbackCard
                );

        reviewFeedbackCard.setEnabled(false);

        findViewById(R.id.logoutButton)
                .setOnClickListener(view -> {
                    firebaseAuth.signOut();
                    openLoginActivity();
                });

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
                        adminVerified = true;
                        configureAdminAccess();
                        loadDashboardCounts();
                    } else {
                        Toast.makeText(
                                AdminDashboardActivity.this,
                                R.string.admin_access_denied,
                                Toast.LENGTH_SHORT
                        ).show();

                        openMainActivity();
                    }
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(
                            AdminDashboardActivity.this,
                            R.string.unable_to_verify_admin,
                            Toast.LENGTH_SHORT
                    ).show();

                    openMainActivity();
                });
    }

    private void configureAdminAccess() {
        reviewFeedbackCard.setEnabled(true);

        reviewFeedbackCard.setOnClickListener(view -> {
            Intent intent =
                    new Intent(
                            AdminDashboardActivity.this,
                            ReviewFeedbackActivity.class
                    );

            startActivity(intent);
        });
    }

    private void loadDashboardCounts() {
        loadUserCount();
        loadFeedbackCounts();
    }

    private void loadUserCount() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot ->
                        totalUsersTextView.setText(
                                getString(
                                        R.string.admin_count_value,
                                        querySnapshot.size()
                                )
                        )
                )
                .addOnFailureListener(exception ->
                        showCountLoadError()
                );
    }

    private void loadFeedbackCounts() {
        firestore.collection("feedback")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalFeedback =
                            querySnapshot.size();

                    int newFeedback = 0;

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        String status =
                                document.getString("status");

                        if ("New".equalsIgnoreCase(status)) {
                            newFeedback++;
                        }
                    }

                    totalFeedbackTextView.setText(
                            getString(
                                    R.string.admin_count_value,
                                    totalFeedback
                            )
                    );

                    newFeedbackTextView.setText(
                            getString(
                                    R.string.admin_count_value,
                                    newFeedback
                            )
                    );
                })
                .addOnFailureListener(exception ->
                        showCountLoadError()
                );
    }

    private void showCountLoadError() {
        Toast.makeText(
                AdminDashboardActivity.this,
                R.string.unable_to_load_admin_counts,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adminVerified) {
            loadDashboardCounts();
        }
    }

    private void openMainActivity() {
        Intent intent =
                new Intent(
                        AdminDashboardActivity.this,
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
                        AdminDashboardActivity.this,
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