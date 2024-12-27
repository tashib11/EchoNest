package echonest.sociogram.connectus;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class CustomItemAnimator extends DefaultItemAnimator {
    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(holder.itemView.getHeight()); // Start from below the view
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0) // Slide into place
                .setDuration(3000)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> dispatchAddFinished(holder)) // Notify animation finished
                .start();
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        holder.itemView.animate()
                .alpha(0f)
                .translationY(holder.itemView.getHeight()) // Slide down
                .setDuration(300)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    dispatchRemoveFinished(holder); // Notify animation finished
                    holder.itemView.setTranslationY(0); // Reset for reuse
                })
                .start();
        return true;
    }

    @Override
    public void runPendingAnimations() {
        super.runPendingAnimations();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        return true;
    }
}