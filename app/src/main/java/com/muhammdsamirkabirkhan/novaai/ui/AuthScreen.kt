package com.muhammdsamirkabirkhan.novaai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muhammdsamirkabirkhan.novaai.BuildConfig
import com.muhammdsamirkabirkhan.novaai.findActivity
import com.muhammdsamirkabirkhan.novaai.openUrl

@Composable
fun AuthScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()
    var signup by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    val valid = email.contains("@") && pw.length >= (if (signup) 8 else 6) && (!signup || name.isNotBlank())

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape), Alignment.Center) {
            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("NOVA AI", style = MaterialTheme.typography.headlineLarge)
        Text(if (signup) "Create your account" else "Sign in to continue",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (signup) {
            OutlinedTextField(name, { name = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pw, { pw = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton({ show = !show }) {
                    Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle password")
                }
            })

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center) }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (signup) vm.signUp(name, email, pw) else vm.signIn(email, pw) },
            enabled = valid && !busy, modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (signup) "Create account" else "Sign in")
        }
        if (!signup) TextButton({ vm.resetPassword(email) }, enabled = email.contains("@") && !busy) { Text("Forgot password?") }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f)); Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant); HorizontalDivider(Modifier.weight(1f))
        }
        OutlinedButton({ vm.google(ctx.findActivity()) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.AccountCircle, null); Spacer(Modifier.width(8.dp)); Text("Continue with Google")
        }
        TextButton({ signup = !signup; vm.error.value = null }) {
            Text(if (signup) "Already have an account? Sign in" else "New here? Create an account")
        }
        Spacer(Modifier.weight(1f))
        Text("By continuing you agree to our", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            TextButton({ openUrl(ctx, BuildConfig.TERMS_URL) }) { Text("Terms of Service") }
            TextButton({ openUrl(ctx, BuildConfig.PRIVACY_URL) }) { Text("Privacy Policy") }
        }
    }
}
