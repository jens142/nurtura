package com.jenny.nurtura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private TextView welcomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        welcomeTextView = findViewById(R.id.welcomeTextView);
        MaterialButton logoutButton =
                findViewById(R.id.logoutButton);

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        loadUserName(currentUser.getUid());

        logoutButton.setOnClickListener(view -> {
            firebaseAuth.signOut();
            openLoginActivity();
        });
    }

    private void loadUserName(String userId) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name =
                                document.getString("name");

                        if (name != null && !name.isEmpty()) {
                            welcomeTextView.setText(
                                    getString(
                                            R.string.welcome_user,
                                            name
                                    )
                            );
                        }
                    }
                });
    }

    private void openLoginActivity() {
        Intent intent = new Intent(
                MainActivity.this,
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