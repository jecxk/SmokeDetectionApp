package com.example.smokedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;
    private Button btnLogin, btnRegister, btnGuest;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // if used to login, get straight to main
        if(auth.getCurrentUser() != null){
            goToMain();
        }

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
        btnGuest.setOnClickListener(v -> loginGuest());
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 6) return false;
        if (!password.matches(".*[A-Z].*")) return false; // Uppercase
        if (!password.matches(".*[!@#$%^&*].*")) return false; // Symbol
        return true;
    }

    // Disable button -> anti spam
    private void setButtonsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnGuest.setEnabled(enabled);
    }

    private void loginUser(){
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Wrong format of email", Toast.LENGTH_SHORT).show();
            return;
        }

        setButtonsEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setButtonsEnabled(true);

                        if (task.isSuccessful()){
                            Toast.makeText(LoginActivity.this, "Login successfully", Toast.LENGTH_SHORT).show();
                            goToMain();
                        }
                        else {
                            // Handle errors
                            try {
                                throw task.getException();
                            } catch(FirebaseAuthException e) {
                                String errorCode = e.getErrorCode();
                                switch (errorCode) {
                                    case "ERROR_INVALID_EMAIL":
                                        Toast.makeText(LoginActivity.this, "Invalid email format", Toast.LENGTH_LONG).show();
                                        break;
                                    case "ERROR_WRONG_PASSWORD":
                                        Toast.makeText(LoginActivity.this, "Wrong password", Toast.LENGTH_LONG).show();
                                        inputPassword.requestFocus();
                                        break;
                                    case "ERROR_INVALID_CREDENTIAL":
                                        Toast.makeText(LoginActivity.this, "Wrong email or password", Toast.LENGTH_LONG).show();
                                        break;
                                    case "ERROR_USER_NOT_FOUND":
                                        Toast.makeText(LoginActivity.this, "Email not registered", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                // Other errors
                                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void registerUser(){
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Wrong email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must have at least 6 characters, 1 uppercase and 1 symbol", Toast.LENGTH_LONG).show();
            return;
        }

        setButtonsEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setButtonsEnabled(true);

                        if(task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Registration successfully", Toast.LENGTH_SHORT).show();
                            goToMain();
                        }
                        else {
                            try {
                                throw task.getException();
                            } catch(FirebaseAuthException e) {
                                String errorCode = e.getErrorCode();
                                switch (errorCode) {
                                    case "ERROR_EMAIL_ALREADY_IN_USE":
                                        Toast.makeText(LoginActivity.this, "This email is existed", Toast.LENGTH_LONG).show();
                                        break;
                                    case "ERROR_INVALID_EMAIL":
                                        Toast.makeText(LoginActivity.this, "Invalid email format", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(LoginActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void loginGuest() {
        setButtonsEnabled(false);

        auth.signInAnonymously()
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setButtonsEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login as guest", Toast.LENGTH_SHORT).show();
                            goToMain();
                        }
                        else{
                            Toast.makeText(LoginActivity.this,
                                    "Guest login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void goToMain(){
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}