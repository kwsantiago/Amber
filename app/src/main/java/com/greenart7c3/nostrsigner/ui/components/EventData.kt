package com.greenart7c3.nostrsigner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.greenart7c3.nostrsigner.R
import com.greenart7c3.nostrsigner.models.Account
import com.greenart7c3.nostrsigner.models.Permission
import com.greenart7c3.nostrsigner.models.SignerType
import com.greenart7c3.nostrsigner.service.model.AmberEvent
import com.greenart7c3.nostrsigner.ui.RememberType
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent

// Helper functions for tag parsing
private fun Event.getReferencedEvents(): List<String> =
    tags.filter { it.size >= 2 && it[0] == "e" }.mapNotNull { it.getOrNull(1) }

private fun Event.getReferencedPubkeys(): List<String> =
    tags.filter { it.size >= 2 && it[0] == "p" }.mapNotNull { it.getOrNull(1) }

private data class UserMetadata(
    val name: String? = null,
    val about: String? = null,
    val picture: String? = null,
)

private fun parseMetadataJson(json: String): UserMetadata? =
    try {
        val mapper = jacksonObjectMapper()
        val tree = mapper.readTree(json)
        UserMetadata(
            name = tree.get("name")?.asText(),
            about = tree.get("about")?.asText(),
            picture = tree.get("picture")?.asText(),
        )
    } catch (e: Exception) {
        null
    }

