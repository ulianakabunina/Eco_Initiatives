package com.example.ecoinitiatives.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoinitiatives.R;
import com.example.ecoinitiatives.model.Initiative;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.List;

public class InitiativeAdapter extends RecyclerView.Adapter<InitiativeAdapter.ViewHolder> {
    private Context context;
    private List<Initiative> initiatives;
    private boolean showStatus;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    public InitiativeAdapter(Context context, List<Initiative> initiatives, boolean showStatus) {
        this.context = context;
        this.initiatives = initiatives;
        this.showStatus = showStatus;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_initiative, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Initiative initiative = initiatives.get(position);

        holder.tvTitle.setText(initiative.getTitle());
        holder.tvDescription.setText(initiative.getDescription());
        holder.tvLikes.setText("❤️ " + initiative.getLikesCount());
        holder.tvResponses.setText("💬 " + initiative.getResponsesCount());

        if (showStatus) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            String statusText = "";
            switch (initiative.getStatus()) {
                case "moderation":
                    statusText = "Статус: На модерации";
                    break;
                case "approved":
                    statusText = "Статус: Одобрена";
                    break;
                case "rejected":
                    statusText = "Статус: Отклонена";
                    break;
            }
            holder.tvStatus.setText(statusText);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // Проверка лайка
        checkIfLiked(initiative.getId(), holder.btnLike);

        // Проверка отклика
        checkIfResponded(initiative.getId(), holder.btnRespond);

        holder.btnLike.setOnClickListener(v -> toggleLike(initiative, holder.btnLike));
        holder.btnRespond.setOnClickListener(v -> showResponseDialog(initiative, holder.btnRespond));

        holder.itemView.setOnClickListener(v -> showInitiativeDetails(initiative));
    }

    private void checkIfLiked(String initiativeId, Button btnLike) {
        if (currentUser == null) return;

        mDatabase.child("initiatives").child(initiativeId).child("likes")
                .child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                            btnLike.setText("❤️ Убрать лайк");
                        } else {
                            btnLike.setText("🤍 Лайкнуть");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkIfResponded(String initiativeId, Button btnRespond) {
        if (currentUser == null) return;

        mDatabase.child("initiatives").child(initiativeId).child("responses")
                .child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            btnRespond.setText("✏️ Отменить отклик");
                        } else {
                            btnRespond.setText("📝 Оставить отклик");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void toggleLike(Initiative initiative, Button btnLike) {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference likeRef = mDatabase.child("initiatives")
                .child(initiative.getId()).child("likes").child(userId);

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    // Удалить лайк
                    likeRef.removeValue();
                    mDatabase.child("initiatives").child(initiative.getId())
                            .child("likesCount").setValue(initiative.getLikesCount() - 1);
                    btnLike.setText("🤍 Лайкнуть");
                    Toast.makeText(context, "Лайк удален", Toast.LENGTH_SHORT).show();
                } else {
                    // Добавить лайк
                    likeRef.setValue(true);
                    mDatabase.child("initiatives").child(initiative.getId())
                            .child("likesCount").setValue(initiative.getLikesCount() + 1);
                    btnLike.setText("❤️ Убрать лайк");
                    Toast.makeText(context, "Лайк добавлен", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResponseDialog(Initiative initiative, Button btnRespond) {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference responseRef = mDatabase.child("initiatives")
                .child(initiative.getId()).child("responses").child(userId);

        responseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Отменить отклик
                    responseRef.removeValue();
                    mDatabase.child("initiatives").child(initiative.getId())
                            .child("responsesCount").setValue(initiative.getResponsesCount() - 1);
                    btnRespond.setText("📝 Оставить отклик");
                    Toast.makeText(context, "Отклик отменен", Toast.LENGTH_SHORT).show();
                } else {
                    // Добавить отклик
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Ваш отклик");

                    final android.widget.EditText input = new android.widget.EditText(context);
                    input.setHint("Напишите ваш отклик...");
                    input.setPadding(50, 20, 50, 20);
                    builder.setView(input);

                    builder.setPositiveButton("Отправить", (dialog, which) -> {
                        String response = input.getText().toString().trim();
                        if (!response.isEmpty()) {
                            responseRef.setValue(response);
                            mDatabase.child("initiatives").child(initiative.getId())
                                    .child("responsesCount").setValue(initiative.getResponsesCount() + 1);
                            btnRespond.setText("✏️ Отменить отклик");
                            Toast.makeText(context, "Отклик отправлен", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Введите текст отклика", Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.setNegativeButton("Отмена", null);
                    builder.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInitiativeDetails(Initiative initiative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Детали инициативы");

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_initiative_details, null);
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvDescription = view.findViewById(R.id.tvDetailDescription);
        TextView tvLikes = view.findViewById(R.id.tvDetailLikes);
        TextView tvResponses = view.findViewById(R.id.tvDetailResponses);

        tvTitle.setText(initiative.getTitle());
        tvDescription.setText(initiative.getDescription());
        tvLikes.setText("❤️ Лайков: " + initiative.getLikesCount());
        tvResponses.setText("💬 Откликов: " + initiative.getResponsesCount());

        builder.setView(view)
                .setPositiveButton("Закрыть", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return initiatives.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvLikes, tvResponses, tvStatus;
        Button btnLike, btnRespond;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvResponses = itemView.findViewById(R.id.tvResponses);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnRespond = itemView.findViewById(R.id.btnRespond);
        }
    }
}