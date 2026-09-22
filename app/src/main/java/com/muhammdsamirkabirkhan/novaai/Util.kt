package com.muhammdsamirkabirkhan.novaai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code

tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No activity")
}

fun Throwable.friendly(): String = when (this) {
    is FirebaseFunctionsException -> when (code) {
        Code.UNAUTHENTICATED -> "Your session expired. Please sign in again."
        Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> "Can't reach NOVA AI. Check your connection and try again."
        Code.INTERNAL -> "Something went wrong on our side. Please try again."
        else -> message ?: "Something went wrong."
    }
    is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 8 characters."
    is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    is FirebaseNetworkException -> "No internet connection."
    else -> message ?: "Something went wrong."
}

/** True when the server refused because of a usage limit or a Premium-only feature. */
val Throwable.isQuota: Boolean
    get() = this is FirebaseFunctionsException &&
        (code == Code.RESOURCE_EXHAUSTED || code == Code.PERMISSION_DENIED)

fun copyText(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("NOVA AI", text))
    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
}

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(i, "Share"))
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

fun manageSubscriptionUrl(ctx: Context) =
    "https://play.google.com/store/account/subscriptions?sku=${BuildConfig.PREMIUM_PRODUCT_ID}&package=${ctx.packageName}"
