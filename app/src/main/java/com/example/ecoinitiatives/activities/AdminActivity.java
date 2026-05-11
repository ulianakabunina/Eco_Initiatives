package com.example.ecoinitiatives.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecoinitiatives.MainActivity;
import com.example.ecoinitiatives.R;
import com.example.ecoinitiatives.adapters.AdminInitiativeAdapter;
import com.example.ecoinitiatives.adapters.UserAdapter;
import com.example.ecoinitiatives.model.Initiative;
import com.example.ecoinitiatives.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private List<User> userList;
    private List<Initiative> initiativeList;
    private UserAdapter userAdapter;
    private AdminInitiativeAdapter initiativeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Проверяем авторизацию
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Проверяем, что пользователь - администратор
        verifyAdminAndProceed();
    }

    private void verifyAdminAndProceed() {
        String userId = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);

                    if (role != null && role.equals("admin")) {
                        // Пользователь админ, загружаем админ-панель
                        initializeAdminPanel();
                    } else {
                        // Не админ, отправляем на главный экран
                        Toast.makeText(AdminActivity.this,
                                "У вас нет прав администратора", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AdminActivity.this, MainActivity.class));
                        finish();
                    }
                } else {
                    Toast.makeText(AdminActivity.this,
                            "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    startActivity(new Intent(AdminActivity.this, LoginActivity.class));
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this,
                        "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeAdminPanel() {
        initViews();
        setupToolbar();
        setupTabs();
        setupFAB();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        initiativeList = new ArrayList<>();

        userAdapter = new UserAdapter(this, userList, this::deleteUser);
        initiativeAdapter = new AdminInitiativeAdapter(this, initiativeList,
                this::approveInitiative,
                this::rejectInitiative,
                this::deleteInitiative);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Панель администратора");
            if (mAuth.getCurrentUser() != null) {
                getSupportActionBar().setSubtitle(mAuth.getCurrentUser().getEmail());
            }
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Пользователи"));
        tabLayout.addTab(tabLayout.newTab().setText("Инициативы"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadUsers();
                    recyclerView.setAdapter(userAdapter);
                    fab.hide();
                } else {
                    loadInitiatives();
                    recyclerView.setAdapter(initiativeAdapter);
                    fab.show();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadUsers();
        recyclerView.setAdapter(userAdapter);
        fab.hide();
    }

    private void setupFAB() {
        fab.setOnClickListener(v -> showCreateInitiativeDialog());
    }

    private void showCreateInitiativeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создать инициативу");

        View view = getLayoutInflater().inflate(R.layout.dialog_create_initiative, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDescription = view.findViewById(R.id.etDescription);

        builder.setView(view)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (title.isEmpty() || description.isEmpty()) {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createInitiative(title, description);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void createInitiative(String title, String description) {
        String id = mDatabase.child("initiatives").push().getKey();
        Initiative initiative = new Initiative(id, title, description, mAuth.getCurrentUser().getUid());
        initiative.setStatus("approved");

        mDatabase.child("initiatives").child(id).setValue(initiative)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Инициатива создана", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUsers() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String role = dataSnapshot.child("role").getValue(String.class);
                    // Показываем всех пользователей, кроме администраторов
                    if (role == null || !role.equals("admin")) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            user.setId(dataSnapshot.getKey());
                            userList.add(user);
                        }
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this,
                        "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadInitiatives() {
        mDatabase.child("initiatives").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                initiativeList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Initiative initiative = dataSnapshot.getValue(Initiative.class);
                    if (initiative != null) {
                        initiative.setId(dataSnapshot.getKey());
                        initiativeList.add(initiative);
                    }
                }
                initiativeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this,
                        "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить пользователя")
                .setMessage("Удалить " + user.getName() + "?")
                .setPositiveButton("Да", (dialog, which) -> {
                    mDatabase.child("users").child(user.getId()).removeValue();
                    // Также удаляем инициативы пользователя
                    mDatabase.child("initiatives")
                            .orderByChild("userId")
                            .equalTo(user.getId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    for (DataSnapshot child : snapshot.getChildren()) {
                                        child.getRef().removeValue();
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError error) {}
                            });
                    Toast.makeText(this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void approveInitiative(Initiative initiative) {
        mDatabase.child("initiatives").child(initiative.getId())
                .child("status").setValue("approved")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Одобрено", Toast.LENGTH_SHORT).show());
    }

    private void rejectInitiative(Initiative initiative) {
        mDatabase.child("initiatives").child(initiative.getId())
                .child("status").setValue("rejected")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Отклонено", Toast.LENGTH_SHORT).show());
    }

    private void deleteInitiative(Initiative initiative) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить инициативу")
                .setMessage("Удалить \"" + initiative.getTitle() + "\"?")
                .setPositiveButton("Да", (dialog, which) -> {
                    mDatabase.child("initiatives").child(initiative.getId()).removeValue();
                    Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            Toast.makeText(this, "Вы вышли", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}