package com.muhammdsamirkabirkhan.novaai.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muhammdsamirkabirkhan.novaai.ads.AdManager
import com.muhammdsamirkabirkhan.novaai.copyText
import com.muhammdsamirkabirkhan.novaai.data.ChatSummary
import com.muhammdsamirkabirkhan.novaai.data.Message
import com.muhammdsamirkabirkhan.novaai.findActivity
import com.muhammdsamirkabirkhan.novaai.shareText

private val suggestions = listOf(
    "Explain quantum computing simply",
    "Plan a productive week for me",
    "Write a polite follow-up email",
    "Give me 5 startup ideas",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(chat: ChatViewModel, main: MainViewModel, onUpgrade: () -> Unit) {
    val ctx = LocalContext.current
    val messages by chat.messages.collectAsStateWithLifecycle()
    val pending by chat.pending.collectAsStateWithLifecycle()
    val sending by chat.sending.collectAsStateWithLifecycle()
    val error by chat.error.collectAsStateWithLifecycle()
    val notice by chat.notice.collectAsStateWithLifecycle()
    val profile by main.profile.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var reporting by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val snack = remember { SnackbarHostState() }
    val premium = profile?.premium == true

    val showPending = pending?.let { (_, base) -> messages.size < base + 2 } == true
    val count = messages.size + (if (showPending) 1 else 0) + (if (sending) 1 else 0)
    LaunchedEffect(count) { if (count > 0) listState.animateScrollToItem(count - 1) }

    LaunchedEffect(error) {
        val e = error ?: return@LaunchedEffect
        val quota = chat.quotaHit.value
        val res = snack.showSnackbar(e, actionLabel = if (quota) "Upgrade" else "Retry", duration = SnackbarDuration.Long)
        if (res == SnackbarResult.ActionPerformed) { if (quota) onUpgrade() else chat.retry(messages.size) }
        chat.clearError()
    }
    LaunchedEffect(notice) { notice?.let { snack.showSnackbar(it); chat.notice.value = null } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("NOVA AI") },
                actions = {
                    AssistChip(
                        onClick = onUpgrade,
                        label = { Text(if (premium) "Premium" else "${profile?.remainingToday ?: 0} left today") },
                        leadingIcon = { Icon(if (premium) Icons.Filled.WorkspacePremium else Icons.Filled.Bolt, null, Modifier.size(18.dp)) },
                    )
                    IconButton({
                        chat.newChat(); input = ""
                        if (!premium) AdManager.maybeShowInterstitial(ctx.findActivity())   // natural break
                    }) { Icon(Icons.Filled.Add, "New chat") }
                },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().imePadding().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = input, onValueChange = { if (it.length <= 4000) input = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Message NOVA AI") },
                    maxLines = 5, shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { chat.send(input, messages.size); input = "" },
                    enabled = input.isNotBlank() && !sending, modifier = Modifier.size(52.dp),
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
            }
        },
    ) { pad ->
        if (count == 0) {
            Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Hi ${profile?.displayName?.substringBefore(' ').orEmpty()}, how can I help?",
                    style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    suggestions.forEach { s -> SuggestionChip({ chat.send(s, 0) }, { Text(s) }) }
                }
                Spacer(Modifier.height(16.dp))
                Text("AI can make mistakes. Check important information.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), listState, contentPadding = PaddingValues(12.dp)) {
                items(messages, key = { it.id }) { m -> Bubble(m.role == "user", m.content) { reporting = m } }
                if (showPending) item { Bubble(true, pending!!.first) {} }
                if (sending) item {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp)); Text("Thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    reporting?.let { m ->
        AlertDialog(
            onDismissRequest = { reporting = null },
            title = { Text("Report this response?") },
            text = { Text("Send this response to our team for review because it is harmful, offensive or unsafe.") },
            confirmButton = { TextButton({ chat.report(m); reporting = null }) { Text("Report") } },
            dismissButton = { TextButton({ reporting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Bubble(user: Boolean, text: String, onReport: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (user) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 320.dp),
        ) { SelectionContainer { Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) } }
        if (!user) Row {
            IconButton({ copyText(ctx, text) }, Modifier.size(36.dp)) { Icon(Icons.Filled.ContentCopy, "Copy", Modifier.size(18.dp)) }
            IconButton({ shareText(ctx, text) }, Modifier.size(36.dp)) { Icon(Icons.Filled.Share, "Share", Modifier.size(18.dp)) }
            IconButton(onReport, Modifier.size(36.dp)) { Icon(Icons.Filled.Flag, "Report response", Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun HistoryScreen(main: MainViewModel, chat: ChatViewModel, onOpen: () -> Unit) {
    val chats by main.chats.collectAsStateWithLifecycle()
    var q by rememberSaveable { mutableStateOf("") }
    var del by remember { mutableStateOf<ChatSummary?>(null) }
    val shown = remember(chats, q) { chats.filter { it.title.contains(q, ignoreCase = true) } }

    Column(Modifier.fillMaxSize()) {
        Text("History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
            placeholder = { Text("Search chats") }, leadingIcon = { Icon(Icons.Filled.Search, null) })
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(if (chats.isEmpty()) "No chats yet. Start one from the Chat tab." else "No matches.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.title.ifBlank { "New chat" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        c.updatedAt?.toDate()?.time?.let { Text(DateUtils.getRelativeTimeSpanString(it).toString()) }
                    },
                    trailingContent = { IconButton({ del = c }) { Icon(Icons.Filled.Delete, "Delete chat") } },
                    modifier = Modifier.clickable { chat.open(c.id); onOpen() },
                )
            }
        }
    }

    del?.let { c ->
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("Delete this chat?") },
            text = { Text("“${c.title}” and all its messages will be permanently deleted.") },
            confirmButton = {
                TextButton({
                    main.deleteChat(c.id)
                    if (chat.chatId.value == c.id) chat.newChat()
                    del = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton({ del = null }) { Text("Cancel") } },
        )
    }
}
