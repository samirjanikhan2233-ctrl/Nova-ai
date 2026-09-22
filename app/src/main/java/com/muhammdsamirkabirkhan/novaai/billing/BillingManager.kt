package com.muhammdsamirkabirkhan.novaai.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.muhammdsamirkabirkhan.novaai.BuildConfig
import com.muhammdsamirkabirkhan.novaai.friendly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Google Play Billing (subscriptions). The app only STARTS the purchase and forwards the purchase
 * token to the backend, which verifies it with the Google Play Developer API, acknowledges it and
 * flips the user's plan. The app never decides on its own that someone is Premium.
 */
class BillingManager(
    context: Context,
    private val scope: CoroutineScope,
    private val verify: suspend (token: String, productId: String) -> Result<Unit>,
) : PurchasesUpdatedListener {

    data class Plan(
        val basePlanId: String, val price: String, val micros: Long,
        val period: String, val offerToken: String, val details: ProductDetails,
    )

    data class State(
        val loading: Boolean = true,
        val plans: List<Plan> = emptyList(),
        val busy: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (client.isReady) { scope.launch { loadPlans() }; return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { loadPlans(); restore(silent = true) } // silent restore also retries un-acknowledged purchases
                } else {
                    _state.update { it.copy(loading = false, message = "Google Play Billing is unavailable on this device.") }
                }
            }
            override fun onBillingServiceDisconnected() { /* auto-reconnect enabled */ }
        })
    }

    private suspend fun loadPlans() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS).build()
        val res = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
        val d = res.productDetailsList?.firstOrNull()
        if (res.billingResult.responseCode != BillingClient.BillingResponseCode.OK || d == null) {
            _state.update { it.copy(loading = false, plans = emptyList(), message = "Subscriptions aren't available right now.") }
            return
        }
        val plans = d.subscriptionOfferDetails.orEmpty().groupBy { it.basePlanId }.map { (id, offers) ->
            val o = offers.firstOrNull { it.offerId == null } ?: offers.first()   // plain base-plan offer
            val phase = o.pricingPhases.pricingPhaseList.last()
            Plan(id, phase.formattedPrice, phase.priceAmountMicros, phase.billingPeriod, o.offerToken, d)
        }.sortedBy { if (it.basePlanId == BuildConfig.PLAN_MONTHLY) 0 else 1 }
        _state.update { it.copy(loading = false, plans = plans, message = null) }
    }

    fun purchase(activity: Activity, plan: Plan, uid: String) {
        val p = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.details).setOfferToken(plan.offerToken).build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(p))
            .setObfuscatedAccountId(sha256(uid))   // ties purchase to this account, no raw uid sent to Google
            .build()
        val r = client.launchBillingFlow(activity, flow)
        if (r.responseCode != BillingClient.BillingResponseCode.OK)
            _state.update { it.copy(message = "Couldn't start the purchase. Please try again.") }
    }

    override fun onPurchasesUpdated(r: BillingResult, purchases: MutableList<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach(::handle)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                scope.launch { restore(silent = false) }
            else -> _state.update { it.copy(message = "Purchase failed. You have not been charged.") }
        }
    }

    private fun handle(p: Purchase) {
        when (p.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> scope.launch {
                _state.update { it.copy(busy = true, message = null) }
                verify(p.purchaseToken, p.products.first()).fold(
                    onSuccess = { _state.update { s -> s.copy(busy = false, message = "Premium activated. Thank you!") } },
                    onFailure = { e -> _state.update { s -> s.copy(busy = false, message = e.friendly() + " Tap “Restore purchases” to retry.") } },
                )
            }
            Purchase.PurchaseState.PENDING ->
                _state.update { it.copy(message = "Payment pending. Premium activates automatically once it completes.") }
        }
    }

    /** Re-verifies every subscription owned by the signed-in Google Play account. */
    suspend fun restore(silent: Boolean = false) {
        if (!client.isReady) { connect(); return }
        if (!silent) _state.update { it.copy(busy = true, message = null) }
        val res = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        val owned = res.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        var failure: Throwable? = null
        owned.forEach { p -> verify(p.purchaseToken, p.products.first()).onFailure { failure = it } }
        if (!silent) _state.update {
            it.copy(busy = false, message = when {
                failure != null -> failure!!.friendly()
                owned.isEmpty() -> "No active subscription found for this Google account."
                else -> "Purchases restored."
            })
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun sha256(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
