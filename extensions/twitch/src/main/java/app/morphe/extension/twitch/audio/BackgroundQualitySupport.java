package app.morphe.extension.twitch.audio;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class BackgroundQualitySupport {
    private static boolean pausedAuto;

    private BackgroundQualitySupport() {
    }

    // Without a video surface the IVS auto quality drops to the lowest stream, and switching back on
    // return flushes the buffer. Hold the current quality while in the background instead.
    public static void onBackgrounded(Object player, boolean backgrounded) {
        if (player == null) {
            return;
        }
        try {
            Method setAuto = player.getClass().getMethod("setAutoQualityMode", boolean.class);
            if (backgrounded) {
                pausedAuto = (boolean) player.getClass().getMethod("isAutoQualityMode").invoke(player);
                if (pausedAuto) {
                    setAuto.invoke(player, false);
                }
            } else if (pausedAuto) {
                pausedAuto = false;
                setAuto.invoke(player, true);
            }
        } catch (Throwable ignored) {
        }
    }
}
