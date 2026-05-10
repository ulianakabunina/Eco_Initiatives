package com.example.ecoinitiatives.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoinitiatives.R;
import com.example.ecoinitiatives.adapters.AdminInitiativeAdapter;
import com.example.ecoinitiatives.adapters.UserAdapter;
import com.example.ecoinitiatives.model.Initiative;  // Исправлен импорт
import com.example.ecoinitiatives.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private List<User> userList;
    private List<Initiative> initiativeList;
    private UserAdapter userAdapter;
    private AdminInitiativeAdapter initiativeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupTabs();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        initiativeList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList, this::deleteUser);

        // Исправленный конструктор AdminInitiativeAdapter
        initiativeAdapter = new AdminInitiativeAdapter(
                this,
                initiativeList,
                this::approveInitiative,
                this::rejectInitiative,
                this::deleteInitiative
        );
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
                } else {
                    loadInitiatives();
                    recyclerView.setAdapter(initiativeAdapter);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadUsers();
        recyclerView.setAdapter(userAdapter);
    }

    private void loadUsers() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && !"admin".equals(user.getRole())) {
                        user.setId(dataSnapshot.getKey());
                        userList.add(user);
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Ошибка загрузки пользователей",
                        Toast.LENGTH_SHORT).show();
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
                Toast.makeText(AdminActivity.this, "Ошибка загрузки инициатив",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить пользователя")
                .setMessage("Вы уверены, что хотите удалить пользователя " + user.getName() + "?")
                .setPositiveButton("Да", (dialog, which) -> {
                    // Удаление всех инициатив пользователя
                    mDatabase.child("initiatives")
                            .orderByChild("userId")
                            .equalTo(user.getId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    for (DataSnapshot initiativeSnapshot : snapshot.getChildren()) {
                                        initiativeSnapshot.getRef().removeValue();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {}
                            });

                    // Удаление пользователя
                    mDatabase.child("users").child(user.getId()).removeValue();
                    Toast.makeText(this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void approveInitiative(Initiative initiative) {
        mDatabase.child("initiatives").child(initiative.getId())
                .child("status").setValue("approved")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Инициатива одобрена",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void rejectInitiative(Initiative initiative) {
        mDatabase.child("initiatives").child(initiative.getId())
                .child("status").setValue("rejected")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Инициатива отклонена",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void deleteInitiative(Initiative initiative) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить инициативу")
                .setMessage("Вы уверены, что хотите удалить инициативу \"" + initiative.getTitle() + "\"?")
                .setPositiveButton("Да", (dialog, which) -> {
                    mDatabase.child("initiatives").child(initiative.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Инициатива удалена",
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Нет", null)
                .show();
    }
}