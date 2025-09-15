package com.example.gallimart.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gallimart.R;

public class ResetPasswordActivity extends AppCompatActivity {
    EditText etPhone, etNewPassword;
    Button btnReset;
    SharedPreferences prefs;
    private static final String PREFS = "users_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset);

        etPhone = findViewById(R.id.etPhone);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnReset = findViewById(R.id.btnReset);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        btnReset.setOnClickListener(v -> doReset());
    }

    private void doReset() {
        String phone = etPhone.getText().toString().trim();
        String newPass = etNewPassword.getText().toString();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(newPass)) {
            Toast.makeText(this, "Enter phone and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = "user_" + phone;
        if (!prefs.contains(key)) {
            Toast.makeText(this, "No such user", Toast.LENGTH_SHORT).show();
            return;
        }

        // In real app: verify ownership via OTP. Here we directly allow reset for demo.
        prefs.edit().putString(key, newPass).apply();
        Toast.makeText(this, "Password reset. Please login.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
