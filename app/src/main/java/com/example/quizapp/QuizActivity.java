package com.example.quizapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quizapp.databinding.ActivityQuizBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Random;

public class QuizActivity extends AppCompatActivity {

    ActivityQuizBinding binding;

    ArrayList<Question> questions;
    int index = 0;
    Question question; // Represents the current question object
    CountDownTimer timer;
    FirebaseFirestore database;
    int correctAnswers = 0;
    private static final String TAG = "QuizActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        questions = new ArrayList<>();
        database = FirebaseFirestore.getInstance();

        showLoadingUI(true); // Show loading UI at the start

        final String catId = getIntent().getStringExtra("catId");
        if (catId == null || catId.isEmpty()) {
            Log.e(TAG, "Category ID is null or empty!");
            Toast.makeText(this, "Error: Category not found.", Toast.LENGTH_LONG).show();
            showLoadingUI(false);
            handleNoQuestionsAvailable();
            return;
        }

        Random random = new Random();
        final int rand = random.nextInt(12); // Assuming your indices are in a reasonable range

        fetchQuestions(catId, rand);
        resetTimer(); // Initialize timer, but don't start it until a question is set
    }

    private void showLoadingUI(boolean isLoading) {
        if (isLoading) {
            binding.loadingOverlay.setVisibility(View.VISIBLE);
            binding.quizContentGroup.setVisibility(View.GONE);
        } else {
            binding.loadingOverlay.setVisibility(View.GONE);
            binding.quizContentGroup.setVisibility(View.VISIBLE);
        }
    }

    private void fetchQuestions(final String catId, final int rand) {
        database.collection("categories")
                .document(catId)
                .collection("questions")
                .whereGreaterThanOrEqualTo("index", rand)
                .orderBy("index")
                .limit(5).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                            Question q = snapshot.toObject(Question.class);
                            if (q != null) questions.add(q);
                        }
                        if (questions.size() < 5) { // If first query didn't fetch enough
                            database.collection("categories")
                                    .document(catId)
                                    .collection("questions")
                                    .whereLessThan("index", rand) // Use "lessThan" to avoid overlap if rand was 0
                                    .orderBy("index")
                                    .limit(5 - questions.size()) // Fetch remaining needed
                                    .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots2) {
                                            for (DocumentSnapshot snapshot : queryDocumentSnapshots2) {
                                                Question q = snapshot.toObject(Question.class);
                                                if (q != null) questions.add(q);
                                            }
                                            finalizeQuestionLoading();
                                        }
                                    });
                        } else {
                            finalizeQuestionLoading();
                        }
                    }
                });
    }

    private void finalizeQuestionLoading() {
        showLoadingUI(false);
        if (questions.isEmpty()) {
            handleNoQuestionsAvailable();
        } else {
            setNextQuestion();
        }
    }

    private void handleNoQuestionsAvailable() {
        binding.question.setText("No questions available for this category.");
        binding.option1.setVisibility(View.GONE);
        binding.option2.setVisibility(View.GONE);
        binding.option3.setVisibility(View.GONE);
        binding.option4.setVisibility(View.GONE);
        binding.imageView4.setVisibility(View.GONE); // 50/50 lifeline
        binding.imageView5.setVisibility(View.GONE); // Audience poll
        binding.nextBtn.setText("Go Back");
        binding.nextBtn.setOnClickListener(v -> finish()); // Or navigate to MainActivity
        binding.quizBtn.setVisibility(View.GONE);
        binding.timer.setVisibility(View.GONE);
        binding.questionCounter.setVisibility(View.GONE);
    }


    void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.timer.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                // Time's up, automatically go to the next question or show result
                if (index < questions.size() -1) {
                    index++;
                    reset();
                    setNextQuestion();
                } else {
                    // If it was the last question, or if no questions, handle end of quiz
                    Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
                    intent.putExtra("correct", correctAnswers);
                    intent.putExtra("total", questions.size());
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    void showAnswer() {
        if (question == null) return; // Should not happen if questions list is populated

        if (question.getAnswer().equals(binding.option1.getText().toString()))
            binding.option1.setBackground(getResources().getDrawable(R.drawable.option_right));
        else if (question.getAnswer().equals(binding.option2.getText().toString()))
            binding.option2.setBackground(getResources().getDrawable(R.drawable.option_right));
        else if (question.getAnswer().equals(binding.option3.getText().toString()))
            binding.option3.setBackground(getResources().getDrawable(R.drawable.option_right));
        else if (question.getAnswer().equals(binding.option4.getText().toString()))
            binding.option4.setBackground(getResources().getDrawable(R.drawable.option_right));
    }

    void setNextQuestion() {
        if (questions.isEmpty() || index >= questions.size()) {
            // This case should be handled by finalizeQuestionLoading or end of quiz logic
            if (questions.isEmpty()) handleNoQuestionsAvailable();
            else { // Quiz finished normally
                Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
                intent.putExtra("correct", correctAnswers);
                intent.putExtra("total", questions.size());
                startActivity(intent);
                finish();
            }
            return;
        }

        resetTimer(); // Cancel previous and prepare new timer
        timer.start(); // Start timer for the new question

        binding.questionCounter.setText(String.format("%d/%d", (index + 1), questions.size()));
        question = questions.get(index); // Get the current question
        binding.question.setText(question.getQuestion());
        binding.option1.setText(question.getOption1());
        binding.option2.setText(question.getOption2());
        binding.option3.setText(question.getOption3());
        binding.option4.setText(question.getOption4());

        // Ensure options are visible if they were hidden by handleNoQuestionsAvailable
        binding.option1.setVisibility(View.VISIBLE);
        binding.option2.setVisibility(View.VISIBLE);
        binding.option3.setVisibility(View.VISIBLE);
        binding.option4.setVisibility(View.VISIBLE);
        // binding.imageView4.setVisibility(View.VISIBLE); // Lifelines might need separate logic if used
        // binding.imageView5.setVisibility(View.VISIBLE);
    }

    void checkAnswer(TextView textView) {
        if (question == null) return;

        timer.cancel(); // Stop timer once an answer is selected
        String selectedAnswer = textView.getText().toString();
        if (selectedAnswer.equals(question.getAnswer())) {
            correctAnswers++;
            textView.setBackground(getResources().getDrawable(R.drawable.option_right));
        } else {
            showAnswer(); // Highlight the correct answer
            textView.setBackground(getResources().getDrawable(R.drawable.option_wrong));
        }
        // Disable further clicks on options after an answer is selected
        binding.option1.setClickable(false);
        binding.option2.setClickable(false);
        binding.option3.setClickable(false);
        binding.option4.setClickable(false);
    }

    void reset() {
        binding.option1.setBackground(getResources().getDrawable(R.drawable.option_unselected));
        binding.option2.setBackground(getResources().getDrawable(R.drawable.option_unselected));
        binding.option3.setBackground(getResources().getDrawable(R.drawable.option_unselected));
        binding.option4.setBackground(getResources().getDrawable(R.drawable.option_unselected));

        // Re-enable clicks
        binding.option1.setClickable(true);
        binding.option2.setClickable(true);
        binding.option3.setClickable(true);
        binding.option4.setClickable(true);
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {
        int viewId = view.getId();

        if (view.getId() == R.id.option_1 || view.getId() == R.id.option_2 || view.getId() == R.id.option_3 || view.getId() == R.id.option_4) {
            // Timer cancellation and answer checking are now in checkAnswer
            TextView selected = (TextView) view;
            checkAnswer(selected);
        } else if (view.getId() == R.id.nextBtn) {
            if (questions.isEmpty()){ // If next is clicked when no questions are loaded
                finish(); // or navigate to main
                return;
            }
            reset(); // Reset colors and clickability
            if (index < questions.size() - 1) { // If there are more questions
                index++;
                setNextQuestion();
            } else { // Last question was answered, or no more questions
                Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
                intent.putExtra("correct", correctAnswers);
                intent.putExtra("total", questions.size());
                startActivity(intent);
                finish();
            }
        } else if (view.getId() == R.id.quizBtn) { // Handle Quit Button
            if (timer != null) {
                timer.cancel();
            }
            // Go to result screen even if quitting early
            Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
            intent.putExtra("correct", correctAnswers);
            intent.putExtra("total", questions.size()); // Show score based on attempted/total questions
            startActivity(intent);
            finish();
        }
    }
}
