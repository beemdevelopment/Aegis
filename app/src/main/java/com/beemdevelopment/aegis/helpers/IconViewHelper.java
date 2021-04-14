package com.beemdevelopment.aegis.helpers;

import android.os.Build;
import android.widget.ImageView;

import com.beemdevelopment.aegis.icons.IconType;

public class IconViewHelper {
    private IconViewHelper() {

    }

    /**
     * Sets the layer type of the given ImageView based on the given IconType. If the
     * icon type is SVG and SDK <= 27, the layer type is set to software. Otherwise, it
     * is set to hardware.
     */
    public static void setLayerType(ImageView view, IconType iconType) {
        if (iconType == IconType.SVG && Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
            return;
        }

        view.setLayerType(ImageView.LAYER_TYPE_HARDWARE, null);
    }
}
