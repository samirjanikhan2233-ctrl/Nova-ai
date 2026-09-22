package com.muhammdsamirkabirkhan.novaai.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.muhammdsamirkabirkhan.novaai.BuildConfig

/**
 * AdMob policy notes baked in:
 *  - Consent (UMP/GDPR) is collected BEFORE the SDK is initialised; nothing loads for Premium users.
 *  - Banners never sit next to the chat input. Interstitials only appear at natural breaks
 *    (new chat / finishing a tool), never on launch, at most once per 3 minutes and every 4th action.
 *  - Rewarded ads are always opt-in and verified server-side (SSV) before any credit is granted.
 */
object AdManager {
    var ready by mutableStateOf(false); private set
    var rewardedReady by mutableStateOf(false); private set

    private var started = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var lastInterstitial = 0L
    private var actions = 0

    fun init(activity: Activity) {
        if (started) return
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                if (info.canRequestAds()) start(activity)
            }
        }, { if (info.canRequestAds()) start(activity) })
        if (info.canRequestAds()) start(activity)
    }

    private fun start(ctx: Context) {
        if (started) return
        started = true
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG).build())
        MobileAds.initialize(ctx) { ready = true; loadInterstitial(ctx); loadRewarded(ctx) }
    }

    fun privacyOptionsRequired(ctx: Context) =
        UserMessagingPlatform.getConsentInformation(ctx).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) = UserMessagingPlatform.showPrivacyOptionsForm(activity) { }

    private fun loadInterstitial(ctx: Context) {
        InterstitialAd.load(ctx, BuildConfig.AD_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
            })
    }

    private fun loadRewarded(ctx: Context) {
        RewardedAd.load(ctx, BuildConfig.AD_REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedReady = true }
                override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null; rewardedReady = false }
            })
    }

    /** Call after a completed AI action. */
    fun onAction() { actions++ }

    /** Call ONLY at a natural break (new chat, finished a tool). No-op for Premium (never initialised). */
    fun maybeShowInterstitial(activity: Activity) {
        val ad = interstitial ?: return
        val now = SystemClock.elapsedRealtime()
        if (actions < 4 || now - lastInterstitial < 180_000) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial(activity) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { interstitial = null; loadInterstitial(activity) }
        }
        actions = 0; lastInterstitial = now
        ad.show(activity)
    }

    /** User-initiated. The credit itself is granted by your backend after Google's SSV callback. */
    fun showRewarded(activity: Activity, uid: String, onDone: (earned: Boolean) -> Unit) {
        val ad = rewarded ?: run { loadRewarded(activity); onDone(false); return }
        ad.setServerSideVerificationOptions(ServerSideVerificationOptions.Builder().setUserId(uid).build())
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null; rewardedReady = false; loadRewarded(activity); onDone(earned)
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                rewarded = null; rewardedReady = false; loadRewarded(activity); onDone(false)
            }
        }
        ad.show(activity) { earned = true }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!AdManager.ready) return
    val ctx = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp
    val size = remember(width) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, width) }
    AndroidView(
        modifier = modifier.fillMaxWidth().height(size.height.dp),
        factory = { c ->
            AdView(c).apply {
                setAdSize(size)
                adUnitId = BuildConfig.AD_BANNER
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
