package com.beemdevelopment.aegis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.beemdevelopment.aegis.ui.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@LargeTest
public class AutoLockTest extends AegisTest {
    @Before
    public void before() {
        initEncryptedVault();
    }

    @Test
    public void testLockInBackgroundKeepsMainActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            assertFalse(lockAndCheckFinishing(scenario));
        }
    }

    @Test
    public void testLockInForegroundFinishesMainActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertTrue(lockAndCheckFinishing(scenario));
        }
    }

    private boolean lockAndCheckFinishing(ActivityScenario<MainActivity> scenario) {
        AtomicBoolean finishing = new AtomicBoolean();
        scenario.onActivity(activity -> {
            _vaultManager.lock(false);
            finishing.set(activity.isFinishing());
        });
        return finishing.get();
    }
}
