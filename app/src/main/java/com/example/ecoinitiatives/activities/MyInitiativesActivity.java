// MyInitiativesActivity.java
package com.example.ecoinitiatives.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoinitiatives.R;
import com.example.ecoinitiatives.adapters.InitiativeAdapter;
import com.example.ecoinitiatives.model.Initiative;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MyInitiativesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private InitiativeAdapter adapter;
    private List<Initiative> initiativeList;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_initiatives);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recyclerView = findViewById(R.id.recyclerViewMyInitiatives);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        initiativeList = new ArrayList<>();
        adapter = new InitiativeAdapter(this, initiativeList, true);
        recyclerView.setAdapter(adapter);

        loadMyInitiatives();
    }

    private void loadMyInitiatives() {
        mDatabase.child("initiatives")
                .orderByChild("userId")
                .equalTo(currentUser.getUid())
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
                        // Обработка ошибки
                    }
                });
    }
}