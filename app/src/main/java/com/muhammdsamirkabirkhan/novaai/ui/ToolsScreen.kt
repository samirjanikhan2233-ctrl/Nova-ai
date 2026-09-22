package com.muhammdsamirkabirkhan.novaai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muhammdsamirkabirkhan.novaai.ads.AdManager
import com.muhammdsamirkabirkhan.novaai.copyText
import com.muhammdsamirkabirkhan.novaai.findActivity
import com.muhammdsamirkabirkhan.novaai.shareText

/** Option lists MUST match the allow-lists in backend/functions/src/index.ts. */
data class ToolDef(
    val id: String, val title: String, val blurb: String, val icon: ImageVector,
    val premium: Boolean, val hint: String, val optionLabel: String, val options: List<String>,
)

object Tools {
    val all = listOf(
        ToolDef("summarize", "Summarizer", "Condense long text into key points", Icons.Filled.Summarize, false,
            "Paste an article, email or notes…", "Style", listOf("Brief", "Bullet points", "Detailed")),
        ToolDef("translate", "Translator", "Translate into 14 languages", Icons.Filled.Translate, false,
            "Type or paste text to translate…", "Translate to",
            listOf("English", "Spanish", "French", "German", "Portuguese", "Arabic", "Hindi", "Urdu", "Bengali",
                "Chinese", "Japanese", "Russian", "Turkish", "Indonesian")),
        ToolDef("write", "Writing Assistant", "Improve, fix, shorten or expand", Icons.Filled.Edit, false,
            "Paste your draft…", "Action", listOf("Improve", "Fix grammar", "Shorten", "Expand", "Formal", "Friendly")),
        ToolDef("generate", "Text Generator", "Create posts, stories, ideas and more", Icons.Filled.AutoAwesome, false,
            "Describe what you want written…", "Type",
            listOf("Blog post", "Social media post", "Short story", "Product description", "Ideas list", "Speech")),
        ToolDef("email", "Email Composer", "Write ready-to-send emails", Icons.Filled.Email, true,
            "What should the email say? Who is it for?", "Tone",
            listOf("Professional", "Friendly", "Persuasive", "Apologetic", "Follow-up")),
        ToolDef("actions", "Meeting → Actions", "Turn notes into tasks and decisions", Icons.Filled.Checklist, true,
            "Paste meeting notes or a transcript…", "Output", listOf("Action items", "Full minutes")),
        ToolDef("planner", "Goal Planner", "Break a goal into a clear plan", Icons.Filled.CalendarMonth, true,
            "What do you want to achieve?", "Plan", listOf("Daily plan", "Weekly plan", "30-day plan")),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

@Composable
fun ToolsScreen(main: MainViewModel, onOpen: (String) -> Unit) {
    val profile by main.profile.collectAsStateWithLifecycle()
    val premium = profile?.premium == true
    Column(Modifier.fillMaxSize()) {
        Text("AI Tools", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
        if (!premium) Text("Free plan: Premium tools need 1 credit. Earn credits by watching a short ad, or go Premium.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Tools.all, key = { it.id }) { t ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(t.id) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(t.icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium)
                            Text(t.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (t.premium && !premium) Icon(Icons.Filled.Lock, "Premium", Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolScreen(toolId: String, main: MainViewModel, onBack: () -> Unit, onUpgrade: () -> Unit, vm: ToolsViewModel = viewModel()) {
    val tool = Tools.byId(toolId)
    if (tool == null) { LaunchedEffect(Unit) { onBack() }; return }
    val ctx = LocalContext.current
    val st by vm.state.collectAsStateWithLifecycle()
    val profile by main.profile.collectAsStateWithLifecycle()
    val premium = profile?.premium == true
    val credits = profile?.bonusCredits ?: 0
    var text by rememberSaveable { mutableStateOf("") }
    var option by rememberSaveable { mutableStateOf(tool.options.first()) }
    var locked by remember { mutableStateOf(false) }
    val needsCredit = tool.premium && !premium

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        TopAppBar(
            title = { Text(tool.title) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (needsCredit) AssistChip({}, { Text("Premium tool · $credits credit${if (credits == 1L) "" else "s"}") },
                leadingIcon = { Icon(Icons.Filled.WorkspacePremium, null, Modifier.size(18.dp)) })
            Text(tool.optionLabel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tool.options.forEach { o -> FilterChip(option == o, { option = o }, { Text(o) }) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                text, { if (it.length <= 6000) text = it }, Modifier.fillMaxWidth().heightIn(min = 160.dp),
                placeholder = { Text(tool.hint) }, supportingText = { Text("${text.length}/6000") },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (needsCredit && credits < 1) locked = true else vm.run(tool.id, text, option) },
                enabled = text.isNotBlank() && !st.loading, modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (st.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (needsCredit) "Run (uses 1 credit)" else "Run")
            }
            st.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
                if (st.quota) TextButton(onUpgrade) { Text("See Premium") }
            }
            st.result?.let { r ->
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    SelectionContainer { Text(r, Modifier.padding(16.dp)) }
                    Row(Modifier.padding(horizontal = 8.dp)) {
                        TextButton({ copyText(ctx, r) }) { Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copy") }
                        TextButton({ shareText(ctx, r) }) { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share") }
                        TextButton({
                            vm.reset(); text = ""
                            if (!premium) AdManager.maybeShowInterstitial(ctx.findActivity())   // natural break
                        }) { Text("New") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (locked) AlertDialog(
        onDismissRequest = { locked = false },
        title = { Text("Premium tool") },
        text = { Text("${tool.title} is a Premium tool. Watch a short ad to earn 1 credit, or upgrade for unlimited-style access with higher limits and no ads.") },
        confirmButton = { TextButton({ locked = false; onUpgrade() }) { Text("Go Premium") } },
        dismissButton = {
            Row {
                TextButton({ locked = false }) { Text("Cancel") }
                TextButton(
                    enabled = AdManager.rewardedReady,
                    onClick = {
                        locked = false
                        val uid = main.user.value?.uid ?: return@TextButton
                        AdManager.showRewarded(ctx.findActivity(), uid) { earned ->
                            if (earned) main.info.value = "Thanks! Your credit will appear in a few seconds."
                            else main.error.value = "No reward earned. Try again shortly."
                        }
                    },
                ) { Text(if (AdManager.rewardedReady) "Watch ad" else "Ad loading…") }
            }
        },
    )
}
