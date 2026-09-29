package hooman.morphe.patches.twitch.emotes

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val CHANNEL_CONNECTION_KEY =
    "Ltv/twitch/android/shared/chat/pub/messages/data/ChannelChatConnectionKey;"
internal const val VOD_TEXT_SETTER =
    "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V"
internal const val CHAT_TEXT_SETTER =
    "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;Landroid/widget/TextView\$BufferType;)V"

// Twitch keeps this Parcelable model unobfuscated because it crosses the public chat connection API.
// Its constructor receives the numeric broadcaster ID required by both 7TV and BTTV.
internal object ChannelConnectionConstructorFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        custom = { _, classDef -> classDef.type == CHANNEL_CONNECTION_KEY },
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
)

// MessageRecyclerItem is R8-renamed, but its data-class toString labels survive. sourceChannelId is
// also used as the per-row fallback when Shared Chat shows messages from another broadcaster.
internal object MessageRecyclerItemClassFingerprint : Fingerprint(
    strings = listOf(
        "MessageRecyclerItem(messageId=",
        ", sourceChannelId=",
    ),
)

// VOD chat replay ("chomments") rows. ChommentModel's toString labels survive R8, and its channelId
// is the VOD owner's broadcaster ID.
internal object ChommentModelClassFingerprint : Fingerprint(
    strings = listOf(
        "ChommentModel(id=",
        ", channelId=",
    ),
)

// The VOD chat row item's constructor null-checks both of these parameter names.
internal object ChommentRowItemConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "chommentModel",
        "messageListStyle",
    ),
)
