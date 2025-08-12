package com.example.quizapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.quizapp.databinding.ActivityResultBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class ResultActivity extends AppCompatActivity {

    ActivityResultBinding binding;
    int POINTS = 10;
    private static final String TAG = "ResultActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int correctAnswers = getIntent().getIntExtra("correct", 0);
        int totalQuestions = getIntent().getIntExtra("total", 0);

        long points = correctAnswers * POINTS;

        binding.score.setText(String.format("%d/%d", correctAnswers, totalQuestions));
        binding.earnedCoins.setText(String.valueOf(points));

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .update("coins", FieldValue.increment(points));

        binding.restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultActivity.this, MainActivity.class));
                finishAffinity();
            }
        });

        binding.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri screenshotUri = takeScreenshot();
                if (screenshotUri != null) {
                    shareScreenshot(screenshotUri);
                } else {
                    Toast.makeText(ResultActivity.this, "Error taking screenshot", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Uri takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // Create a bitmap of the root view
            View rootView = getWindow().getDecorView().getRootView();
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            // Save the bitmap to a file in the cache directory
            File imagePath = new File(getCacheDir(), "images");
            if (!imagePath.exists()) {
                imagePath.mkdirs();
            }
            File imageFile = new File(imagePath, "screenshot_" + now.getTime() + ".png");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Screenshot saved to: " + imageFile.getAbsolutePath());

            // Get the URI using FileProvider
            // Ensure your AndroidManifest.xml provider authority is ${applicationId}.provider
            return FileProvider.getUriForFile(ResultActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    imageFile);

        } catch (IOException e) {
            Log.e(TAG, "Error taking screenshot: " + e.getMessage(), e);
            return null;
        }
    }

    private void shareScreenshot(Uri imageUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Important for granting permission

        try {
            startActivity(Intent.createChooser(shareIntent, "Share your result via"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No app can handle this request.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "ActivityNotFoundException for sharing: " + ex.getMessage());
        }
    }
}
