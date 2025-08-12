package com.example.quizapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.quizapp.SpinWheel.LuckyWheelView;
import com.example.quizapp.SpinWheel.model.LuckyItem;
import com.example.quizapp.databinding.ActivitySpinnerBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SpinnerActivity extends AppCompatActivity {

    ActivitySpinnerBinding binding;
    private static final String PREFS_NAME = "QuizAppPrefs";
    private static final String LAST_SPIN_DATE_KEY = "lastSpinDate";
    private static final float DIMMED_ALPHA = 0.5f; // For visually indicating disabled state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpinnerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<LuckyItem> data = new ArrayList<>();
        setupLuckyWheelItems(data);
        binding.wheelview.setData(data);
        binding.wheelview.setRound(5);

        updateUIBasedOnSpinStatus();

        binding.spinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canSpinToday()) {
                    Random r = new Random();
                    int randomNumber = r.nextInt(8);
                    binding.wheelview.startLuckyWheelWithTargetIndex(randomNumber);
                    // Spin date will be saved in LuckyRoundItemSelectedListener
                } else {
                    // User already knows from the UI, but a Toast can be extra feedback
                    Toast.makeText(SpinnerActivity.this, "Already spun today!", Toast.LENGTH_SHORT).show();
                    // Ensure message is visible if they click again
                    binding.tvSpinStatusMessage.setVisibility(View.VISIBLE);
                    binding.tvSpinStatusMessage.setText("Patience, young spinner!\nTry again tomorrow.");
                }
            }
        });

        binding.wheelview.setLuckyRoundItemSelectedListener(new LuckyWheelView.LuckyRoundItemSelectedListener() {
            @Override
            public void LuckyRoundItemSelected(int index) {
                updateCash(index); // This contains the Toast for coins won
                saveLastSpinDate();
                // Update UI to reflect that spin for the day is used
                binding.spinBtn.setEnabled(false);
                binding.spinBtn.setAlpha(DIMMED_ALPHA);
                binding.wheelview.setAlpha(DIMMED_ALPHA); // Dim the wheel too
                binding.tvSpinStatusMessage.setText("Great spin! Come back tomorrow.");
                binding.tvSpinStatusMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateUIBasedOnSpinStatus() {
        if (!canSpinToday()) {
            binding.spinBtn.setEnabled(false);
            binding.spinBtn.setAlpha(DIMMED_ALPHA);
            binding.wheelview.setAlpha(DIMMED_ALPHA); // Dim the wheel
            binding.tvSpinStatusMessage.setText("You've already spined today!\nCome back for more rewards tomorrow.");
            binding.tvSpinStatusMessage.setVisibility(View.VISIBLE);
        } else {
            binding.spinBtn.setEnabled(true);
            binding.spinBtn.setAlpha(1.0f);
            binding.wheelview.setAlpha(1.0f); // Ensure wheel is fully opaque
            binding.tvSpinStatusMessage.setVisibility(View.GONE);
        }
    }

    private void setupLuckyWheelItems(List<LuckyItem> data) {
        // ... (Your existing LuckyItem setup code remains the same)
        LuckyItem luckyItem1 = new LuckyItem();
        luckyItem1.topText = "5";
        luckyItem1.secondaryText = "COINS";
        luckyItem1.textColor = Color.parseColor("#212121");
        luckyItem1.color = Color.parseColor("#eceff1");
        data.add(luckyItem1);

        LuckyItem luckyItem2 = new LuckyItem();
        luckyItem2.topText = "10";
        luckyItem2.secondaryText = "COINS";
        luckyItem2.color = Color.parseColor("#00cf00");
        luckyItem2.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem2);

        LuckyItem luckyItem3 = new LuckyItem();
        luckyItem3.topText = "15";
        luckyItem3.secondaryText = "COINS";
        luckyItem3.textColor = Color.parseColor("#212121");
        luckyItem3.color = Color.parseColor("#eceff1");
        data.add(luckyItem3);

        LuckyItem luckyItem4 = new LuckyItem();
        luckyItem4.topText = "20";
        luckyItem4.secondaryText = "COINS";
        luckyItem4.color = Color.parseColor("#7f00d9");
        luckyItem4.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem4);

        LuckyItem luckyItem5 = new LuckyItem();
        luckyItem5.topText = "25";
        luckyItem5.secondaryText = "COINS";
        luckyItem5.textColor = Color.parseColor("#212121");
        luckyItem5.color = Color.parseColor("#eceff1");
        data.add(luckyItem5);

        LuckyItem luckyItem6 = new LuckyItem();
        luckyItem6.topText = "30";
        luckyItem6.secondaryText = "COINS";
        luckyItem6.color = Color.parseColor("#dc0000");
        luckyItem6.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem6);

        LuckyItem luckyItem7 = new LuckyItem();
        luckyItem7.topText = "35";
        luckyItem7.secondaryText = "COINS";
        luckyItem7.textColor = Color.parseColor("#212121");
        luckyItem7.color = Color.parseColor("#eceff1");
        data.add(luckyItem7);

        LuckyItem luckyItem8 = new LuckyItem();
        luckyItem8.topText = "0";
        luckyItem8.secondaryText = "COINS";
        luckyItem8.color = Color.parseColor("#008bff");
        luckyItem8.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem8);
    }


    void updateCash(int index) {
        long cash = 0;
        switch (index) {
            case 0: cash = 5; break;
            case 1: cash = 10; break;
            case 2: cash = 15; break;
            case 3: cash = 20; break;
            case 4: cash = 25; break;
            case 5: cash = 30; break;
            case 6: cash = 35; break;
            case 7: cash = 0; break;
        }

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        long finalCash = cash;
        database
                .collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .update("coins", FieldValue.increment(cash)).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(SpinnerActivity.this, finalCash + " coins added to your account!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    private void saveLastSpinDate() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_SPIN_DATE_KEY, getCurrentDate());
        editor.apply();
    }

    private String getLastSpinDate() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_SPIN_DATE_KEY, null);
    }

    private boolean canSpinToday() {
        String lastSpinDate = getLastSpinDate();
        if (lastSpinDate == null) {
            return true; // Never spun before
        }
        String currentDate = getCurrentDate();
        return !lastSpinDate.equals(currentDate);
    }
}
