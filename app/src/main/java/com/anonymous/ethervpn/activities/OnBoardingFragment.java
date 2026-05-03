package com.anonymous.ethervpn.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.services.OAuthService;

public class OnBoardingFragment extends AppCompatActivity {

    private ViewPager2 pager;
    private Button ctaBtn;
    private View[] segments;
    private static final int PAGE_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.freeopenvpn_onboardingfragment);

        pager = findViewById(R.id.onboardingPager);
        ctaBtn = findViewById(R.id.onboardingCta);
        segments = new View[]{
                findViewById(R.id.seg0),
                findViewById(R.id.seg1),
                findViewById(R.id.seg2)
        };

        pager.setAdapter(new OnboardingPagerAdapter(this));
        pager.setOffscreenPageLimit(PAGE_COUNT);

        updateSegments(0);
        updateCta(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateSegments(position);
                updateCta(position);
            }
        });

        ctaBtn.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current < PAGE_COUNT - 1) {
                pager.setCurrentItem(current + 1, true);
            } else {
                navigateToAuth();
            }
        });

        ImageButton closeBtn = findViewById(R.id.onboardingClose);
        closeBtn.setOnClickListener(v -> navigateToAuth());
    }

    private void updateSegments(int activePosition) {
        Drawable active = ContextCompat.getDrawable(this, R.drawable.bg_progress_segment_active);
        Drawable inactive = ContextCompat.getDrawable(this, R.drawable.bg_progress_segment);
        for (int i = 0; i < segments.length; i++) {
            segments[i].setBackground(i == activePosition ? active : inactive);
        }
    }

    private void updateCta(int position) {
        if (position == PAGE_COUNT - 1) {
            ctaBtn.setText(R.string.cta_begin);
            ctaBtn.setBackgroundResource(R.drawable.bg_button_primary);
        } else {
            ctaBtn.setText(R.string.cta_next);
            ctaBtn.setBackgroundResource(R.drawable.bg_button_primary);
        }
    }

    private void navigateToAuth() {
        Intent intent = new Intent(this, OAuthService.class);
        intent.putExtra("first_run", true);
        startActivity(intent);
        finish();
    }
}
