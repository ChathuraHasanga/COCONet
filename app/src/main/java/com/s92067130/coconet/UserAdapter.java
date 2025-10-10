package com.s92067130.coconet;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<User> users;
    private final DatabaseReference userRef;
    private final DatabaseReference logRef;
    private final Context context;

    public UserAdapter(List<User> users, DatabaseReference userRef, Context context){
        this.users = users;
        this.userRef = userRef;
        this.context = context;
        this.logRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("logs").child("admin_actions");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText("Name : " + (user.getName() != null ? user.getName() : ""));
        holder.email.setText("Email : " + (user.getEmail() != null ? user.getEmail() : ""));
        holder.role.setText("Role : "+user.getRole());
        holder.status.setText("Status : "+ user.getStatus());

        boolean isSuspended = "suspended".equalsIgnoreCase(user.getStatus());
        holder.btnSuspend.setText(isSuspended ? "Activate" : "Suspend");

        //suspend/Activate with confirmation + logging
        holder.btnSuspend.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    User u = users.get(pos);
                    if (u.getUid() == null) return;

                    String newStatus = "suspended".equalsIgnoreCase(u.getStatus()) ? "active" : "suspended";

                    new AlertDialog.Builder(context)
                            .setTitle("Confirm Action")
                            .setMessage("Are you sure you want to change status to " + newStatus + "?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                userRef.child(u.getUid()).child("status").setValue(newStatus)
                                        .addOnSuccessListener(aVoid -> {
                                            u.setStatus(newStatus);
                                            notifyItemChanged(pos);

                                            //save log
                                            String logId = logRef.push().getKey();
                                            if (logId != null) {
                                                AuditLog log = new AuditLog(
                                                        getAdminId(),
                                                        u.getUid(),
                                                        "Changed status to " + newStatus,
                                                        System.currentTimeMillis()
                                                );
                                                logRef.child(logId).setValue(log);
                                            }
                                        });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
        });

        //change role with confirmation + logging
        holder.btnRole.setText("Role");
        holder.btnRole.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            User u = users.get(pos);
            if (u.getUid() == null) return;

            String[] roles = {"user", "manager", "moderator", "admin"};

            new AlertDialog.Builder(context)
                    .setTitle("Select Role")
                    .setItems(roles, (dialog, which) -> {
                        String selectedRole = roles[which];
                        new AlertDialog.Builder(context)
                    .setTitle("Confirm Role change")
                    .setMessage("Are you sure you want to change role to " + selectedRole + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        userRef.child(u.getUid()).child("role").setValue(selectedRole)
                                .addOnSuccessListener(aVoid -> {
                                    u.setRole(selectedRole);
                                    notifyItemChanged(pos);

                                    //save log
                                    String logId = logRef.push().getKey();
                                    if (logId != null) {
                                        AuditLog log = new AuditLog(
                                                getAdminId(),
                                                u.getUid(),
                                                "Changed role to " + selectedRole,
                                                System.currentTimeMillis()
                                        );
                                        logRef.child(logId).setValue(log);
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        })
                .show();
        });
    }

    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name, email, role, status;
        Button btnSuspend, btnRole;

        ViewHolder(View itemView){
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            email = itemView.findViewById(R.id.txtEmail);
            role = itemView.findViewById(R.id.txtRole);
            status = itemView.findViewById(R.id.txtStatus);
            btnSuspend = itemView.findViewById(R.id.btnSuspend);
            btnRole = itemView.findViewById(R.id.btnRole);
        }
    }

    private String getAdminId() {
        // Implement logic to get current admin's UID
        ;
        return FirebaseAuth.getInstance(FirebaseApp.getInstance())
                .getCurrentUser().getUid(); // Placeholder
    }
}
