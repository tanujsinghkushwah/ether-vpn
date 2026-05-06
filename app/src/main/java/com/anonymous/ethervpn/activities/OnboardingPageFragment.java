package com.anonymous.ethervpn.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anonymous.ethervpn.R;

public class OnboardingPageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        ((ImageView) view.findViewById(R.id.pageIllustration))
                .setImageResource(args.getInt("illustration"));
        ((TextView) view.findViewById(R.id.pageEyebrow))
                .setText(args.getInt("eyebrow"));
        ((TextView) view.findViewById(R.id.pageTitle))
                .setText(args.getInt("title"));
        ((TextView) view.findViewById(R.id.pageDesc))
                .setText(args.getInt("desc"));
    }
}
