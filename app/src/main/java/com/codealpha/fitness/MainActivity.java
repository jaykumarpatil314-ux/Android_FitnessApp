package com.codealpha.fitness;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Android Mobile Application in Java
 * CodeAlpha Task 3: Fitness Tracker App
 * Connects to CodeAlpha Java REST Backend on port 8080
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvSteps, tvCalories, tvWorkoutTime, tvWater;
    private ProgressBar pbSteps, pbCalories, pbWorkoutTime, pbWater;
    private Button btnAddSteps, btnAddWater, btnLogWorkout;
    private LinearLayout layoutRecentWorkouts;

    private int steps = 7850;
    private int stepGoal = 10000;
    private int calories = 520;
    private int calorieGoal = 700;
    private int workoutMinutes = 45;
    private int workoutGoal = 60;
    private int waterGlasses = 6;
    private int waterGoal = 8;

    private final List<String> recentWorkouts = new ArrayList<>();
    private static final String BACKEND_URL = "http://10.0.2.2:8080/api/fitness";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        recentWorkouts.add("🏃 Running • 30 mins (310 kcal)");
        recentWorkouts.add("🏋️ Gym & Weights • 45 mins (290 kcal)");

        updateDashboard();

        btnAddSteps.setOnClickListener(v -> {
            steps += 500;
            calories += 20;
            updateDashboard();
            Toast.makeText(this, "+500 Steps logged!", Toast.LENGTH_SHORT).show();
        });

        btnAddWater.setOnClickListener(v -> {
            waterGlasses += 1;
            updateDashboard();
            Toast.makeText(this, "+1 Glass of water logged!", Toast.LENGTH_SHORT).show();
        });

        btnLogWorkout.setOnClickListener(v -> showLogWorkoutDialog());
    }

    private void initViews() {
        tvSteps = findViewById(R.id.tv_steps);
        tvCalories = findViewById(R.id.tv_calories);
        tvWorkoutTime = findViewById(R.id.tv_workout_time);
        tvWater = findViewById(R.id.tv_water);

        pbSteps = findViewById(R.id.pb_steps);
        pbCalories = findViewById(R.id.pb_calories);
        pbWorkoutTime = findViewById(R.id.pb_workout_time);
        pbWater = findViewById(R.id.pb_water);

        btnAddSteps = findViewById(R.id.btn_add_steps);
        btnAddWater = findViewById(R.id.btn_add_water);
        btnLogWorkout = findViewById(R.id.btn_log_workout);
        layoutRecentWorkouts = findViewById(R.id.layout_recent_workouts);
    }

    private void updateDashboard() {
        tvSteps.setText(String.format("%,d / %,d", steps, stepGoal));
        pbSteps.setProgress(Math.min(100, (int) (((float) steps / stepGoal) * 100)));

        tvCalories.setText(String.format("%d / %d kcal", calories, calorieGoal));
        pbCalories.setProgress(Math.min(100, (int) (((float) calories / calorieGoal) * 100)));

        tvWorkoutTime.setText(String.format("%d / %d mins", workoutMinutes, workoutGoal));
        pbWorkoutTime.setProgress(Math.min(100, (int) (((float) workoutMinutes / workoutGoal) * 100)));

        tvWater.setText(String.format("%d / %d glasses", waterGlasses, waterGoal));
        pbWater.setProgress(Math.min(100, (int) (((float) waterGlasses / waterGoal) * 100)));

        // Update list
        layoutRecentWorkouts.removeAllViews();
        for (String w : recentWorkouts) {
            TextView tv = new TextView(this);
            tv.setText(w);
            tv.setTextColor(0xFFE2E8F0);
            tv.setTextSize(13);
            tv.setPadding(0, 8, 0, 8);
            layoutRecentWorkouts.addView(tv);
        }
    }

    private void showLogWorkoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Fitness Workout");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_log_workout, null);
        Spinner spType = view.findViewById(R.id.sp_exercise_type);
        EditText etDuration = view.findViewById(R.id.et_duration);
        EditText etCalories = view.findViewById(R.id.et_calories);

        String[] exercises = {"Running", "Cycling", "Gym & Weights", "Walking", "Yoga", "HIIT Cardio"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, exercises);
        spType.setAdapter(adapter);

        builder.setView(view);
        builder.setPositiveButton("Save Workout", (dialog, which) -> {
            String type = spType.getSelectedItem().toString();
            int dur = 30;
            int cal = 250;
            try {
                dur = Integer.parseInt(etDuration.getText().toString().trim());
                cal = Integer.parseInt(etCalories.getText().toString().trim());
            } catch (Exception ignored) {}

            workoutMinutes += dur;
            calories += cal;
            recentWorkouts.add(0, type + " • " + dur + " mins (" + cal + " kcal)");
            updateDashboard();

            // Sync with Java Backend in background
            final int fDur = dur;
            final int fCal = cal;
            new Thread(() -> syncWorkoutWithJavaBackend(type, fDur, fCal)).start();

            Toast.makeText(this, "Workout saved & synced!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void syncWorkoutWithJavaBackend(String type, int duration, int calories) {
        try {
            URL url = new URL(BACKEND_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);

            String jsonPayload = String.format("{\"type\":\"%s\",\"duration\":\"%d\",\"calories\":\"%d\"}", type, duration, calories);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } catch (Exception ignored) {}
    }
}
