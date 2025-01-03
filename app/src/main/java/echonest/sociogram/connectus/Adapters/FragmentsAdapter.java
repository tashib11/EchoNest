package echonest.sociogram.connectus.Adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import echonest.sociogram.connectus.Fragments.ChatsFragment;
import echonest.sociogram.connectus.Fragments.PeopleFragment;

public class FragmentsAdapter extends FragmentPagerAdapter {
    public FragmentsAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }



    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0 : return new ChatsFragment();

            case 1 : return new PeopleFragment();

            default: return  new ChatsFragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        String title =null;
        if(position==0){
            title="Chats";

        }

        if(position==1) {
            title = "People";
        }
        return super.getPageTitle(position);
    }
}
