package com.example.quizapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.quizapp.databinding.ActivitySignupBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class SignupActivity extends AppCompatActivity {
    ActivitySignupBinding binding;
    FirebaseAuth auth;
    FirebaseFirestore database;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("We're creating new account...");


        binding.createNewBtn.setOnClickListener(v -> {
            String email, pass, name;

            email = binding.emailBox.getText().toString();
            pass = binding.passwordBox.getText().toString();
            name = binding.nameBox.getText().toString();

            final User user = new User(name, email, pass);

            dialog.show();
            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    String uid = Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getUser()).getUid();

                    database
                            .collection("users")
                            .document(uid)
                            .set(user).addOnCompleteListener(task1 -> {
                                if(task1.isSuccessful()) {
                                    dialog.dismiss();
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(SignupActivity.this, Objects.requireNonNull(task1.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    dialog.dismiss();
                    Toast.makeText(SignupActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        });


    }

    public void login(View view) {
        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
    }
}