package hooman.morphe.patches.twitch.audio

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION = "Lapp/morphe/extension/twitch/audio/BackgroundQualitySupport;"

// Player setBackgrounded(boolean): stores the flag, then saves the current quality and switches to
// audio_only (or restores the saved quality when leaving the background).
internal object PlayerSetBackgroundedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("auto", "audio_only"),
)

@Suppress("unused")
val keepQualityInBackgroundPatch = bytecodePatch(
    name = "Keep video quality in background",
    description = "Keeps the stream at the selected quality when the screen is locked or the app is " +
        "in the background, instead of switching to audio only or dropping to the lowest quality. " +
        "Playback continues without gaps on lock and unlock, but uses more data and battery.",
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
        val method = PlayerSetBackgroundedFingerprint.method
        // Keep the flag: start() needs it to play without a video surface.
        val flagIndex = method.instructions.indexOfFirst { it.opcode == Opcode.IPUT_BOOLEAN }
        if (flagIndex < 0) {
            throw PatchException("Twitch background quality: backgrounded flag write was not found.")
        }
        val playerField = method.instructions.firstNotNullOfOrNull { instruction ->
            ((instruction as? ReferenceInstruction)?.reference as? FieldReference)
                ?.takeIf { it.type == "Lcom/amazonaws/ivs/player/MediaPlayer;" }
        } ?: throw PatchException("Twitch background quality: IVS player field was not found.")
        // v0 only holds a settings provider that is dead once the method returns here.
        method.addInstructions(
            flagIndex + 1,
            """
                iget-object v0, p0, $playerField
                invoke-static { v0, p1 }, $EXTENSION->onBackgrounded(Ljava/lang/Object;Z)V
                return-void
            """,
        )
    }
}
