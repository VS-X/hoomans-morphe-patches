package hooman.morphe.patches.twitch.layout

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val OVERLAY_CHAT_COLOR = "morphe_overlay_chat_background"

private val twitchCompatibility = Compatibility(
    name = "Twitch",
    packageName = "tv.twitch.android.app",
    appIconColor = 0x9147FF,
    targets = listOf(AppTarget("30.7.2")),
)

// Chat input state render: picks background_base or background_body (0x7f06002b / 0x7f06002c in
// 30.7.2) for chat_input_background on every state update.
internal object ChatInputBackgroundFingerprint : Fingerprint(
    strings = listOf("state"),
    filters = listOf(
        literal(0x7f06002b),
        literal(0x7f06002c),
        methodCall(definingClass = "Landroid/view/View;", name = "setBackgroundResource"),
    ),
)

private val chatInputBackgroundPatch = bytecodePatch(
    description = "Stops Twitch from repainting the chat input footer with a solid color. Applied " +
        "automatically with Overlay chat opacity.",
) {
    compatibleWith(twitchCompatibility)

    execute {
        val call = ChatInputBackgroundFingerprint.instructionMatches.last()
        ChatInputBackgroundFingerprint.method.replaceInstruction(call.index, "nop")
    }
}

@Suppress("unused")
val overlayChatOpacityPatch = resourcePatch(
    name = "Overlay chat opacity",
    description = "Makes the background of the chat shown on top of fullscreen video more see-through, " +
        "so more of the video shows behind it. Twitch's default is 85% black in dark theme.",
) {
    compatibleWith(twitchCompatibility)
    dependsOn(chatInputBackgroundPatch)

    val opacity by intOption(
        key = "opacity",
        title = "Background opacity",
        description = "How solid the chat background is, from 0 (fully transparent) to 100 (solid).",
        default = 25,
        values = mapOf("0%" to 0, "15%" to 15, "25%" to 25, "40%" to 40, "55%" to 55),
        required = true,
        validator = { it != null && it in 0..100 },
    )

    execute {
        val alpha = "%02x".format(Math.round(opacity!! * 255 / 100f))

        // Chat column containers. Stock is transparent_background: 85% black (dark) / 95% white (light).
        // Only these views get the new color, since transparent_background is shared with other panels.
        // The live theatre always inflates theatre_coordinator and re-constrains it for landscape.
        mapOf("values" to "ffffff", "values-night" to "000000").forEach { (folder, rgb) ->
            document("res/$folder/colors.xml").use { document ->
                val color = document.createElement("color")
                color.setAttribute("name", OVERLAY_CHAT_COLOR)
                color.textContent = "#$alpha$rgb"
                document.documentElement.appendChild(color)
            }
        }
        listOf(
            Triple("theatre_coordinator", "chat_wrapper", "@color/$OVERLAY_CHAT_COLOR"),
            Triple("theatre_coordinator_land", "chat_wrapper", "@color/$OVERLAY_CHAT_COLOR"),
            Triple("player_view", "landscape_chat_container", "@color/$OVERLAY_CHAT_COLOR"),
            // The input footer lets the chat column's color show through instead of a solid gray.
            Triple("chat_message_input_view", "chat_input_background", "@null"),
        ).forEach { (layout, id, background) ->
            document("res/layout/$layout.xml").use { document ->
                val nodes = document.getElementsByTagName("*")
                val view = (0 until nodes.length)
                    .mapNotNull { nodes.item(it) as? Element }
                    .singleOrNull { it.getAttribute("android:id").substringAfter("/") == id }
                    ?: throw PatchException("Twitch overlay chat: $id was not found uniquely in $layout.")
                view.setAttribute("android:background", background)
            }
        }

        // OneChat's expanded chat panel. Stock is 70% black.
        document("res/drawable/expanded_chat_background.xml").use { document ->
            val solid = document.getElementsByTagName("solid").item(0) as? Element
                ?: throw PatchException("Twitch overlay chat: expanded_chat_background has no solid fill.")
            solid.setAttribute("android:color", "#${alpha}000000")
        }
    }
}
