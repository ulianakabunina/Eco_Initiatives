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
import com.example.ecoinitiatives.model.User;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;
    private OnUserDeleteListener deleteListener;

    public interface OnUserDeleteListener {
        void onDelete(User user);
    }

    public UserAdapter(Context context, List<User> userList, OnUserDeleteListener deleteListener) {
        this.context = context;
        this.userList = userList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUserName.setText(user.getName());
        holder.tvUserLogin.setText("Логин: " + user.getLogin());
        holder.tvUserEmail.setText("Email: " + user.getEmail());

        holder.btnDeleteUser.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserLogin, tvUserEmail;
        Button btnDeleteUser;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserLogin = itemView.findViewById(R.id.tvUserLogin);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}