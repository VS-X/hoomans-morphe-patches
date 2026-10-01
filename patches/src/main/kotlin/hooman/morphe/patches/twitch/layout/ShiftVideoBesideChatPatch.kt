package hooman.morphe.patches.twitch.layout

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val VIDEO_EXTENSION = "Lapp/morphe/extension/twitch/layout/VideoAlignmentSupport;"

// Swipe-down-to-minimize hit test. Stock leaves the chat column out of the area only when chat is
// beside the video, so swipes on overlaid chat minimize the player instead of scrolling.
internal object TheatreDragAreaFingerprint : Fingerprint(
    definingClass = "Ltv/twitch/android/shared/ui/elements/draggable/ConstraintTheatreContainerView;",
    name = "q",
    filters = listOf(
        methodCall(definingClass = "Landroid/graphics/Rect;", name = "contains"),
    ),
)

@Suppress("unused")
val shiftVideoBesideChatPatch = bytecodePatch(
    name = "Shift video beside overlay chat",
    description = "When side chat is shown over the expanded video, moves the video left so less of " +
        "it sits under the chat, and lets the chat scroll without swiping the player down. The video " +
        "stays centered when chat is hidden or next to it.",
) {
    compatibleWith(
        Compatibility(
            name = "Twitch",
            packageName = "tv.twitch.android.app",
            appIconColor = 0x9147FF,
            targets = listOf(AppTarget("30.7.2")),
        ),
    )

    extendWith("extensions/twitch.mpe")

    execute {
        val match = ViewerLandingSetContentViewFingerprint.instructionMatches.first()
        val root = (match.instruction as FiveRegisterInstruction).registerD
        ViewerLandingSetContentViewFingerprint.method.addInstruction(
            match.index + 1,
            "invoke-static/range { v$root .. v$root }, $VIDEO_EXTENSION->install(Landroid/view/View;)V",
        )

        val contains = TheatreDragAreaFingerprint.instructionMatches.first()
        val area = (contains.instruction as FiveRegisterInstruction).registerC
        TheatreDragAreaFingerprint.method.addInstruction(
            contains.index,
            "invoke-static { p0, v$area }, $VIDEO_EXTENSION->clipDragArea(Landroid/view/View;Landroid/graphics/Rect;)V",
        )
    }
}
