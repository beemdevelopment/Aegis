package com.beemdevelopment.aegis.ui.intro;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

public abstract class SlideFragment extends Fragment implements IntroActivityInterface {
    private WeakReference<IntroBaseActivity> _parent;

    @CallSuper
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (!(context instanceof IntroBaseActivity)) {
            throw new ClassCastException("Parent context is expected to be of type IntroBaseActivity");
        }

        _parent = new WeakReference<>((IntroBaseActivity) context);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        getParent().setCurrentSlide(this);
    }

    /**
     * Reports whether or not all required user actions are finished on this slide,
     * indicating that we're ready to move to the next slide.
     */
    public boolean isFinished() {
        return true;
    }

    /**
     * Called if the user tried to move to the next slide, but isFinished returned false.
     */
    protected void onNotFinishedError() {

    }

    /**
     * Called when the SlideFragment is expected to write its state to the given shared
     * introState. This is only called if the user navigates to the next slide, not
     * when a previous slide is next to be shown.
     */
    protected void onSaveIntroState(@NonNull Bundle introState) {

    }

    @Override
    public void goToNextSlide() {
        getParent().goToNextSlide();
    }

    @Override
    public void goToPreviousSlide() {
        getParent().goToPreviousSlide();
    }

    @Override
    public void skipToSlide(Class<? extends SlideFragment> type) {
        getParent().skipToSlide(type);
    }

    @NonNull
    @Override
    public Bundle getState() {
        return getParent().getState();
    }

    @NonNull
    private IntroBaseActivity getParent() {
        if (_parent == null || _parent.get() == null) {
            throw new IllegalStateException("This method must not be called before onAttach()");
        }

        return _parent.get();
    }
}
