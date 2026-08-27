package com.jenny.nurtura;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class SupportContactsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextInputEditText nameEditText;
    private TextInputEditText relationshipEditText;
    private TextInputEditText phoneEditText;

    private LinearLayout contactsContainer;
    private TextView noContactsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_contacts);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        nameEditText = findViewById(
                R.id.contactNameEditText
        );

        relationshipEditText = findViewById(
                R.id.contactRelationshipEditText
        );

        phoneEditText = findViewById(
                R.id.contactPhoneEditText
        );

        contactsContainer = findViewById(
                R.id.contactsContainer
        );

        noContactsTextView = findViewById(
                R.id.noContactsTextView
        );

        findViewById(R.id.backButton)
                .setOnClickListener(view -> finish());

        findViewById(R.id.saveContactButton)
                .setOnClickListener(
                        view -> saveContact()
                );

        loadContacts();
    }

    private void saveContact() {
        String name = getTextFromField(nameEditText);
        String relationship =
                getTextFromField(relationshipEditText);
        String phone = getTextFromField(phoneEditText);

        if (TextUtils.isEmpty(name)
                || TextUtils.isEmpty(relationship)
                || TextUtils.isEmpty(phone)) {
            Toast.makeText(
                    this,
                    R.string.complete_support_contact_details,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("relationship", relationship);
        contact.put("phone", phone);
        contact.put(
                "createdAtMillis",
                System.currentTimeMillis()
        );
        contact.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumSupportContacts")
                .add(contact)
                .addOnSuccessListener(
                        documentReference -> {
                            Toast.makeText(
                                    SupportContactsActivity.this,
                                    R.string.support_contact_saved,
                                    Toast.LENGTH_SHORT
                            ).show();

                            clearForm();
                            loadContacts();
                        }
                )
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        SupportContactsActivity.this,
                                        R.string.unable_to_save_contact,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void loadContacts() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumSupportContacts")
                .orderBy(
                        "createdAtMillis",
                        Query.Direction.DESCENDING
                )
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    contactsContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        noContactsTextView.setVisibility(
                                View.VISIBLE
                        );

                        return;
                    }

                    noContactsTextView.setVisibility(
                            View.GONE
                    );

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {
                        addContactCard(document);
                    }
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        SupportContactsActivity.this,
                                        R.string.unable_to_load_contacts,
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void addContactCard(
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

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(18),
                dpToPixels(16),
                dpToPixels(18),
                dpToPixels(16)
        );

        TextView nameText = createTextView(
                getDocumentString(document, "name"),
                17,
                true
        );

        content.addView(nameText);

        addDetailText(
                content,
                getString(
                        R.string.support_contact_relationship_value,
                        getDocumentString(
                                document,
                                "relationship"
                        )
                )
        );

        String phone =
                getDocumentString(document, "phone");

        addDetailText(
                content,
                getString(
                        R.string.support_contact_phone_value,
                        phone
                )
        );

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        LinearLayout.LayoutParams actionsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        actionsParams.topMargin = dpToPixels(8);
        actions.setLayoutParams(actionsParams);

        MaterialButton callButton =
                new MaterialButton(this);

        callButton.setText(
                R.string.call_support_contact
        );

        callButton.setOnClickListener(
                view -> openDialler(phone)
        );

        MaterialButton deleteButton =
                new MaterialButton(this);

        deleteButton.setText(R.string.delete);

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        deleteParams.setMarginStart(dpToPixels(8));
        deleteButton.setLayoutParams(deleteParams);

        deleteButton.setOnClickListener(
                view -> confirmDelete(document.getId())
        );

        actions.addView(callButton);
        actions.addView(deleteButton);
        content.addView(actions);

        card.addView(content);
        contactsContainer.addView(card);
    }

    private void openDialler(String phone) {
        Intent intent = new Intent(
                Intent.ACTION_DIAL,
                Uri.fromParts("tel", phone, null)
        );

        startActivity(intent);
    }

    private void addDetailText(
            LinearLayout container,
            String text
    ) {
        TextView textView = createTextView(
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
        TextView textView = new TextView(this);
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

    private void confirmDelete(String documentId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) ->
                                deleteContact(documentId)
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .show();
    }

    private void deleteContact(String documentId) {
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("postpartumSupportContacts")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            SupportContactsActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadContacts();
                })
                .addOnFailureListener(
                        exception ->
                                Toast.makeText(
                                        SupportContactsActivity.this,
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
        String value = document.getString(fieldName);
        return value == null ? "" : value;
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void clearForm() {
        nameEditText.setText("");
        relationshipEditText.setText("");
        phoneEditText.setText("");
    }

    private void openLoginActivity() {
        Intent intent = new Intent(
                SupportContactsActivity.this,
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