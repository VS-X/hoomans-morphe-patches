package hooman.morphe.patches.twitch.chat

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

@Suppress("unused")
val hideChatHeaderPatch = resourcePatch(
    name = "Hide chat header",
    description = "Removes the bar above chat that says \"Chat\" and holds the top gifters button, " +
        "leaving more room for messages.",
) {
    compatibleWith(
        Compatibility(
            name = "Twitch",
            packageName = "tv.twitch.android.app",
            appIconColor = 0x9147FF,
            targets = listOf(AppTarget("30.7.2")),
        ),
    )

    execute {
        // Twitch drops the header view into this container but never changes the container's own
        // visibility, so hiding it here sticks.
        document("res/layout/chat_view_delegate.xml").use { document ->
            val header = document.getElementsByTagName("FrameLayout").let { nodes ->
                (0 until nodes.length)
                    .mapNotNull { nodes.item(it) as? Element }
                    .singleOrNull { it.getAttribute("android:id").substringAfter("/") == "chat_header_container" }
            } ?: throw PatchException("Twitch chat header: chat_header_container was not found uniquely.")
            header.setAttribute("android:visibility", "gone")
        }
    }
}
