package com.beemdevelopment.aegis.ui.intro;

import android.os.Bundle;

import androidx.annotation.NonNull;

public interface IntroActivityInterface {
    /**
     * Navigate to the next slide.
     */
    void goToNextSlide();

    /**
     * Navigate to the previous slide.
     */
    void goToPreviousSlide();

    /**
     * Navigate to the slide of the given type.
     */
    void skipToSlide(Class<? extends SlideFragment> type);

    /**
     * Retrieves the state of the intro. The state is shared among all slides and is
     * properly restored after a configuration change. This method may only be called
     * after onAttach has been called.
     */
    @NonNull
    Bundle getState();
}
