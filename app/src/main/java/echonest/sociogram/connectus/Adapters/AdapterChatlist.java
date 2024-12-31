package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import echonest.sociogram.connectus.ChatDetailActivity;
import echonest.sociogram.connectus.Models.ModelUser;
import com.example.connectus.R;

import java.util.HashMap;
import java.util.List;

public class AdapterChatlist extends RecyclerView.Adapter<AdapterChatlist.MyHolder> {

    Context context;
    List<ModelUser> userList;
    private HashMap<String, String> lastMessageMap;

    public AdapterChatlist(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        this.lastMessageMap = new HashMap<>();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        ModelUser user = userList.get(position);
        String hisUid = user.getUserId();
        String userImage = user.getProfilePhoto();
        String userName = user.getName();
        String lastMessage = lastMessageMap.get(hisUid);

        holder.nameTv.setText(userName);
        holder.lastMessageTv.setText(lastMessage != null ? lastMessage : "");

        try {
            Glide.with(context)
                    .load(userImage)
                    .placeholder(R.drawable.avatar)
                    .into(holder.profileIv);
        } catch (Exception e) {
            Glide.with(context)
                    .load(R.drawable.avatar)
                    .into(holder.profileIv);
        }

        String onlineStatus = user.getOnlineStatus();
        if (onlineStatus != null && onlineStatus.equals("online")) {
            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);
        } else {
            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
        }

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("hisUid", hisUid);
            context.startActivity(intent);
        });
    }

    public void setLastMessageMap(HashMap<String, String> lastMessageMap) {
        this.lastMessageMap = lastMessageMap;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv, onlineStatusIv;
        TextView nameTv, lastMessageTv;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }
    }
}

