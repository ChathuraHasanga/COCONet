package com.s92067130.coconet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavouriteAdapter extends RecyclerView.Adapter<FavouriteAdapter.FavViewHolder> {

    private List<String> favouriteIds; //storeIds
    private List<Store> favouriteStores;
    private Context context;
    private String currentUid;

    public FavouriteAdapter(List<String> favouriteIds, List<Store> favouriteStores, Context context, String currentUid){
        this.favouriteIds = favouriteIds;
        this.favouriteStores = favouriteStores;
        this.context = context;
        this.currentUid = currentUid;
    }

    @NonNull
    @Override
    public FavouriteAdapter.FavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new FavViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavViewHolder holder, int position) {
        Store store = favouriteStores.get(position);
        String storeId = favouriteIds.get(position);

        holder.tvName.setText("STORE NAME: "+ store.getName());
        holder.tvOwnerName.setText("OWNER NAME: "+ store.getOwnerName());
        holder.tvDistrict.setText("DISTRICT: " +store.getDistrict());
        holder.tvContact.setText("CONTACT NUMBER: " + store.getContactNumber());
        holder.tvNote.setText("NOTE: " + store.getNote());

        //Remove Button
        holder.btnRemove.setOnClickListener(v-> {
            DatabaseReference favRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users")
                    .child(currentUid)
                    .child("favorites")
                    .child(storeId);

            favRef.removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                logUserAction("favorite_removed", storeId, store.getName());
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < favouriteStores.size() && pos < favouriteIds.size()) {
                    favouriteStores.remove(pos);
                    favouriteIds.remove(pos);
                    notifyItemRemoved(pos);
                }
            });
        });
    }

    private void logUserAction(String action, String storeId, String storeName){
        DatabaseReference logsRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("logs").child("users").child(currentUid);

        String logId = logsRef.push().getKey();

        Map<String, Object> logData = new HashMap<>();
        logData.put("action", action);
        logData.put("storeId", storeId);
        logData.put("storeName", storeName);
        logData.put("timestamp", System.currentTimeMillis());

        logsRef.child(logId).setValue(logData);
    }

    public int getItemCount() {
        return favouriteStores.size();
    }

    static class FavViewHolder extends RecyclerView.ViewHolder{
        TextView tvName,tvOwnerName, tvDistrict, tvContact, tvNote;
        Button btnRemove;

        public FavViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFavName);
            tvOwnerName = itemView.findViewById(R.id.tvFavOwnerName);
            tvDistrict = itemView.findViewById(R.id.tvFavDistrict);
            tvContact = itemView.findViewById(R.id.tvFavContact);
            tvNote = itemView.findViewById(R.id.tvFavNote);
            btnRemove = itemView.findViewById(R.id.btnRemoveFav);
        }
    }
}
