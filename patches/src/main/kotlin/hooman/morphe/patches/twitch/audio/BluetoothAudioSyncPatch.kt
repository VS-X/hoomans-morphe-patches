package hooman.morphe.patches.twitch.audio

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val EXTENSION = "Lapp/morphe/extension/twitch/audio/AudioSyncSupport;"
private const val RENDERER = "Lcom/amazonaws/ivs/player/AudioTrackRenderer;"

internal object RenderedPresentationTimeFingerprint : Fingerprint(
    definingClass = RENDERER,
    name = "getRenderedPresentationTime",
    filters = listOf(
        methodCall(definingClass = RENDERER, name = "getRenderedPositionInFrames"),
    ),
)

@Suppress("unused")
val bluetoothAudioSyncPatch = bytecodePatch(
    name = "Bluetooth audio sync",
    description = "Delays the video to match the audio delay of Bluetooth headphones, so speech " +
        "lines up with lips.",
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
        val method = RenderedPresentationTimeFingerprint.method
        val headIndex = RenderedPresentationTimeFingerprint.instructionMatches.first().index + 1
        val head = method.getInstruction<OneRegisterInstruction>(headIndex)
        if (method.getInstruction(headIndex).opcode != Opcode.MOVE_RESULT_WIDE) {
            throw PatchException("Twitch audio sync: play head result is not a move-result-wide.")
        }
        // The zero constant's register pair is free here, so borrow it for the track and restore it.
        val zero = method.instructions.firstOrNull { instruction ->
            instruction.opcode == Opcode.CONST_WIDE_16 && (instruction as WideLiteralInstruction).wideLiteral == 0L
        } as? OneRegisterInstruction
            ?: throw PatchException("Twitch audio sync: zero constant was not found.")
        val h = head.registerA
        val z = zero.registerA
        if (z == h || z == h + 1 || z + 1 == h) {
            throw PatchException("Twitch audio sync: register layout changed.")
        }
        method.addInstructions(
            headIndex + 1,
            """
                iget-object v$z, p0, $RENDERER->track:Landroid/media/AudioTrack;
                invoke-static { v$z, v$h, v${h + 1} }, $EXTENSION->presentedFrames(Landroid/media/AudioTrack;J)J
                move-result-wide v$h
                const-wide/16 v$z, 0x0
            """,
        )
    }
}
