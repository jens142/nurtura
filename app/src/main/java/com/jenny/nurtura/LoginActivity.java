package com.jenny.nurtura;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private ProgressBar loginProgressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        emailInputLayout =
                findViewById(R.id.emailInputLayout);

        passwordInputLayout =
                findViewById(R.id.passwordInputLayout);

        emailEditText =
                findViewById(R.id.emailEditText);

        passwordEditText =
                findViewById(R.id.passwordEditText);

        loginButton =
                findViewById(R.id.loginButton);

        loginProgressBar =
                findViewById(R.id.loginProgressBar);

        TextView forgotPasswordTextView =
                findViewById(R.id.forgotPasswordTextView);

        TextView createAccountTextView =
                findViewById(R.id.createAccountTextView);

        loginButton.setOnClickListener(
                view -> loginUser()
        );

        createAccountTextView.setOnClickListener(view -> {
            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    );

            startActivity(intent);
        });

        forgotPasswordTextView.setOnClickListener(
                view -> sendPasswordResetEmail()
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (firebaseAuth.getCurrentUser() != null) {
            routeAuthenticatedUser();
        }
    }

    private void loginUser() {
        String email =
                getInputText(emailEditText);

        String password =
                getInputText(passwordEditText);

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        if (email.isEmpty()) {
            emailInputLayout.setError(
                    "Email is required"
            );

            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            emailInputLayout.setError(
                    "Enter a valid email address"
            );

            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError(
                    "Password is required"
            );

            passwordEditText.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Login successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        routeAuthenticatedUser();
                    } else {
                        setLoading(false);

                        Toast.makeText(
                                LoginActivity.this,
                                "Login failed. Check your email and password.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void routeAuthenticatedUser() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            setLoading(false);
            return;
        }

        setLoading(true);

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role =
                            documentSnapshot.getString("role");

                    if ("admin".equalsIgnoreCase(role)) {
                        openAdminDashboard();
                    } else {
                        openMainActivity();
                    }
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(
                            LoginActivity.this,
                            R.string.unable_to_load_account_role,
                            Toast.LENGTH_SHORT
                    ).show();

                    openMainActivity();
                });
    }

    private void sendPasswordResetEmail() {
        String email =
                getInputText(emailEditText);

        emailInputLayout.setError(null);

        if (email.isEmpty()) {
            emailInputLayout.setError(
                    "Enter your email address first"
            );

            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            emailInputLayout.setError(
                    "Enter a valid email address"
            );

            emailEditText.requestFocus();
            return;
        }

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                LoginActivity.this,
                                "Unable to send reset email",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String getInputText(
            TextInputEditText input
    ) {
        if (input.getText() == null) {
            return "";
        }

        return input.getText()
                .toString()
                .trim();
    }

    private void setLoading(boolean loading) {
        loginProgressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        loginButton.setEnabled(!loading);
    }

    private void openAdminDashboard() {
        Intent intent =
                new Intent(
                        LoginActivity.this,
                        AdminDashboardActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void openMainActivity() {
        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}