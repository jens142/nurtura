package com.jenny.nurtura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextView welcomeTextView;
    private TextView lifeStageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        welcomeTextView =
                findViewById(R.id.welcomeTextView);

        lifeStageTextView =
                findViewById(R.id.lifeStageTextView);

        MaterialButton logoutButton =
                findViewById(R.id.logoutButton);

        loadUserProfile();

        findViewById(R.id.lifeStageCard)
                .setOnClickListener(view ->
                        showLifeStageDialog()
                );

        findViewById(R.id.menstrualCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            MainActivity.this,
                            MenstrualActivity.class
                    );

                    startActivity(intent);
                });

        findViewById(R.id.pregnancyCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            MainActivity.this,
                            MaternalActivity.class
                    );

                    startActivity(intent);
                });

        findViewById(R.id.postpartumCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            MainActivity.this,
                            PostpartumActivity.class
                    );

                    startActivity(intent);
                });

        findViewById(R.id.babyCareCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            MainActivity.this,
                            BabyCareActivity.class
                    );

                    startActivity(intent);
                });

        findViewById(R.id.wellnessCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            MainActivity.this,
                            WellnessActivity.class
                    );

                    startActivity(intent);
                });

        logoutButton.setOnClickListener(view -> {
            firebaseAuth.signOut();
            openLoginActivity();
        });
    }

    private void loadUserProfile() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    String name =
                            document.getString("name");

                    String lifeStage =
                            document.getString(
                                    "lifeStage"
                            );

                    if (name != null
                            && !name.isEmpty()) {

                        welcomeTextView.setText(
                                getString(
                                        R.string.welcome_user,
                                        name
                                )
                        );
                    }

                    if (lifeStage != null
                            && !lifeStage.isEmpty()
                            && !lifeStage.equals(
                            getString(
                                    R.string.not_selected
                            )
                    )) {

                        lifeStageTextView.setText(
                                lifeStage
                        );
                    }
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                MainActivity.this,
                                R.string.unable_to_load_profile,
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void showLifeStageDialog() {
        String[] lifeStages =
                getResources().getStringArray(
                        R.array.life_stage_options
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.choose_life_stage
                )
                .setItems(
                        lifeStages,
                        (dialog, selectedIndex) ->
                                saveLifeStage(
                                        lifeStages[
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

    private void saveLifeStage(
            String lifeStage
    ) {
        firestore.collection("users")
                .document(currentUser.getUid())
                .update(
                        "lifeStage",
                        lifeStage
                )
                .addOnSuccessListener(unused -> {
                    lifeStageTextView.setText(
                            lifeStage
                    );

                    Toast.makeText(
                            MainActivity.this,
                            R.string.life_stage_updated,
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                MainActivity.this,
                                R.string.unable_to_update_life_stage,
                                Toast.LENGTH_SHORT
                        ).show()
                );
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