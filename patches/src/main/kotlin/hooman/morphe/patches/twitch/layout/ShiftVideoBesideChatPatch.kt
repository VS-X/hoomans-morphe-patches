package hooman.morphe.patches.twitch.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val VIDEO_EXTENSION = "Lapp/morphe/extension/twitch/layout/VideoAlignmentSupport;"

@Suppress("unused")
val shiftVideoBesideChatPatch = bytecodePatch(
    name = "Shift video beside overlay chat",
    description = "When side chat is shown over the expanded video, moves the video left so less of " +
        "it sits under the chat. The video stays centered when chat is hidden or next to it.",
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
    }
}
