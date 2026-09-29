package app.morphe.extension.twitch.layout;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class VideoAlignmentSupport {
    private static int videoId;
    private static int chatId;
    private static int vodChatId;

    private VideoAlignmentSupport() {
    }

    // Patched in right after ViewerLandingActivity.setContentView(root). The player's parent centers
    // the video on every layout pass, so re-align after each pass, before the frame is drawn.
    public static void install(View root) {
        try {
            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                try {
                    align(root);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    // Several theatres can be alive at once (e.g. a VOD opened over a live stream), so align each.
    private static void align(View root) {
        if (videoId == 0) {
            videoId = id(root, "playback_view_container");
            chatId = id(root, "chat_wrapper");
            vodChatId = id(root, "landscape_chat_container");
        }
        List<View> videos = new ArrayList<>();
        collect(root, videos);
        for (View video : videos) {
            alignVideo(video);
        }
    }

    private static void collect(View view, List<View> videos) {
        if (view.getId() == videoId) {
            videos.add(view);
            return;
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i), videos);
            }
        }
    }

    // Moves the video left just enough to clear a chat column drawn over it, never past the edge.
    private static void alignVideo(View video) {
        View parent = (View) video.getParent();
        if (parent == null || !video.isShown()) {
            return;
        }
        int width = video.getWidth();
        int centered = Math.max((parent.getWidth() - width) / 2, 0);
        int shift = 0;
        View chat = centered > 0 ? chatOf(video) : null;
        if (chat != null && chat.isShown() && chat.getWidth() > 0) {
            int[] parentLocation = new int[2];
            int[] chatLocation = new int[2];
            parent.getLocationInWindow(parentLocation);
            chat.getLocationInWindow(chatLocation);
            int overlap = parentLocation[0] + centered + width - chatLocation[0];
            if (overlap > 0 && chatLocation[0] > parentLocation[0]) {
                shift = Math.min(centered, overlap);
            }
        }
        int offset = centered - shift - video.getLeft();
        if (offset != 0) {
            video.offsetLeftAndRight(offset);
        }
    }

    // The chat of the same theatre: the nearest ancestor that contains one. Live uses chat_wrapper,
    // the VOD theatre uses landscape_chat_container.
    private static View chatOf(View video) {
        for (ViewParent p = video.getParent(); p instanceof View ancestor; p = p.getParent()) {
            View chat = ancestor.findViewById(chatId);
            if (chat == null) {
                chat = ancestor.findViewById(vodChatId);
            }
            if (chat != null) {
                return chat;
            }
        }
        return null;
    }

    private static int id(View root, String name) {
        return root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
    }
}
