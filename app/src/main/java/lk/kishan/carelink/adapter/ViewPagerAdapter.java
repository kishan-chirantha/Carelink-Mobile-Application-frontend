package lk.kishan.carelink.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import lk.kishan.carelink.fragment.OrderOngoingFragment;
import lk.kishan.carelink.fragment.OrderRejectedFragment;
import lk.kishan.carelink.fragment.OrdersCompletedFragment;


public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new OrderOngoingFragment();

            case 1:
                return new OrdersCompletedFragment();

            case 2:
                return new OrderRejectedFragment();

            default:
                return new OrderOngoingFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
