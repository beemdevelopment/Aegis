package com.beemdevelopment.aegis;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;

import com.beemdevelopment.aegis.vault.VaultManager;

import org.hamcrest.Matcher;

public abstract class AegisTest {
    protected AegisApplication getApp() {
        return (AegisApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    }

    protected VaultManager getVault() {
        return getApp().getVaultManager();
    }

    // source: https://stackoverflow.com/a/30338665
    protected static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
