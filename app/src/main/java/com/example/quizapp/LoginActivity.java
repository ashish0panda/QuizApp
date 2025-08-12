package com.example.quizapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.quizapp.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    FirebaseAuth auth;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Logging in...");

        binding.submitBtn.setOnClickListener(v -> {
            String email, pass;
            email = binding.emailBox.getText().toString();
            pass = binding.passwordBox.getText().toString();


            dialog.show();

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                dialog.dismiss();
                if(task.isSuccessful()) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.createNewBtn.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));


    }
}