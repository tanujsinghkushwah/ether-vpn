package com.anonymous.ethervpn.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.anonymous.ethervpn.R;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    private static final int PAGE_COUNT = 3;

    static final int[] ILLUSTRATIONS = {
            R.drawable.ic_planet_ringed,
            R.drawable.ic_mesh_nodes,
            R.drawable.ic_compass_sparkle,
    };

    static final int[] EYEBROWS = {
            R.string.onboarding_page1_eyebrow,
            R.string.onboarding_page2_eyebrow,
            R.string.onboarding_page3_eyebrow,
    };

    static final int[] TITLES = {
            R.string.onboarding_page1_title,
            R.string.onboarding_page2_title,
            R.string.onboarding_page3_title,
    };

    static final int[] DESCS = {
            R.string.onboarding_page1_desc,
            R.string.onboarding_page2_desc,
            R.string.onboarding_page3_desc,
    };

    public OnboardingPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment page = new OnboardingPageFragment();
        Bundle args = new Bundle();
        args.putInt("illustration", ILLUSTRATIONS[position]);
        args.putInt("eyebrow", EYEBROWS[position]);
        args.putInt("title", TITLES[position]);
        args.putInt("desc", DESCS[position]);
        page.setArguments(args);
        return page;
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
