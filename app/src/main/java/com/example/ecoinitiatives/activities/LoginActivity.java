package com.example.ecoinitiatives.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecoinitiatives.MainActivity;
import com.example.ecoinitiatives.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Проверяем, не залогинен ли уже пользователь
        if (mAuth.getCurrentUser() != null) {
            checkUserRole(mAuth.getCurrentUser().getUid());
        }

        initViews();

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
        });
    }

    private void initViews() {
        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loginUser() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(login)) {
            etLogin.setError("Введите логин");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Введите пароль");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Сначала ищем пользователя по логину в базе
        mDatabase.child("users").orderByChild("login").equalTo(login)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                String email = userSnapshot.child("email").getValue(String.class);
                                final String role = userSnapshot.child("role").getValue(String.class);
                                final String userId = userSnapshot.getKey();

                                // Аутентификация через Firebase Auth
                                mAuth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(task -> {
                                            progressBar.setVisibility(View.GONE);

                                            if (task.isSuccessful()) {
                                                // Сохраняем роль в SharedPreferences для быстрого доступа
                                                getSharedPreferences("app_prefs", MODE_PRIVATE)
                                                        .edit()
                                                        .putString("user_role", role != null ? role : "user")
                                                        .apply();

                                                Toast.makeText(LoginActivity.this,
                                                        "Добро пожаловать!", Toast.LENGTH_SHORT).show();

                                                // Перенаправляем в зависимости от роли
                                                if (role != null && role.equals("admin")) {
                                                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                                } else {
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                }
                                                finish();
                                            } else {
                                                Toast.makeText(LoginActivity.this,
                                                        "Ошибка входа: " + task.getException().getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                                break;
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,
                                    "Пользователь с логином \"" + login + "\" не найден",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this,
                                "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);

                    if (role != null && role.equals("admin")) {
                        startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                    finish();
                } else {
                    // Если нет данных, выходим
                    mAuth.signOut();
                    Toast.makeText(LoginActivity.this,
                            "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LoginActivity.this,
                        "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}