@Composable
fun EventContentPreview(event: Event) {
    when (event.kind) {
        0 -> {
            // User Metadata - Parse and display structured fields
            val metadata = parseMetadataJson(event.content)
            if (metadata != null) {
                Text(
                    stringResource(R.string.event_kind_0),
                    fontWeight = FontWeight.Bold,
                )
                metadata.name?.let {
                    ContactListDetail(
                        title = stringResource(R.string.name),
                        text = it,
                    )
                }
                metadata.about?.let {
                    Text(
                        it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            } else {
                // Fallback if JSON parsing fails
                GenericEventContent(event.content)
            }
        }
        1 -> {
            // Text Note - Show more lines for better readability
            Text(
                stringResource(R.string.event_kind_1),
                fontWeight = FontWeight.Bold,
            )
            Text(
                event.content,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        4 -> {
            // Encrypted DM - Show indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.event_kind_4),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                stringResource(R.string.encrypted_decrypted_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        5 -> {
            // Event Deletion - Show what's being deleted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.event_kind_5),
                    fontWeight = FontWeight.Bold,
                )
            }
            val deletedEvents = event.getReferencedEvents()
            if (deletedEvents.isNotEmpty()) {
                Text(
                    "Deleting ${deletedEvents.size} event(s)",
                    modifier = Modifier.padding(top = 4.dp),
                )
                deletedEvents.take(2).forEach { eventId ->
                    Text(
                        eventId.take(16) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            }
        }
        6 -> {
            // Repost
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repost",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.event_kind_6),
                    fontWeight = FontWeight.Bold,
                )
            }
            val repostedEvents = event.getReferencedEvents()
            if (repostedEvents.isNotEmpty()) {
                Text(
                    "Reposting: ${repostedEvents.first().take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        7 -> {
            // Reaction
            Text(
                stringResource(R.string.event_kind_7),
                fontWeight = FontWeight.Bold,
            )
            Text(
                event.content.ifBlank { "❤️" },
                fontSize = 32.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            val reactedEvents = event.getReferencedEvents()
            if (reactedEvents.isNotEmpty()) {
                Text(
                    "To: ${reactedEvents.first().take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
        9735 -> {
            // Zap
            Text(
                stringResource(R.string.event_kind_9735),
                fontWeight = FontWeight.Bold,
            )
            // Try to extract amount from bolt11 invoice in description tag
            val descTag = event.tags.firstOrNull { it.size >= 2 && it[0] == "description" }
            if (descTag != null) {
                Text(
                    "⚡ Zap Receipt",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                GenericEventContent(event.content)
            }
        }
        else -> {
            // Fallback for all other kinds
            GenericEventContent(event.content)
        }
    }
}

@Composable
private fun GenericEventContent(content: String) {
    Text(
        "Event content",
        fontWeight = FontWeight.Bold,
    )
    Text(
        content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
fun EventData(
    account: Account,
    modifier: Modifier,
    shouldAcceptOrReject: Boolean?,
    packageName: String?,
    appName: String,
    applicationName: String?,
    event: Event,
    rawJson: String,
    type: SignerType,
    onAccept: (RememberType) -> Unit,
    onReject: (RememberType) -> Unit,
) {
    var showMore by androidx.compose.runtime.remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    var rememberType by remember {
        mutableStateOf(RememberType.NEVER)
    }

    Column(
        modifier,
    ) {
        ProfilePicture(account)

        val permission = Permission("sign_event", event.kind)
        val text = stringResource(R.string.wants_you_to_sign_a, permission.toLocalizedString(context))
        packageName?.let {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = it,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(4.dp))
        }
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(applicationName ?: appName)
                }
                append(" $text")
            },
            fontSize = 18.sp,
        )
        Spacer(Modifier.size(4.dp))

        val content = if (event.kind == 22242) AmberEvent.relay(event) else event.content
        if (content.isNotBlank() || event is ContactListEvent) {
            key("event-data-card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (event is ContactListEvent) {
                            ContactListDetail(
                                title = stringResource(R.string.following),
                                text = "${event.verifiedFollowKeySet().size}",
                            )
                            ContactListDetail(
                                title = stringResource(R.string.relays_text),
                                text = "${event.relays()?.keys?.size ?: 0}",
                            )
                        } else {
                            EventContentPreview(event)
                        }
                    }
                }
            }
        }

        RawJsonButton(
            onCLick = {
                showMore = !showMore
            },
            if (!showMore) stringResource(R.string.show_details) else stringResource(R.string.hide_details),
        )
        if (showMore) {
            RawJson(rawJson, "", Modifier.height(200.dp), type = type)
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        RememberMyChoice(
            shouldAcceptOrReject,
            packageName,
            false,
            onAccept,
            onReject,
        ) {
            rememberType = it
        }

        AcceptRejectButtons(
            onAccept = {
                onAccept(rememberType)
            },
            onReject = {
                onReject(rememberType)
            },
        )
    }
}

@Composable
fun BunkerEventData(
    account: Account,
    modifier: Modifier,
    shouldAcceptOrReject: Boolean?,
    appName: String,
    event: Event,
    rawJson: String,
    type: SignerType,
    onAccept: (RememberType) -> Unit,
    onReject: (RememberType) -> Unit,
) {
    var showMore by androidx.compose.runtime.remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    var rememberType by remember {
        mutableStateOf(RememberType.NEVER)
    }

    Column(
        modifier,
    ) {
        ProfilePicture(account)

        val permission = Permission("sign_event", event.kind)
        val text = stringResource(R.string.wants_you_to_sign_a, permission.toLocalizedString(context))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(appName)
                }
                append(" $text")
            },
            fontSize = 18.sp,
        )
        Spacer(Modifier.size(4.dp))

        val content = if (event.kind == 22242) AmberEvent.relay(event) else event.content
        if (content.isNotBlank() || event is ContactListEvent) {
            key("event-data-card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (event is ContactListEvent) {
                            ContactListDetail(
                                title = stringResource(R.string.following),
                                text = "${event.verifiedFollowKeySet().size}",
                            )
                            ContactListDetail(
                                title = stringResource(R.string.relays_text),
                                text = "${event.relays()?.keys?.size ?: 0}",
                            )
                        } else {
                            EventContentPreview(event)
                        }
                    }
                }
            }
        }

        RawJsonButton(
            onCLick = {
                showMore = !showMore
            },
            if (!showMore) stringResource(R.string.show_details) else stringResource(R.string.hide_details),
        )
        if (showMore) {
            RawJson(rawJson, "", Modifier.height(200.dp), type = type)
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        RememberMyChoice(
            shouldAcceptOrReject,
            null,
            true,
            onAccept,
            onReject,
        ) {
            rememberType = it
        }

        AcceptRejectButtons(
            onAccept = {
                onAccept(rememberType)
            },
            onReject = {
                onReject(rememberType)
            },
        )
    }
}

@Composable
fun ContactListDetail(title: String, text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 6.dp),
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text,
        )
    }
}
