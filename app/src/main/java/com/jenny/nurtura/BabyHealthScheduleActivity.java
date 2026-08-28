package com.jenny.nurtura;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BabyHealthScheduleActivity extends AppCompatActivity {

    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private static final String[] SCHEDULE_IDS = {
            "bcg",
            "two_month_vaccines",
            "four_month_vaccines",
            "six_month_vaccines",
            "nine_month_mmr",
            "twelve_month_je",
            "eighteen_month_vaccines",
            "three_year_mmr",
            "five_year_vaccines"
    };

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private MaterialButton birthDateButton;
    private TextView babyAgeTextView;
    private TextView needsBirthDateTextView;
    private TextView noCustomVaccinationsTextView;
    private TextView noBabyAppointmentsTextView;
    private LinearLayout officialVaccineContainer;
    private LinearLayout customVaccinationsContainer;
    private LinearLayout babyAppointmentsContainer;
    private CircularProgressIndicator progressIndicator;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private long babyBirthDateMillis = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // The system stores the user's choice. Reminders remain
                    // scheduled and will display only when permission exists.
                }
        );

        setContentView(R.layout.activity_baby_health_schedule);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        birthDateButton = findViewById(R.id.babyBirthDateButton);
        babyAgeTextView = findViewById(R.id.babyAgeTextView);
        needsBirthDateTextView = findViewById(R.id.scheduleNeedsBirthDateTextView);
        noCustomVaccinationsTextView = findViewById(R.id.noCustomVaccinationsTextView);
        noBabyAppointmentsTextView = findViewById(R.id.noBabyAppointmentsTextView);
        officialVaccineContainer = findViewById(R.id.officialVaccineContainer);
        customVaccinationsContainer = findViewById(R.id.customVaccinationsContainer);
        babyAppointmentsContainer = findViewById(R.id.babyAppointmentsContainer);
        progressIndicator = findViewById(R.id.babyScheduleProgressIndicator);

        findViewById(R.id.babyScheduleBackButton)
                .setOnClickListener(view -> finish());

        birthDateButton.setOnClickListener(view -> showBirthDatePicker());

        findViewById(R.id.addCustomVaccinationButton)
                .setOnClickListener(view -> showCustomVaccinationDialog());

        findViewById(R.id.addBabyAppointmentButton)
                .setOnClickListener(view -> showAppointmentDialog());

        ReminderWorker.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
        loadBabyProfile();
        loadAppointments();
    }

    private void loadBabyProfile() {
        showLoading(true);

        getBabyProfileDocument().get()
                .addOnSuccessListener(document -> {
                    Long birthDate = document.getLong("birthDateMillis");

                    if (birthDate != null) {
                        babyBirthDateMillis = birthDate;
                        updateBabyDetails();
                    } else {
                        showBirthDateRequired();
                    }

                    loadVaccinationRecords();
                })
                .addOnFailureListener(exception -> {
                    showBirthDateRequired();
                    loadVaccinationRecords();

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_load_baby_schedule,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void showBirthDatePicker() {
        Calendar calendar = Calendar.getInstance();

        if (babyBirthDateMillis > 0) {
            calendar.setTimeInMillis(babyBirthDateMillis);
        }

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {
                    Calendar selectedDate = createDate(year, month, day);
                    saveBabyBirthDate(selectedDate.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void saveBabyBirthDate(long birthDateMillis) {
        Map<String, Object> babyProfile = new HashMap<>();
        babyProfile.put("birthDateMillis", birthDateMillis);
        babyProfile.put("updatedAt", FieldValue.serverTimestamp());

        showLoading(true);

        getBabyProfileDocument()
                .set(babyProfile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    babyBirthDateMillis = birthDateMillis;
                    updateBabyDetails();
                    loadVaccinationRecords();

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.baby_birth_date_saved,
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_save_baby_birth_date,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void updateBabyDetails() {
        birthDateButton.setText(
                getString(
                        R.string.baby_birth_date_value,
                        formatDate(babyBirthDateMillis)
                )
        );

        babyAgeTextView.setText(
                getString(
                        R.string.baby_age_months_value,
                        calculateAgeInMonths(babyBirthDateMillis)
                )
        );
    }

    private int calculateAgeInMonths(long birthDateMillis) {
        Calendar birthDate = Calendar.getInstance();
        birthDate.setTimeInMillis(birthDateMillis);

        Calendar today = Calendar.getInstance();

        int months = (today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)) * 12;
        months += today.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH);

        if (today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH)) {
            months--;
        }

        return Math.max(months, 0);
    }

    private void showBirthDateRequired() {
        birthDateButton.setText(R.string.select_baby_birth_date);
        babyAgeTextView.setText(R.string.vaccination_schedule_needs_birth_date);
        needsBirthDateTextView.setVisibility(View.VISIBLE);
        officialVaccineContainer.removeAllViews();
    }

    private void loadVaccinationRecords() {
        showLoading(true);

        getVaccinationRecords().get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Long> receivedDates = new HashMap<>();
                    List<DocumentSnapshot> customVaccinations = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Boolean custom = document.getBoolean("isCustom");

                        if (Boolean.TRUE.equals(custom)) {
                            customVaccinations.add(document);
                            continue;
                        }

                        Long receivedDate = document.getLong("receivedDateMillis");

                        if (receivedDate != null) {
                            receivedDates.put(document.getId(), receivedDate);
                        }
                    }

                    customVaccinations.sort(
                            (first, second) -> Long.compare(
                                    getDocumentLong(second, "receivedDateMillis"),
                                    getDocumentLong(first, "receivedDateMillis")
                            )
                    );

                    displayOfficialSchedule(receivedDates);
                    displayCustomVaccinations(customVaccinations);
                    showLoading(false);
                })
                .addOnFailureListener(exception -> {
                    displayOfficialSchedule(new HashMap<>());
                    displayCustomVaccinations(new ArrayList<>());
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_load_baby_schedule,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void displayOfficialSchedule(Map<String, Long> receivedDates) {
        officialVaccineContainer.removeAllViews();

        if (babyBirthDateMillis <= 0) {
            needsBirthDateTextView.setVisibility(View.VISIBLE);
            return;
        }

        needsBirthDateTextView.setVisibility(View.GONE);

        String[] vaccineNames = getResources().getStringArray(
                R.array.sri_lanka_vaccine_names
        );

        String[] vaccineAges = getResources().getStringArray(
                R.array.sri_lanka_vaccine_ages
        );

        int[] dueMonths = getResources().getIntArray(
                R.array.sri_lanka_vaccine_due_months
        );

        int itemCount = Math.min(
                SCHEDULE_IDS.length,
                Math.min(
                        vaccineNames.length,
                        Math.min(vaccineAges.length, dueMonths.length)
                )
        );

        for (int index = 0; index < itemCount; index++) {
            long targetDate = addMonths(babyBirthDateMillis, dueMonths[index]);

            Long receivedDate = receivedDates.get(SCHEDULE_IDS[index]);

            addOfficialVaccineCard(
                    SCHEDULE_IDS[index],
                    vaccineNames[index],
                    vaccineAges[index],
                    targetDate,
                    receivedDate
            );

            if (receivedDate == null) {
                scheduleVaccinationReminder(
                        SCHEDULE_IDS[index],
                        vaccineNames[index],
                        targetDate
                );
            } else {
                cancelReminder(getVaccineWorkName(SCHEDULE_IDS[index]));
            }
        }
    }

    private void addOfficialVaccineCard(
            String scheduleId,
            String vaccineName,
            String vaccineAge,
            long targetDate,
            Long receivedDate
    ) {
        MaterialCardView card = createRecordCard();
        LinearLayout content = createCardContent();

        content.addView(createTextView(vaccineName, 17, true));

        addDetailText(
                content,
                getString(R.string.vaccination_age_value, vaccineAge)
        );

        addDetailText(
                content,
                getString(
                        R.string.vaccination_target_date_value,
                        formatDate(targetDate)
                )
        );

        if (receivedDate != null) {
            TextView receivedText = createTextView(
                    getString(
                            R.string.vaccination_received_value,
                            formatDate(receivedDate)
                    ),
                    14,
                    true
            );

            receivedText.setTextColor(
                    ContextCompat.getColor(this, R.color.baby_accent)
            );
            addViewWithTopMargin(content, receivedText, 8);
        }

        MaterialButton statusButton = createEndButton();
        statusButton.setText(
                receivedDate == null
                        ? R.string.mark_vaccination_received
                        : R.string.edit_vaccination_date
        );

        statusButton.setOnClickListener(
                view -> showReceivedDatePicker(
                        scheduleId,
                        vaccineName,
                        vaccineAge,
                        targetDate,
                        receivedDate
                )
        );

        content.addView(statusButton);
        card.addView(content);
        officialVaccineContainer.addView(card);
    }

    private void showReceivedDatePicker(
            String scheduleId,
            String vaccineName,
            String vaccineAge,
            long targetDate,
            Long existingReceivedDate
    ) {
        Calendar calendar = Calendar.getInstance();

        if (existingReceivedDate != null) {
            calendar.setTimeInMillis(existingReceivedDate);
        }

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> saveOfficialVaccination(
                        scheduleId,
                        vaccineName,
                        vaccineAge,
                        targetDate,
                        createDate(year, month, day).getTimeInMillis()
                ),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void saveOfficialVaccination(
            String scheduleId,
            String vaccineName,
            String vaccineAge,
            long targetDate,
            long receivedDate
    ) {
        Map<String, Object> vaccination = new HashMap<>();
        vaccination.put("scheduleId", scheduleId);
        vaccination.put("name", vaccineName);
        vaccination.put("recommendedAge", vaccineAge);
        vaccination.put("targetDateMillis", targetDate);
        vaccination.put("receivedDateMillis", receivedDate);
        vaccination.put("isCustom", false);
        vaccination.put("updatedAt", FieldValue.serverTimestamp());

        showLoading(true);

        getVaccinationRecords()
                .document(scheduleId)
                .set(vaccination, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    cancelReminder(getVaccineWorkName(scheduleId));

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.vaccination_recorded,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadVaccinationRecords();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_save_vaccination,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void showCustomVaccinationDialog() {
        LinearLayout content = createDialogContent();
        EditText nameField = createDialogField(
                getString(R.string.custom_vaccination_name_hint),
                false
        );
        MaterialButton dateButton = createDialogDateButton(
                R.string.vaccination_date_button
        );
        EditText clinicField = createDialogField(
                getString(R.string.clinic_provider_hint),
                false
        );
        EditText notesField = createDialogField(
                getString(R.string.baby_health_notes_hint),
                true
        );

        addViewWithTopMargin(content, nameField, 4);
        addViewWithTopMargin(content, dateButton, 10);
        addViewWithTopMargin(content, clinicField, 10);
        addViewWithTopMargin(content, notesField, 10);

        long[] selectedDate = {-1L};

        dateButton.setOnClickListener(view -> showDialogDatePicker(
                selectedDate,
                dateButton,
                false
        ));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_custom_vaccination)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_custom_vaccination, null)
                .create();

        dialog.setOnShowListener(unused -> dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = getInputText(nameField);

                    if (name.isEmpty()) {
                        nameField.setError(
                                getString(R.string.vaccination_name_required)
                        );
                        nameField.requestFocus();
                        return;
                    }

                    if (selectedDate[0] <= 0) {
                        Toast.makeText(
                                BabyHealthScheduleActivity.this,
                                R.string.vaccination_date_required,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    saveCustomVaccination(
                            name,
                            selectedDate[0],
                            getInputText(clinicField),
                            getInputText(notesField),
                            dialog
                    );
                }));

        dialog.show();
    }

    private void saveCustomVaccination(
            String name,
            long receivedDate,
            String clinic,
            String notes,
            AlertDialog dialog
    ) {
        Map<String, Object> vaccination = new HashMap<>();
        vaccination.put("name", name);
        vaccination.put("receivedDateMillis", receivedDate);
        vaccination.put("clinic", clinic);
        vaccination.put("notes", notes);
        vaccination.put("isCustom", true);
        vaccination.put("createdAtMillis", System.currentTimeMillis());
        vaccination.put("createdAt", FieldValue.serverTimestamp());

        showLoading(true);

        getVaccinationRecords().add(vaccination)
                .addOnSuccessListener(document -> {
                    dialog.dismiss();

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.custom_vaccination_saved,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadVaccinationRecords();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_save_vaccination,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void displayCustomVaccinations(List<DocumentSnapshot> documents) {
        customVaccinationsContainer.removeAllViews();

        if (documents.isEmpty()) {
            noCustomVaccinationsTextView.setVisibility(View.VISIBLE);
            return;
        }

        noCustomVaccinationsTextView.setVisibility(View.GONE);

        for (DocumentSnapshot document : documents) {
            String name = getDocumentString(document, "name");
            Long date = document.getLong("receivedDateMillis");

            if (name.isEmpty() || date == null) {
                continue;
            }

            MaterialCardView card = createRecordCard();
            LinearLayout content = createCardContent();
            content.addView(createTextView(name, 17, true));

            addDetailText(
                    content,
                    getString(
                            R.string.vaccination_received_value,
                            formatDate(date)
                    )
            );

            addOptionalDetail(
                    content,
                    getDocumentString(document, "clinic"),
                    R.string.baby_health_clinic_value
            );

            addOptionalDetail(
                    content,
                    getDocumentString(document, "notes"),
                    R.string.baby_health_notes_value
            );

            MaterialButton deleteButton = createDeleteButton();
            deleteButton.setOnClickListener(
                    view -> confirmDeleteVaccination(document.getId())
            );

            content.addView(deleteButton);
            card.addView(content);
            customVaccinationsContainer.addView(card);
        }
    }

    private void showAppointmentDialog() {
        LinearLayout content = createDialogContent();
        EditText purposeField = createDialogField(
                getString(R.string.appointment_purpose_hint),
                false
        );
        MaterialButton dateButton = createDialogDateButton(
                R.string.baby_appointment_date_button
        );
        EditText clinicField = createDialogField(
                getString(R.string.clinic_provider_hint),
                false
        );
        EditText notesField = createDialogField(
                getString(R.string.baby_health_notes_hint),
                true
        );

        addViewWithTopMargin(content, purposeField, 4);
        addViewWithTopMargin(content, dateButton, 10);
        addViewWithTopMargin(content, clinicField, 10);
        addViewWithTopMargin(content, notesField, 10);

        long[] selectedDate = {-1L};

        dateButton.setOnClickListener(view -> showDialogDatePicker(
                selectedDate,
                dateButton,
                true
        ));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_baby_appointment)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_baby_appointment, null)
                .create();

        dialog.setOnShowListener(unused -> dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String purpose = getInputText(purposeField);

                    if (purpose.isEmpty()) {
                        purposeField.setError(
                                getString(R.string.appointment_purpose_required)
                        );
                        purposeField.requestFocus();
                        return;
                    }

                    if (selectedDate[0] <= 0) {
                        Toast.makeText(
                                BabyHealthScheduleActivity.this,
                                R.string.appointment_date_required,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    saveAppointment(
                            purpose,
                            selectedDate[0],
                            getInputText(clinicField),
                            getInputText(notesField),
                            dialog
                    );
                }));

        dialog.show();
    }

    private void saveAppointment(
            String purpose,
            long appointmentDate,
            String clinic,
            String notes,
            AlertDialog dialog
    ) {
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("purpose", purpose);
        appointment.put("dateMillis", appointmentDate);
        appointment.put("clinic", clinic);
        appointment.put("notes", notes);
        appointment.put("createdAtMillis", System.currentTimeMillis());
        appointment.put("createdAt", FieldValue.serverTimestamp());

        showLoading(true);

        getAppointmentRecords().add(appointment)
                .addOnSuccessListener(document -> {
                    if (appointmentDate < startOfToday() + ONE_DAY_MILLIS) {
                        ReminderWorker.showNotification(
                                BabyHealthScheduleActivity.this,
                                getString(
                                        R.string.appointment_reminder_notification_title
                                ),
                                getString(
                                        R.string.appointment_today_notification_message,
                                        purpose
                                ),
                                getAppointmentWorkName(document.getId())
                                        .hashCode() & Integer.MAX_VALUE
                        );
                    } else {
                        scheduleAppointmentReminder(
                                document.getId(),
                                purpose,
                                appointmentDate
                        );
                    }

                    dialog.dismiss();

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.baby_appointment_saved,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadAppointments();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_save_baby_appointment,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadAppointments() {
        getAppointmentRecords()
                .orderBy("dateMillis", Query.Direction.ASCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(snapshot -> {
                    babyAppointmentsContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        noBabyAppointmentsTextView.setVisibility(View.VISIBLE);
                        showLoading(false);
                        return;
                    }

                    noBabyAppointmentsTextView.setVisibility(View.GONE);

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        addAppointmentCard(document);
                    }

                    showLoading(false);
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_load_baby_schedule,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void addAppointmentCard(DocumentSnapshot document) {
        String purpose = getDocumentString(document, "purpose");
        Long appointmentDate = document.getLong("dateMillis");

        if (purpose.isEmpty() || appointmentDate == null) {
            return;
        }

        MaterialCardView card = createRecordCard();
        LinearLayout content = createCardContent();
        content.addView(createTextView(purpose, 17, true));

        addDetailText(content, formatDate(appointmentDate));

        addOptionalDetail(
                content,
                getDocumentString(document, "clinic"),
                R.string.baby_health_clinic_value
        );

        addOptionalDetail(
                content,
                getDocumentString(document, "notes"),
                R.string.baby_health_notes_value
        );

        MaterialButton deleteButton = createDeleteButton();
        deleteButton.setOnClickListener(
                view -> confirmDeleteAppointment(document.getId())
        );

        content.addView(deleteButton);
        card.addView(content);
        babyAppointmentsContainer.addView(card);
    }

    private void showDialogDatePicker(
            long[] selectedDate,
            MaterialButton dateButton,
            boolean allowFutureDates
    ) {
        Calendar calendar = Calendar.getInstance();

        if (selectedDate[0] > 0) {
            calendar.setTimeInMillis(selectedDate[0]);
        }

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {
                    selectedDate[0] = createDate(
                            year,
                            month,
                            day
                    ).getTimeInMillis();

                    dateButton.setText(formatDate(selectedDate[0]));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        if (!allowFutureDates) {
            picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        } else {
            picker.getDatePicker().setMinDate(startOfToday());
        }

        picker.show();
    }

    private void confirmDeleteVaccination(String documentId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) -> deleteVaccination(documentId)
                )
                .show();
    }

    private void deleteVaccination(String documentId) {
        showLoading(true);

        getVaccinationRecords().document(documentId).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadVaccinationRecords();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_delete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void confirmDeleteAppointment(String documentId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) -> deleteAppointment(documentId)
                )
                .show();
    }

    private void deleteAppointment(String documentId) {
        showLoading(true);

        getAppointmentRecords().document(documentId).delete()
                .addOnSuccessListener(unused -> {
                    cancelReminder(getAppointmentWorkName(documentId));
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.record_deleted,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadAppointments();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    Toast.makeText(
                            BabyHealthScheduleActivity.this,
                            R.string.unable_to_delete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private MaterialCardView createRecordCard() {
        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams parameters = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        parameters.bottomMargin = dpToPixels(12);
        card.setLayoutParams(parameters);
        card.setRadius(dpToPixels(17));
        card.setCardElevation(dpToPixels(2));
        card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.nurtura_surface)
        );
        card.setStrokeColor(
                ContextCompat.getColor(this, R.color.baby_accent)
        );
        card.setStrokeWidth(dpToPixels(1));

        return card;
    }

    private LinearLayout createCardContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(17),
                dpToPixels(15),
                dpToPixels(17),
                dpToPixels(12)
        );
        return content;
    }

    private MaterialButton createEndButton() {
        MaterialButton button = new MaterialButton(this);
        button.setAllCaps(false);

        LinearLayout.LayoutParams parameters = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        parameters.gravity = Gravity.END;
        parameters.topMargin = dpToPixels(8);
        button.setLayoutParams(parameters);
        return button;
    }

    private MaterialButton createDeleteButton() {
        MaterialButton button = createEndButton();
        button.setText(R.string.delete);
        button.setTextColor(
                ContextCompat.getColor(this, R.color.nurtura_error)
        );
        button.setBackgroundTintList(
                ColorStateList.valueOf(Color.TRANSPARENT)
        );
        return button;
    }

    private LinearLayout createDialogContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dpToPixels(24),
                dpToPixels(8),
                dpToPixels(24),
                0
        );
        return content;
    }

    private EditText createDialogField(String hint, boolean multiline) {
        EditText field = new EditText(this);
        field.setHint(hint);

        if (multiline) {
            field.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                            | InputType.TYPE_TEXT_FLAG_MULTI_LINE
            );
            field.setGravity(Gravity.TOP);
            field.setMinLines(3);
        } else {
            field.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            );
        }

        return field;
    }

    private MaterialButton createDialogDateButton(int textResource) {
        MaterialButton button = new MaterialButton(this);
        button.setText(textResource);
        button.setAllCaps(false);
        return button;
    }

    private TextView createTextView(String text, int textSize, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(
                ContextCompat.getColor(this, R.color.nurtura_text)
        );

        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return textView;
    }

    private void addDetailText(LinearLayout container, String text) {
        TextView textView = createTextView(text, 13, false);
        textView.setTextColor(
                ContextCompat.getColor(this, R.color.nurtura_text_secondary)
        );
        addViewWithTopMargin(container, textView, 5);
    }

    private void addOptionalDetail(
            LinearLayout container,
            String value,
            int stringResource
    ) {
        if (!value.isEmpty()) {
            addDetailText(container, getString(stringResource, value));
        }
    }

    private void addViewWithTopMargin(
            LinearLayout container,
            View view,
            int marginDp
    ) {
        LinearLayout.LayoutParams parameters = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parameters.topMargin = dpToPixels(marginDp);
        view.setLayoutParams(parameters);
        container.addView(view);
    }

    private String getInputText(EditText field) {
        if (field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim();
    }

    private String getDocumentString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private long getDocumentLong(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0L : value;
    }

    private DocumentReference getBabyProfileDocument() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("babyProfile")
                .document("details");
    }

    private CollectionReference getVaccinationRecords() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("babyVaccinations");
    }

    private CollectionReference getAppointmentRecords() {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .collection("babyAppointments");
    }

    private Calendar createDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private long addMonths(long dateMillis, int numberOfMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.add(Calendar.MONTH, numberOfMonths);
        return calendar.getTimeInMillis();
    }

    private long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }
    }

    private void scheduleVaccinationReminder(
            String scheduleId,
            String vaccineName,
            long targetDate
    ) {
        long reminderTime = atNineInMorning(targetDate, 0);
        String workName = getVaccineWorkName(scheduleId);

        if (reminderTime <= System.currentTimeMillis()) {
            cancelReminder(workName);
            return;
        }

        scheduleReminder(
                workName,
                reminderTime,
                getString(R.string.vaccination_due_notification_title),
                getString(
                        R.string.vaccination_due_notification_message,
                        vaccineName
                )
        );
    }

    private void scheduleAppointmentReminder(
            String documentId,
            String purpose,
            long appointmentDate
    ) {
        long reminderTime = atNineInMorning(appointmentDate, -1);
        String message;

        if (appointmentDate < startOfToday() + ONE_DAY_MILLIS) {
            message = getString(
                    R.string.appointment_today_notification_message,
                    purpose
            );
        } else {
            message = getString(
                    R.string.appointment_reminder_notification_message,
                    purpose
            );
        }

        if (reminderTime <= System.currentTimeMillis()) {
            reminderTime = System.currentTimeMillis();
        }

        scheduleReminder(
                getAppointmentWorkName(documentId),
                reminderTime,
                getString(R.string.appointment_reminder_notification_title),
                message
        );
    }

    private void scheduleReminder(
            String uniqueWorkName,
            long reminderTime,
            String title,
            String message
    ) {
        long delay = Math.max(
                reminderTime - System.currentTimeMillis(),
                0L
        );

        Data reminderData = new Data.Builder()
                .putString(ReminderWorker.KEY_TITLE, title)
                .putString(ReminderWorker.KEY_MESSAGE, message)
                .putInt(
                        ReminderWorker.KEY_NOTIFICATION_ID,
                        uniqueWorkName.hashCode() & Integer.MAX_VALUE
                )
                .build();

        OneTimeWorkRequest.Builder requestBuilder =
                new OneTimeWorkRequest.Builder(ReminderWorker.class)
                        .setInputData(reminderData);

        if (delay > 0L) {
            requestBuilder.setInitialDelay(delay, TimeUnit.MILLISECONDS);
        } else {
            requestBuilder.setExpedited(
                    OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
            );
        }

        OneTimeWorkRequest request = requestBuilder.build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    private long atNineInMorning(long dateMillis, int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String getVaccineWorkName(String scheduleId) {
        return "baby_vaccine_"
                + currentUser.getUid()
                + "_"
                + scheduleId;
    }

    private String getAppointmentWorkName(String documentId) {
        return "baby_appointment_"
                + currentUser.getUid()
                + "_"
                + documentId;
    }

    private void cancelReminder(String uniqueWorkName) {
        WorkManager.getInstance(this).cancelUniqueWork(uniqueWorkName);
    }

    private String formatDate(long dateMillis) {
        SimpleDateFormat format = new SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
        );
        return format.format(new Date(dateMillis));
    }

    private void showLoading(boolean loading) {
        progressIndicator.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
    }

    private int dpToPixels(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }
}
