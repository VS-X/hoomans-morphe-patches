package app.morphe.extension.twitch.layout;

import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

@SuppressWarnings("unused")
public final class CutoutSupport {
    private CutoutSupport() {
    }

    // Patched in right after ViewerLandingActivity.setContentView(root). Children receive the insets
    // this listener returns, so the whole app lays out as if the screen had no cutout.
    public static void ignoreCutout(View root) {
        try {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                try {
                    return view.onApplyWindowInsets(withoutCutout(insets));
                } catch (Throwable ignored) {
                    return view.onApplyWindowInsets(insets);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static WindowInsets withoutCutout(WindowInsets insets) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return insets.consumeDisplayCutout();
        }
        int cutout = WindowInsets.Type.displayCutout();
        return new WindowInsets.Builder(insets)
                .setInsets(cutout, Insets.NONE)
                .setInsetsIgnoringVisibility(cutout, Insets.NONE)
                .setDisplayCutout(null)
                .build();
    }
}
