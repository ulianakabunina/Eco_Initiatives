package com.example.ecoinitiatives;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoinitiatives.R;
import com.example.ecoinitiatives.activities.LoginActivity;
import com.example.ecoinitiatives.activities.MyInitiativesActivity;
import com.example.ecoinitiatives.adapters.InitiativeAdapter;
import com.example.ecoinitiatives.model.Initiative;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewInitiatives;
    private InitiativeAdapter adapter;
    private List<Initiative> initiativeList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView tvWelcome;
    private Toolbar toolbar;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupToolbar();
        loadApprovedInitiatives();
        loadUserInfo();

        fab.setOnClickListener(v -> showCreateInitiativeDialog());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewInitiatives = findViewById(R.id.recyclerViewInitiatives);
        tvWelcome = findViewById(R.id.tvWelcome);
        fab = findViewById(R.id.fab);

        recyclerViewInitiatives.setLayoutManager(new LinearLayoutManager(this));
        initiativeList = new ArrayList<>();
        adapter = new InitiativeAdapter(this, initiativeList, false);
        recyclerViewInitiatives.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Эко-Инициативы");
        }
    }

    private void loadUserInfo() {
        mDatabase.child("users").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        tvWelcome.setText("Привет, " + name + "! 👋");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadApprovedInitiatives() {
        mDatabase.child("initiatives")
                .orderByChild("status")
                .equalTo("approved")
                .addValueEventListener(new ValueEventListener() {
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
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Ошибка загрузки инициатив", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCreateInitiativeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🌱 Создать инициативу");

        View view = getLayoutInflater().inflate(R.layout.dialog_create_initiative, null);
        android.widget.EditText etTitle = view.findViewById(R.id.etTitle);
        android.widget.EditText etDescription = view.findViewById(R.id.etDescription);

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
        Initiative initiative = new Initiative(id, title, description, currentUser.getUid());

        mDatabase.child("initiatives").child(id).setValue(initiative)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "✅ Инициатива создана и отправлена на модерацию!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "❌ Ошибка создания инициативы", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_my_initiatives) {
            startActivity(new Intent(this, MyInitiativesActivity.class));
        } else if (item.getItemId() == R.id.action_logout) {
            logout();
        }
        return true;
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }
}