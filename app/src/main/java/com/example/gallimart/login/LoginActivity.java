package com.example.gallimart.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.buyer.BuyerDashboardActivity;
import com.example.gallimart.driver.DriverDashboardActivity;
import com.example.gallimart.shopkeeper.ShopkeeperDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");

        // If already logged in, skip login screen
        if (sessionManager.isLoggedIn()) {
            redirectToDashboard(sessionManager.getUserRole());
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();


        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            return;
        }

        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        // Fetch user data from /users/{uid}
                        FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .child(uid)
                                .get()
                                .addOnCompleteListener(taskUser -> {
                                    if (taskUser.isSuccessful()) {
                                        DataSnapshot snapshot = taskUser.getResult();
                                        String role = String.valueOf(snapshot.child("role").getValue());
                                        String name = String.valueOf(snapshot.child("name").getValue());
                                        String emailFetched = mAuth.getCurrentUser().getEmail();

                                        // Save session (name, email, role)
                                        sessionManager.createLoginSession(name, emailFetched, role);

                                        redirectToDashboard(role);
                                        finish();
                                    } else {
                                        // Default to Buyer if role fetch fails
                                        sessionManager.createLoginSession("Unknown",
                                                mAuth.getCurrentUser().getEmail(), "Buyer");
                                        redirectToDashboard("Buyer");
                                        finish();
                                    }
                                });

                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void redirectToDashboard(String role) {
        if ("Shopkeeper".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, ShopkeeperDashboardActivity.class));
        } else if ("Driver".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, DriverDashboardActivity.class));
        } else {
            startActivity(new Intent(this, BuyerDashboardActivity.class));
        }
    }
}
