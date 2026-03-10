package com.example.freeclipguard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freeclipguard.data.BoundDeviceStore;

public class IntroActivity extends AppCompatActivity {

    private final int[] imageResIds = new int[]{
            R.drawable.ic_intro_boundaries,
            R.drawable.ic_intro_signal,
            R.drawable.ic_intro_memory,
            R.drawable.ic_intro_privacy
    };
    private final int[] titleResIds = new int[]{
            R.string.intro_page_1_title,
            R.string.intro_page_2_title,
            R.string.intro_page_3_title,
            R.string.intro_page_4_title
    };
    private final int[] bodyResIds = new int[]{
            R.string.intro_page_1_body,
            R.string.intro_page_2_body,
            R.string.intro_page_3_body,
            R.string.intro_page_4_body
    };

    private BoundDeviceStore boundDeviceStore;
    private ImageView introImageView;
    private TextView progressTextView;
    private TextView titleTextView;
    private TextView bodyTextView;
    private View[] indicatorViews;
    private Button backButton;
    private Button nextButton;
    private Button skipButton;
    private int currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        boundDeviceStore = new BoundDeviceStore(this);
        introImageView = findViewById(R.id.introImageView);
        progressTextView = findViewById(R.id.introProgressText);
        titleTextView = findViewById(R.id.introTitleText);
        bodyTextView = findViewById(R.id.introBodyText);
        backButton = findViewById(R.id.introBackButton);
        nextButton = findViewById(R.id.introNextButton);
        skipButton = findViewById(R.id.introSkipButton);
        indicatorViews = new View[]{
                findViewById(R.id.indicator1),
                findViewById(R.id.indicator2),
                findViewById(R.id.indicator3),
                findViewById(R.id.indicator4)
        };

        backButton.setOnClickListener(view -> showPage(currentPage - 1));
        nextButton.setOnClickListener(view -> {
            if (currentPage == imageResIds.length - 1) {
                finishIntro();
                return;
            }
            showPage(currentPage + 1);
        });
        skipButton.setOnClickListener(view -> finishIntro());

        showPage(0);
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            showPage(currentPage - 1);
            return;
        }
        super.onBackPressed();
    }

    private void showPage(int pageIndex) {
        currentPage = Math.max(0, Math.min(pageIndex, imageResIds.length - 1));
        introImageView.setImageResource(imageResIds[currentPage]);
        progressTextView.setText(getString(R.string.intro_progress_pattern, currentPage + 1, imageResIds.length));
        titleTextView.setText(titleResIds[currentPage]);
        bodyTextView.setText(bodyResIds[currentPage]);

        backButton.setVisibility(currentPage == 0 ? View.INVISIBLE : View.VISIBLE);
        nextButton.setText(currentPage == imageResIds.length - 1
                ? R.string.action_intro_finish
                : R.string.action_intro_next);
        skipButton.setVisibility(currentPage == imageResIds.length - 1 ? View.INVISIBLE : View.VISIBLE);

        for (int index = 0; index < indicatorViews.length; index++) {
            indicatorViews[index].setBackgroundResource(index == currentPage
                    ? R.drawable.bg_intro_indicator_active
                    : R.drawable.bg_intro_indicator_inactive);
        }
    }

    private void finishIntro() {
        boundDeviceStore.setIntroCompleted(true);
        finish();
    }
}
