package hooman.morphe.patches.twitch.layout

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION = "Lapp/morphe/extension/twitch/layout/CutoutSupport;"

// The single activity hosts both the pages and the player (theatre_portal_container), so its root
// view is the one place every screen's insets pass through.
internal object ViewerLandingSetContentViewFingerprint : Fingerprint(
    definingClass = "Ltv/twitch/android/feature/viewer/landing/ViewerLandingActivity;",
    name = "onCreate",
    filters = listOf(
        methodCall(name = "setContentView", parameters = listOf("Landroid/view/View;")),
    ),
)

@Suppress("unused")
val ignoreCameraCutoutPatch = bytecodePatch(
    name = "Ignore camera cutout",
    description = "Lets the app draw under the front camera cutout instead of reserving a blank " +
        "strip for it. In landscape this gives the video more room, especially with chat on the " +
        "side. Buttons near that edge can end up under the camera.",
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
            "invoke-static/range { v$root .. v$root }, $EXTENSION->ignoreCutout(Landroid/view/View;)V",
        )
    }
}
