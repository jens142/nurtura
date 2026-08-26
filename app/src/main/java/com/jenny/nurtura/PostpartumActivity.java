package com.jenny.nurtura;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class PostpartumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postpartum);

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        findViewById(R.id.recoveryCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            PostpartumActivity.this,
                            RecoveryCheckInActivity.class
                    );
                    startActivity(intent);
                });
        findViewById(R.id.anxietyCard)
                .setOnClickListener(view -> {
                    Intent intent = new Intent(
                            PostpartumActivity.this,
                            AnxietySupportActivity.class
                    );

                    startActivity(intent);
                });
    }
}