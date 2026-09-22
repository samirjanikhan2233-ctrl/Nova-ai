package com.muhammdsamirkabirkhan.novaai

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow

class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // App Check (Play Integrity) proves calls to your backend come from your genuine app.
        if (!BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
        Prefs.init(this)
    }
}

/** Tiny local preferences store (theme only; everything else lives on the server). */
object Prefs {
    private lateinit var sp: SharedPreferences
    val themeMode = MutableStateFlow("system") // system | light | dark
    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE)
        themeMode.value = sp.getString("theme", "system") ?: "system"
    }
    fun setTheme(v: String) { sp.edit().putString("theme", v).apply(); themeMode.value = v }
}
