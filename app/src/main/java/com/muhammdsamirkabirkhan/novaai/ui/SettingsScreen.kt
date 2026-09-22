package com.muhammdsamirkabirkhan.novaai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muhammdsamirkabirkhan.novaai.BuildConfig
import com.muhammdsamirkabirkhan.novaai.Prefs
import com.muhammdsamirkabirkhan.novaai.ads.AdManager
import com.muhammdsamirkabirkhan.novaai.findActivity
import com.muhammdsamirkabirkhan.novaai.openUrl
import com.muhammdsamirkabirkhan.novaai.manageSubscriptionUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onPremium: () -> Unit) {
    val ctx = LocalContext.current
    val profile by vm.profile.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val theme by Prefs.themeMode.collectAsStateWithLifecycle()
    val premium = profile?.premium == true
    var editName by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var name by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card {
            Column(Modifier.padding(16.dp)) {
                Text(profile?.displayName.orEmpty().ifBlank { "Your profile" }, style = MaterialTheme.typography.titleLarge)
                Text(profile?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip({ onPremium() }, { Text(if (premium) "Premium" else "Free plan") },
                        leadingIcon = { Icon(if (premium) Icons.Filled.WorkspacePremium else Icons.Filled.Person, null, Modifier.size(18.dp)) })
                    Spacer(Modifier.width(8.dp))
                    Text(if (premium) "" else "${profile?.remainingToday ?: 0} AI requests left today",
                        style = MaterialTheme.typography.bodySmall)
                }
                TextButton({ editName = true }) { Text("Edit name") }
            }
        }

        Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (k, l) ->
                FilterChip(theme == k, { Prefs.setTheme(k) }, { Text(l) })
            }
        }

        Text("Subscription", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        ListItem(headlineContent = { Text(if (premium) "Manage Premium" else "Upgrade to Premium") },
            leadingContent = { Icon(Icons.Filled.WorkspacePremium, null) }, modifier = Modifier.clickableItem { onPremium() })
        if (premium) ListItem(headlineContent = { Text("Cancel subscription (Google Play)") },
            supportingContent = { Text("Cancel anytime; you keep Premium until the paid period ends.") },
            leadingContent = { Icon(Icons.Filled.Cancel, null) }, modifier = Modifier.clickableItem { openUrl(ctx, manageSubscriptionUrl(ctx)) })

        Text("Privacy & legal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        ListItem(headlineContent = { Text("Privacy Policy") }, leadingContent = { Icon(Icons.Filled.PrivacyTip, null) },
            modifier = Modifier.clickableItem { openUrl(ctx, BuildConfig.PRIVACY_URL) })
        ListItem(headlineContent = { Text("Terms of Service") }, leadingContent = { Icon(Icons.Filled.Description, null) },
            modifier = Modifier.clickableItem { openUrl(ctx, BuildConfig.TERMS_URL) })
        if (!premium && AdManager.privacyOptionsRequired(ctx)) ListItem(
            headlineContent = { Text("Ad privacy choices") }, leadingContent = { Icon(Icons.Filled.AdsClick, null) },
            modifier = Modifier.clickableItem { AdManager.showPrivacyOptions(ctx.findActivity()) })
        ListItem(headlineContent = { Text("Contact support") }, leadingContent = { Icon(Icons.Filled.Email, null) },
            modifier = Modifier.clickableItem { openUrl(ctx, "mailto:${BuildConfig.SUPPORT_EMAIL}") })

        Text("Account", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        ListItem(headlineContent = { Text("Sign out") }, leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
            modifier = Modifier.clickableItem { vm.signOut(ctx) })
        ListItem(headlineContent = { Text("Delete account and data", color = MaterialTheme.colorScheme.error) },
            leadingContent = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickableItem { confirmDelete = true })

        Spacer(Modifier.height(8.dp))
        Text("NOVA AI ${BuildConfig.VERSION_NAME}\nDeveloped by muhammdsamirkabirkhan\nAI responses may be inaccurate. Not professional advice.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (editName) AlertDialog(
        onDismissRequest = { editName = false }, title = { Text("Edit name") },
        text = { OutlinedTextField(name, { name = it.take(60) }, singleLine = true) },
        confirmButton = { TextButton({ vm.rename(name); editName = false }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton({ editName = false }) { Text("Cancel") } },
    )

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete your account?") },
        text = {
            Text("This permanently deletes your profile and all chats. It does NOT cancel a Premium subscription - " +
                "cancel it first in Google Play (Settings → Subscription).")
        },
        confirmButton = { TextButton({ confirmDelete = false; vm.deleteAccount(ctx) }, enabled = !busy) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
    )
}

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier = this.then(Modifier.clickable(onClick = onClick))
