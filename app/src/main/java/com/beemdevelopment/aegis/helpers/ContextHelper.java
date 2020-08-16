package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import javax.annotation.Nullable;

/**
 * ContextHelper contains some disgusting hacks to obtain the Activity/Lifecycle from a Context.
 */
public class ContextHelper {
    private ContextHelper() {

    }

    // source: https://github.com/androidx/androidx/blob/e32e1da51a0c7448c74861c667fa76738a415a89/mediarouter/mediarouter/src/main/java/androidx/mediarouter/app/MediaRouteButton.java#L425-L435
    @Nullable
    public static ComponentActivity getActivity(@NonNull Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof ComponentActivity) {
                return (ComponentActivity) context;
            }

            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
    }

    @Nullable
    public static Lifecycle getLifecycle(@NonNull Context context) {
        ComponentActivity activity = getActivity(context);
        return activity == null ? null : activity.getLifecycle();
    }
}
