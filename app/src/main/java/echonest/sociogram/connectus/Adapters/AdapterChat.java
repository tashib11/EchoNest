package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import echonest.sociogram.connectus.FullScreenImageActivity;
import echonest.sociogram.connectus.FullScreenVideoActivity;
import echonest.sociogram.connectus.Models.ModelChat;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    private final Context context;
    private final String imageUrl;
    private List<ModelChat> chatList;
    private FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == MSG_TYPE_LEFT) ? R.layout.sample_receiver : R.layout.sample_sender;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        ModelChat currentMessage = chatList.get(position);

        // Format timestamp
        holder.timeTv.setText(formatTimestamp(currentMessage.getTimestamp()));

        // Reset visibility for recycled views
        resetViewVisibility(holder);

        // Handle different message types
        switch (currentMessage.getType()) {
            case "text":
                handleTextMessage(holder, currentMessage);
                holder.messageLayout.setOnLongClickListener(v -> {
                    showDeleteDialog(position);
                    return true;
                });
                break;

            case "image":
                handleImageMessage(holder, currentMessage);
                holder.messageIv.setOnLongClickListener(v -> {
                    showDeleteDialog(position);
                    return true;
                });
                break;

            case "video":
                handleVideoMessage(holder, currentMessage);
                holder.messageVideoThumbnail.setOnLongClickListener(v -> {
                    showDeleteDialog(position);
                    return true;
                });
                break;
        }

        // Set profile image
        setProfileImage(holder);

        // Set "seen" status
        if (position == chatList.size() - 1) {
            holder.isSeenTv.setVisibility(View.VISIBLE);
            holder.isSeenTv.setText(currentMessage.isSeen() ? "Seen" : "Delivered");
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        return chatList.get(position).getSender().equals(fUser.getUid()) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
    }

    public void updateChatList(List<ModelChat> newChatList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatDiffCallback(this.chatList, newChatList));
        this.chatList.clear();
        this.chatList.addAll(newChatList);
        diffResult.dispatchUpdatesTo(this);
    }

    private void resetViewVisibility(MyHolder holder) {
        holder.messageTv.setVisibility(View.GONE);
        holder.messageIv.setVisibility(View.GONE);
        holder.messageVideoThumbnail.setVisibility(View.GONE);
        holder.playButtonOverlay.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.GONE);
        holder.videoProgressBar.setVisibility(View.GONE);
    }

    private void handleTextMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageTv.setVisibility(View.VISIBLE);
        holder.messageTv.setText(currentMessage.getMessage());
    }

    private void handleImageMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageIv.setVisibility(View.VISIBLE);

        if (currentMessage.getLocalImageUri() != null) {
            if (currentMessage.isUploading()) {
                // Apply dynamic blur and alpha during upload
                Glide.with(context)
                        .load(currentMessage.getLocalImageUri())
                        .transform(new jp.wasabeef.glide.transformations.BlurTransformation(25 - (currentMessage.getUploadProgress() / 4))) // Dynamic blur
                        .into(holder.messageIv);

                // Show progress bar and update percentage
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(currentMessage.getUploadProgress());
                holder.progressPercentage.setVisibility(View.VISIBLE);
                holder.progressPercentage.setText(currentMessage.getUploadProgress() + "%");

                holder.messageIv.setAlpha(0.3f + (0.7f * currentMessage.getUploadProgress() / 100));
            } else {
                // Upload completed - show clean image
                Glide.with(context)
                        .load(currentMessage.getMessage()) // Load uploaded URL
                        .placeholder(R.drawable.baseline_image_24)
                        .into(holder.messageIv);

                holder.messageIv.setAlpha(1.0f); // Fully opaque
                holder.progressBar.setVisibility(View.GONE);
                holder.progressPercentage.setVisibility(View.GONE); // Hide percentage
            }
        } else if (currentMessage.getMessage() != null) {
            // Display uploaded image
            Glide.with(context)
                    .load(currentMessage.getMessage())
                    .placeholder(R.drawable.baseline_image_24)
                    .into(holder.messageIv);
            holder.messageIv.setAlpha(1.0f); // Fully opaque
            holder.progressBar.setVisibility(View.GONE);
            holder.progressPercentage.setVisibility(View.GONE);
        }

        // Add click listener for full-screen view
        holder.messageIv.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            String imageUrl = currentMessage.getMessage() != null ? currentMessage.getMessage() : currentMessage.getLocalImageUri();
            intent.putExtra("image_url", imageUrl);
            context.startActivity(intent);
        });
    }

    private void handleVideoMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageVideoThumbnail.setVisibility(View.VISIBLE);
        holder.playButtonOverlay.setVisibility(View.GONE); // Hide play button during upload

        if (currentMessage.isUploading()) {
            // Show video thumbnail and update progress bar and percentage
            Glide.with(context)
                    .asBitmap()
                    .load(currentMessage.getLocalImageUri() != null ? Uri.parse(currentMessage.getLocalImageUri()) : R.drawable.baseline_image_24)
                    .into(holder.messageVideoThumbnail);

            holder.videoProgressBar.setVisibility(View.VISIBLE);
            holder.videoProgressPercentage.setVisibility(View.VISIBLE);

            // Update progress
            holder.videoProgressBar.setProgress(currentMessage.getUploadProgress());
            holder.videoProgressPercentage.setText(currentMessage.getUploadProgress() + "%");
        } else {
            // Video upload completed
            Glide.with(context)
                    .asBitmap()
                    .load(currentMessage.getMessage()) // Video thumbnail URL

                    .into(holder.messageVideoThumbnail);

            holder.videoProgressBar.setVisibility(View.GONE);
            holder.videoProgressPercentage.setVisibility(View.GONE);
            holder.playButtonOverlay.setVisibility(View.VISIBLE); // Show play button once upload completes
        }

        // Click listeners for play functionality
        holder.messageVideoThumbnail.setOnClickListener(v -> {
            if (!currentMessage.isUploading()) {
                Intent intent = new Intent(context, FullScreenVideoActivity.class);
                intent.putExtra("videoUrl", currentMessage.getMessage());
                context.startActivity(intent);
            }
        });

        holder.playButtonOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenVideoActivity.class);
            intent.putExtra("videoUrl", currentMessage.getMessage());
            context.startActivity(intent);
        });
    }
    private void setProfileImage(MyHolder holder) {
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.avatar)
                .into(holder.profileIv);
    }

    private String formatTimestamp(String timestamp) {
        try {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(Long.parseLong(timestamp));
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.ENGLISH);
            return sdf.format(cal.getTime());
        } catch (NumberFormatException e) {
            Log.e("Timestamp Error", "Invalid timestamp format", e);
            return "";
        }
    }

    private void showDeleteDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(position))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteMessage(int position) {
        String myUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");

        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (Objects.equals(ds.child("sender").getValue(), myUID)) {
                        String messageType = ds.child("type").getValue(String.class);
                        if ("image".equals(messageType) || "video".equals(messageType)) {
                            // Delete the image or video URL from Firebase Storage
                            String mediaUrl = ds.child("message").getValue(String.class);
                            if (mediaUrl != null) {
                                StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(mediaUrl);
                                storageRef.delete().addOnSuccessListener(unused -> {
                                    // Update message in the database
                                    updateDeletedMessage(ds.getRef());
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to delete media file", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                updateDeletedMessage(ds.getRef());
                            }
                        } else {
                            // For text messages
                            updateDeletedMessage(ds.getRef());
                        }
                    } else {
                        Toast.makeText(context, "You can delete only your message", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Delete Message", "Error deleting message", error.toException());
            }
        });
    }

    private void updateDeletedMessage(DatabaseReference messageRef) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("message", "This message was deleted");
        hashMap.put("type", "text"); // Update type to text
        messageRef.updateChildren(hashMap).addOnSuccessListener(unused ->
                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(context, "Failed to update message", Toast.LENGTH_SHORT).show()
        );
    }


    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv, messageIv, messageVideoThumbnail, playButtonOverlay;
        TextView messageTv, timeTv, isSeenTv,  progressPercentage, videoProgressPercentage;
        LinearLayout messageLayout;
        ProgressBar progressBar, videoProgressBar;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            messageIv = itemView.findViewById(R.id.messageIvImage);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageVideoThumbnail = itemView.findViewById(R.id.messageVideoThumbnail);
            playButtonOverlay = itemView.findViewById(R.id.playButtonOverlay);
            progressBar = itemView.findViewById(R.id.progressBar);
            videoProgressBar = itemView.findViewById(R.id.videoProgressBar);
            progressPercentage = itemView.findViewById(R.id.progressPercentage);
            videoProgressPercentage = itemView.findViewById(R.id.videoProgressPercentage);
        }
    }}