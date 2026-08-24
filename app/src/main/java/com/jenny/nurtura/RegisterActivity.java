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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;

    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;

    private MaterialButton registerButton;
    private ProgressBar registerProgressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout =
                findViewById(R.id.registerEmailInputLayout);
        passwordInputLayout =
                findViewById(R.id.registerPasswordInputLayout);
        confirmPasswordInputLayout =
                findViewById(R.id.confirmPasswordInputLayout);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText =
                findViewById(R.id.registerEmailEditText);
        passwordEditText =
                findViewById(R.id.registerPasswordEditText);
        confirmPasswordEditText =
                findViewById(R.id.confirmPasswordEditText);

        registerButton = findViewById(R.id.registerButton);
        registerProgressBar =
                findViewById(R.id.registerProgressBar);

        TextView signInTextView =
                findViewById(R.id.signInTextView);

        registerButton.setOnClickListener(view ->
                registerUser()
        );

        signInTextView.setOnClickListener(view -> finish());
    }

    private void registerUser() {
        String name = getInputText(nameEditText);
        String email = getInputText(emailEditText);
        String password = getInputText(passwordEditText);
        String confirmedPassword =
                getInputText(confirmPasswordEditText);

        clearErrors();

        if (name.length() < 2) {
            nameInputLayout.setError(
                    "Enter your full name"
            );
            nameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailInputLayout.setError(
                    "Email is required"
            );
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(
                    "Enter a valid email address"
            );
            emailEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInputLayout.setError(
                    "Password must contain at least 6 characters"
            );
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmedPassword)) {
            confirmPasswordInputLayout.setError(
                    "Passwords do not match"
            );
            confirmPasswordEditText.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(
                email,
                password
        ).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user =
                        firebaseAuth.getCurrentUser();

                if (user != null) {
                    saveUserProfile(
                            user.getUid(),
                            name,
                            email
                    );
                } else {
                    setLoading(false);
                    showMessage(
                            "Account created, but user details were unavailable"
                    );
                }
            } else {
                setLoading(false);

                String message = "Registration failed";

                if (task.getException() != null
                        && task.getException()
                        .getLocalizedMessage() != null) {
                    message = task.getException()
                            .getLocalizedMessage();
                }

                showMessage(message);
            }
        });
    }

    private void saveUserProfile(
            String userId,
            String name,
            String email
    ) {
        Map<String, Object> profile = new HashMap<>();

        profile.put("name", name);
        profile.put("email", email);
        profile.put("lifeStage", "Not selected");
        profile.put("profileComplete", false);
        profile.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection("users")
                .document(userId)
                .set(profile)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    showMessage(
                            "Account created successfully"
                    );
                    openMainActivity();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);
                    showMessage(
                            "Account created, but the profile could not be saved"
                    );
                });
    }

    private void clearErrors() {
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
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
        registerProgressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        registerButton.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Toast.makeText(
                RegisterActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void openMainActivity() {
        Intent intent = new Intent(
                RegisterActivity.this,
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