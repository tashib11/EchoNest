package echonest.sociogram.connectus.Adapters;

import android.app.MediaRouteButton;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import echonest.sociogram.connectus.FullScreenImageActivity;
import echonest.sociogram.connectus.Models.ModelChat;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdapterChat extends  RecyclerView.Adapter<AdapterChat.MyHolder> {
    private  static  final  int MSG_TYPE_LEFT=0;
    private  static  final  int MSG_TYPE_RIGHT=1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;
    FirebaseUser fUser;
    private VideoView currentlyPlayingVideo;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==MSG_TYPE_LEFT){
            View view= LayoutInflater.from(context).inflate(R.layout.sample_receiver,parent,false);
            return new MyHolder(view);
        }else{
            View view= LayoutInflater.from(context).inflate(R.layout.sample_sender,parent,false);
            return new MyHolder(view);
        }
        
    }


    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();

        try {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(Long.parseLong(timeStamp));
            SimpleDateFormat sdf = new SimpleDateFormat("dd//MM//yyyy hh:mm aa", Locale.ENGLISH);
            holder.timeTv.setText(sdf.format(cal.getTime()));
        } catch (NumberFormatException e) {
            Log.e("TimeStamp Error", "Invalid timestamp format", e);
        }

        // Reset visibility for recycled views
        holder.messageTv.setVisibility(View.GONE);
        holder.messageIv.setVisibility(View.GONE);
        holder.messageVideoView.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.GONE); // Reset ProgressBar visibility

        String chatType = chatList.get(position).getType();

        if ("text".equals(chatType)) {
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(message);
        } else if ("image".equals(chatType)) {
            holder.messageIv.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE); // Show ProgressBar during loading

            Glide.with(context)
                    .load(message)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE); // Hide ProgressBar on failure
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE); // Hide ProgressBar on success
                            return false;
                        }
                    })
                    .into(holder.messageIv);

            // Add click listener for full-screen view
            holder.messageIv.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("image_url", message); // Pass image URL to the activity
                context.startActivity(intent);
            });

        } else if ("video".equals(chatType)) {
            holder.messageVideoView.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE); // Show ProgressBar during preparation

            Uri videoUri = Uri.parse(message);
            holder.messageVideoView.setVideoURI(videoUri);
            holder.messageVideoView.setOnPreparedListener(mp -> {
                holder.progressBar.setVisibility(View.GONE); // Hide ProgressBar when ready
                holder.messageVideoView.start();
            });
            holder.messageVideoView.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    holder.progressBar.setVisibility(View.GONE);
                }
                return false;
            });
            holder.messageVideoView.setOnCompletionListener(mp -> holder.messageVideoView.seekTo(0));
        }

        try {
            Picasso.get().load(imageUrl).placeholder(R.drawable.avatar).into(holder.profileIv);
        } catch (Exception e) {
            Picasso.get().load(R.drawable.avatar).into(holder.profileIv);
        }



        holder.messageTv.setOnLongClickListener(new View.OnLongClickListener() {
    @Override
    public boolean onLongClick(View view) {
        AlertDialog.Builder builder= new AlertDialog.Builder(context);
        builder.setTitle("Delte message");
        builder.setMessage("Remove for you");
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteMessage(position);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
        return true;
    }
});
        // Set seen/delivered status
        if(position==chatList.size()-1) {
            if (chatList.get(position).isSeen()) {
                holder.isSeenTv.setText("Seen");
            } else {
                holder.isSeenTv.setText("Delivered");
            }
        }else{
            holder.isSeenTv.setVisibility(View.GONE);
        }
    }

    private void deleteMessage(int position) {
        String myUID= Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();//sender id
// get time of clicked msg
        //comapre the time of the clicked mesg with allmsg in chats

        String msgTimeStamp= chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    //delete only own message
                    if(ds.child("sender").getValue().equals(myUID)) {

                        // 1 remove the msg from Chats
                        // set the value of that message"this msg was deleted
//ds.getRef().removeValue();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "This message was removed");
                        ds.getRef().updateChildren(hashMap);

                    }else{
                        Toast.makeText(context, "you can delete only your message", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }
    
    public int getItemViewType(int position){
        fUser= FirebaseAuth.getInstance().getCurrentUser();
        if(chatList.get(position).getSender().equals(fUser.getUid())){
            return  MSG_TYPE_RIGHT;
        }else{
            return MSG_TYPE_LEFT;
        }
    }
    @Override
    public void onViewAttachedToWindow(@NonNull MyHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.messageVideoView.isPlaying()) {
            holder.messageVideoView.start();
        }
        if (currentlyPlayingVideo != null && currentlyPlayingVideo != holder.messageVideoView) {
            currentlyPlayingVideo.stopPlayback();
        }
        currentlyPlayingVideo = holder.messageVideoView;
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MyHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.messageVideoView.isPlaying()) {
            holder.messageVideoView.pause();
        }
        if (currentlyPlayingVideo == holder.messageVideoView) {
            currentlyPlayingVideo = null;
        }
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        VideoView messageVideoView;
        ImageView profileIv, messageIv;
        TextView messageTv, timeTv, isSeenTv;
        LinearLayout messageLayout; // For click listener to show delete
        ProgressBar progressBar;    // Add ProgressBar reference

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            messageIv = itemView.findViewById(R.id.messageIvImage);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageVideoView = itemView.findViewById(R.id.messageVideoView);
            progressBar = itemView.findViewById(R.id.progressBar); // Initialize ProgressBar
        }
    }


}
