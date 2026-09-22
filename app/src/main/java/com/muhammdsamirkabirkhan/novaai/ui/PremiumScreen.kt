package com.muhammdsamirkabirkhan.novaai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muhammdsamirkabirkhan.novaai.BuildConfig
import com.muhammdsamirkabirkhan.novaai.billing.BillingManager
import com.muhammdsamirkabirkhan.novaai.findActivity
import com.muhammdsamirkabirkhan.novaai.manageSubscriptionUrl
import com.muhammdsamirkabirkhan.novaai.openUrl
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val st by vm.billing.state.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val uid = vm.user.collectAsStateWithLifecycle().value?.uid
    val premium = profile?.premium == true
    var selected by remember(st.plans) { mutableStateOf(st.plans.lastOrNull()) }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        TopAppBar(title = { Text("NOVA AI Premium") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (premium) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Premium is active", style = MaterialTheme.typography.titleMedium)
                        val until = profile?.premiumUntil?.toDate()?.let { DateFormat.getDateInstance().format(it) }.orEmpty()
                        Text(if (profile?.autoRenewing == true) "Renews on $until" else "Access ends on $until (auto-renew is off)")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton({ openUrl(ctx, manageSubscriptionUrl(ctx)) }) { Text("Manage or cancel in Google Play") }
                    }
                }
            } else {
                Text("Get more from NOVA AI", style = MaterialTheme.typography.headlineSmall)
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Benefit("Much higher daily AI limits")
                    Benefit("All Premium tools: Email Composer, Meeting → Actions, Goal Planner")
                    Benefit("Our most capable AI model and longer answers")
                    Benefit("Longer chat memory")
                    Benefit("No advertisements")
                }
            }

            if (!premium) {
                if (st.loading) Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
                st.plans.forEach { p -> PlanCard(p, st.plans, selected == p) { selected = p } }
                Button(
                    onClick = { selected?.let { p -> uid?.let { vm.billing.purchase(ctx.findActivity(), p, it) } } },
                    enabled = selected != null && !st.busy, modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (st.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(selected?.let { "Subscribe · ${it.price}/${period(it.period)}" } ?: "Subscribe")
                }
            }
            TextButton({ vm.restorePurchases() }, Modifier.align(Alignment.CenterHorizontally), enabled = !st.busy) { Text("Restore purchases") }
            st.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            // Subscription disclosure (Google Play requires clear terms before purchase)
            Text(
                "Payment is charged to your Google Play account at confirmation. Subscriptions renew automatically at the " +
                    "price shown unless cancelled at least 24 hours before the end of the current period. You can manage or " +
                    "cancel anytime in Google Play → Profile → Payments & subscriptions → Subscriptions. Deleting the app or " +
                    "your NOVA AI account does not cancel your subscription. Refunds are handled by Google Play.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton({ openUrl(ctx, BuildConfig.TERMS_URL) }) { Text("Terms") }
                TextButton({ openUrl(ctx, BuildConfig.PRIVACY_URL) }) { Text("Privacy") }
                TextButton({ openUrl(ctx, manageSubscriptionUrl(ctx)) }) { Text("Manage") }
            }
        }
    }
}

private fun period(iso: String) = when (iso) { "P1M" -> "month"; "P1Y" -> "year"; else -> "period" }

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp)); Text(text)
    }
}

@Composable
private fun PlanCard(p: BillingManager.Plan, all: List<BillingManager.Plan>, selected: Boolean, onClick: () -> Unit) {
    val monthly = all.firstOrNull { it.basePlanId == BuildConfig.PLAN_MONTHLY }
    val save = if (p.basePlanId == BuildConfig.PLAN_YEARLY && monthly != null && monthly.micros > 0)
        (100 - (p.micros * 100 / (monthly.micros * 12))).toInt().takeIf { it > 0 } else null
    OutlinedCard(onClick, border = BorderStroke(if (selected) 2.dp else 1.dp,
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick)
            Column(Modifier.weight(1f)) {
                Text(if (p.basePlanId == BuildConfig.PLAN_YEARLY) "Yearly" else "Monthly", style = MaterialTheme.typography.titleMedium)
                save?.let { Text("Save $it%", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge) }
            }
            Text("${p.price} / ${period(p.period)}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
