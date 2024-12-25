package echonest.sociogram.connectus;
import android.animation.ObjectAnimator;
import android.view.View;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class CustomItemAnimator extends DefaultItemAnimator {
    private static final long ANIMATION_DURATION = 120; // Faster duration in milliseconds

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        holder.itemView.setTranslationY(holder.itemView.getHeight());
        holder.itemView.setAlpha(0);

        ObjectAnimator.ofFloat(holder.itemView, "translationY", 0).setDuration(ANIMATION_DURATION).start();
        ObjectAnimator.ofFloat(holder.itemView, "alpha", 1).setDuration(ANIMATION_DURATION).start();

        return super.animateAdd(holder);
    }
}

