package com.example.ecoinitiatives.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoinitiatives.R;
import com.example.ecoinitiatives.model.Initiative;
import com.google.firebase.database.*;
import java.util.List;

public class AdminInitiativeAdapter extends RecyclerView.Adapter<AdminInitiativeAdapter.ViewHolder> {

    private Context context;
    private List<Initiative> initiatives;
    private OnInitiativeActionListener actionListener;
    private DatabaseReference mDatabase;

    public interface OnInitiativeActionListener {
        void onApprove(Initiative initiative);
        void onReject(Initiative initiative);
        void onDelete(Initiative initiative);
    }

    // Конструктор с тремя лямбдами
    public AdminInitiativeAdapter(Context context, List<Initiative> initiatives,
                                  OnInitiativeActionListener listener) {
        this.context = context;
        this.initiatives = initiatives;
        this.actionListener = listener;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // Альтернативный конструктор с отдельными параметрами
    public AdminInitiativeAdapter(Context context, List<Initiative> initiatives,
                                  Action approveAction, Action rejectAction, Action deleteAction) {
        this(context, initiatives, new OnInitiativeActionListener() {
            @Override
            public void onApprove(Initiative initiative) {
                approveAction.execute(initiative);
            }

            @Override
            public void onReject(Initiative initiative) {
                rejectAction.execute(initiative);
            }

            @Override
            public void onDelete(Initiative initiative) {
                deleteAction.execute(initiative);
            }
        });
    }

    public interface Action {
        void execute(Initiative initiative);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_initiative, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Initiative initiative = initiatives.get(position);

        holder.tvTitle.setText(initiative.getTitle());
        holder.tvDescription.setText(initiative.getDescription());

        // Установка статуса
        String statusText = "";
        int statusColor = 0;
        switch (initiative.getStatus()) {
            case "moderation":
                statusText = "⏳ На модерации";
                statusColor = 0xFFF39C12;
                break;
            case "approved":
                statusText = "✅ Одобрена";
                statusColor = 0xFF27AE60;
                break;
            case "rejected":
                statusText = "❌ Отклонена";
                statusColor = 0xFFE74C3C;
                break;
        }
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setTextColor(statusColor);

        // Загрузка информации об авторе
        loadAuthorInfo(initiative.getUserId(), holder.tvAuthor);

        // Настройка видимости кнопок в зависимости от статуса
        if ("moderation".equals(initiative.getStatus())) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onApprove(initiative);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReject(initiative);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(initiative);
            }
        });
    }

    private void loadAuthorInfo(String userId, TextView tvAuthor) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String authorName = snapshot.child("name").getValue(String.class);
                if (authorName != null && !authorName.isEmpty()) {
                    tvAuthor.setText("Автор: " + authorName);
                } else {
                    tvAuthor.setText("Автор: Неизвестен");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvAuthor.setText("Автор: Ошибка загрузки");
            }
        });
    }

    @Override
    public int getItemCount() {
        return initiatives.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvStatus, tvAuthor;
        Button btnApprove, btnReject, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvInitiativeTitle);
            tvDescription = itemView.findViewById(R.id.tvInitiativeDescription);
            tvStatus = itemView.findViewById(R.id.tvInitiativeStatus);
            tvAuthor = itemView.findViewById(R.id.tvInitiativeAuthor);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnDelete = itemView.findViewById(R.id.btnDeleteInitiative);
        }
    }
}