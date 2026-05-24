package com.example.guysapp;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }
    private void initViews() {
        editTextEmail = findViewById(R.id.edittext_email);
        editTextPassword = findViewById(R.id.edittext_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
    }
    private void setupListeners() {
    buttonLogin.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
                handleLoginClick();
            }
     });
     buttonRegister.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
       startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
     finish();
            }
        });
    }
    private void handleLoginClick() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        setButtonsEnabled(false);
        loginUser(email, password);
    }
    private void loginUser(String email, String password) {
     FBRef.mAuth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {
    if (task.isSuccessful()) {
    Toast.makeText(LoginActivity.this, "Logged in successfully", Toast.LENGTH_SHORT).show();
    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
     finish();
     }
    else {
    editTextPassword.setText("");
    editTextPassword.requestFocus();
    setButtonsEnabled(true);
    String errorMsg = "Login failed";
        if (task.getException() != null) {
            errorMsg = task.getException().getMessage();
        }
    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
     }
     }
     });
    }
    private void setButtonsEnabled(boolean enabled) {
        buttonLogin.setEnabled(enabled);
        buttonRegister.setEnabled(enabled);
    }
}