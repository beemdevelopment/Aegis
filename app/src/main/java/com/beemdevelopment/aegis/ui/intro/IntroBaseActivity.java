package com.beemdevelopment.aegis.ui.intro;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.ui.AegisActivity;
import com.google.android.material.button.MaterialButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class IntroBaseActivity extends AegisActivity implements IntroActivityInterface {
    private Bundle _state;
    private ViewPager2 _pager;
    private ScreenSlidePagerAdapter _adapter;
    private List<Class<? extends SlideFragment>> _slides;
    private WeakReference<SlideFragment> _currentSlide;

    private MaterialButton _btnPrevious;
    private MaterialButton _btnNext;
    private SlideIndicator _slideIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        getOnBackPressedDispatcher().addCallback(this, new BackPressHandler());

        _slides = new ArrayList<>();
        _state = new Bundle();

        _btnPrevious = findViewById(R.id.btnPrevious);
        _btnPrevious.setOnClickListener(v -> goToPreviousSlide());
        _btnNext = findViewById(R.id.btnNext);
        _btnNext.setOnClickListener(v -> goToNextSlide());
        _slideIndicator = findViewById(R.id.slideIndicator);

        _adapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        _pager = findViewById(R.id.pager);
        _pager.setAdapter(_adapter);
        _pager.setUserInputEnabled(false);
        _pager.registerOnPageChangeCallback(new SlideSkipBlocker());

        View pagerChild = _pager.getChildAt(0);
        if (pagerChild instanceof RecyclerView) {
            pagerChild.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        _state = savedInstanceState.getBundle("introState");
        updatePagerControls();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("introState", _state);
    }

    void setCurrentSlide(SlideFragment slide) {
        _currentSlide = new WeakReference<>(slide);
    }

    @Override
    public void goToNextSlide() {
        int pos = _pager.getCurrentItem();
        if (pos != _slides.size() - 1) {
            SlideFragment currentSlide = _currentSlide.get();
            if (currentSlide.isFinished()) {
                currentSlide.onSaveIntroState(_state);
                setPagerPosition(pos, 1);
            } else {
                currentSlide.onNotFinishedError();
            }
        } else {
            onDonePressed();
        }
    }

    @Override
    public void goToPreviousSlide() {
        int pos = _pager.getCurrentItem();
        if (pos != 0 && pos != _slides.size() - 1) {
            setPagerPosition(pos, -1);
        }
    }

    @Override
    public void skipToSlide(Class<? extends SlideFragment> type) {
        int i = _slides.indexOf(type);
        if (i == -1) {
            throw new IllegalStateException(String.format("Cannot skip to slide of type %s because it is not in the slide list", type.getName()));
        }

        setPagerPosition(i);
    }

    /**
     * Called before a slide change is made. Overriding gives implementers the
     * opportunity to block a slide change. onSaveIntroState is guaranteed to have been
     * called on oldSlide before onBeforeSlideChanged is called.
     * @param oldSlide the slide that is currently shown.
     * @param newSlide the next slide that will be shown.
     * @return whether to block the transition.
     */
    protected boolean onBeforeSlideChanged(@Nullable Class<? extends SlideFragment> oldSlide, @NonNull Class<? extends SlideFragment> newSlide) {
        return false;
    }

    /**
     * Called after a slide change was made.
     * @param oldSlide the slide that was previously shown.
     * @param newSlide the slide that is now shown.
     */
    protected void onAfterSlideChanged(@Nullable Class<? extends SlideFragment> oldSlide, @NonNull Class<? extends SlideFragment> newSlide) {

    }

    private void setPagerPosition(int pos) {
        Class<? extends SlideFragment> oldSlide = _currentSlide.get().getClass();
        Class<? extends SlideFragment> newSlide = _slides.get(pos);

        if (!onBeforeSlideChanged(oldSlide, newSlide)) {
            // We can't easily control the speed of the smooth scroll animation, but we
            // can at least disable it if animations are disabled
            boolean smoothScroll = !AnimationsHelper.Scale.TRANSITION.isZero(this);
            _pager.setCurrentItem(pos, smoothScroll);
        }
        onAfterSlideChanged(oldSlide, newSlide);

        updatePagerControls();
    }

    private void setPagerPosition(int pos, int delta) {
        pos += delta;
        setPagerPosition(pos);
    }

    private void updatePagerControls() {
        int pos = _pager.getCurrentItem();
        _btnPrevious.setVisibility(
                pos != 0 && pos != _slides.size() - 1
                        ? View.VISIBLE
                        : View.INVISIBLE);
        if (pos == _slides.size() - 1) {
            _btnNext.setIconResource(R.drawable.ic_outline_check_24);
        }
        _slideIndicator.setSlideCount(_slides.size());
        _slideIndicator.setCurrentSlide(pos);
    }

    @NonNull
    public Bundle getState() {
        return _state;
    }

    protected abstract void onDonePressed();

    public void addSlide(Class<? extends SlideFragment> type) {
        if (_slides.contains(type)) {
            throw new IllegalStateException(String.format("Only one slide of type %s may be added to the intro", type.getName()));
        }

        _slides.add(type);
        _slideIndicator.setSlideCount(_slides.size());

        // send 'slide changed' events for the first slide
        if (_slides.size() == 1) {
            Class<? extends SlideFragment> slide = _slides.get(0);
            onBeforeSlideChanged(null, slide);
            onAfterSlideChanged(null, slide);
        }
    }

    private class BackPressHandler extends OnBackPressedCallback {
        public BackPressHandler() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {
            goToPreviousSlide();
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, getLifecycle());
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Class<? extends SlideFragment> type = _slides.get(position);

            try {
                return type.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int getItemCount() {
            return _slides.size();
        }
    }

    private class SlideSkipBlocker extends ViewPager2.OnPageChangeCallback {
        @Override
        public void onPageScrollStateChanged(@ViewPager2.ScrollState int state) {
            // disable the buttons while scrolling to prevent disallowed skipping of slides
            boolean enabled = state == ViewPager2.SCROLL_STATE_IDLE;
            _btnNext.setEnabled(enabled);
            _btnPrevious.setEnabled(enabled);
        }
    }
}
