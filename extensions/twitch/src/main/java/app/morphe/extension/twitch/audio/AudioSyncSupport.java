package app.morphe.extension.twitch.audio;

import android.media.AudioTimestamp;
import android.media.AudioTrack;

@SuppressWarnings("unused")
public final class AudioSyncSupport {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private AudioSyncSupport() {
    }

    // The IVS player syncs video to the AudioTrack play head, which counts frames handed to the
    // mixer. getTimestamp reports frames actually heard, including Bluetooth delay, so subtract the gap.
    public static long presentedFrames(AudioTrack track, long headFrames) {
        try {
            AudioTimestamp timestamp = new AudioTimestamp();
            if (!track.getTimestamp(timestamp)) {
                return headFrames;
            }
            long elapsedNanos = System.nanoTime() - timestamp.nanoTime;
            if (elapsedNanos < 0 || elapsedNanos > NANOS_PER_SECOND) {
                return headFrames;
            }
            int sampleRate = track.getSampleRate();
            double speed = track.getPlaybackParams().getSpeed();
            long heardFrames = timestamp.framePosition
                    + (long) (elapsedNanos * speed * sampleRate / NANOS_PER_SECOND);
            long latencyFrames = (track.getPlaybackHeadPosition() & 0xFFFFFFFFL) - heardFrames;
            if (latencyFrames <= 0 || latencyFrames > sampleRate) {
                return headFrames;
            }
            return headFrames - latencyFrames;
        } catch (Throwable ignored) {
            return headFrames;
        }
    }
}
