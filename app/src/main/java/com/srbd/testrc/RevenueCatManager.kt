package com.srbd.testrc

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore

/**
 * RevenueCat setup notes:
 * - Replace BuildConfig.REVENUECAT_PUBLIC_KEY per flavor in app/build.gradle.kts.
 * - Product ID default: BuildConfig.REVENUECAT_PRODUCT_ID = "theme_monthly".
 * - Entitlement ID default: BuildConfig.REVENUECAT_ENTITLEMENT_ID = "theme_access".
 * - Offering ID default: BuildConfig.REVENUECAT_OFFERING_ID = "" to use the current offering.
 * - Use matching product IDs in Play Console, Samsung Seller Portal, and RevenueCat dashboard.
 */
class RevenueCatManager(private val context: Context) {
    val storeName: String = BuildConfig.STORE_NAME
    val productId: String = BuildConfig.REVENUECAT_PRODUCT_ID
    val entitlementId: String = BuildConfig.REVENUECAT_ENTITLEMENT_ID
    val offeringId: String = BuildConfig.REVENUECAT_OFFERING_ID

    fun initialize(): RevenueCatStatus {
        if (BuildConfig.REVENUECAT_PUBLIC_KEY.startsWith("REVENUECAT_")) {
            return RevenueCatStatus(
                isConfigured = false,
                hasThemeAccess = false,
                message = "Replace ${BuildConfig.REVENUECAT_STORE} RevenueCat public SDK key in app/build.gradle.kts.",
            )
        }

        if (!Purchases.isConfigured) {
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(
                RevenueCatConfigurationFactory.build(context, BuildConfig.REVENUECAT_PUBLIC_KEY)
            )
        }

        return RevenueCatStatus(
            isConfigured = true,
            hasThemeAccess = false,
            message = "RevenueCat configured for $storeName.",
        )
    }

    suspend fun refreshCustomerInfo(): RevenueCatStatus {
        if (!Purchases.isConfigured) return initialize()
        return runCatching {
            Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
        }.fold(
            onSuccess = { customerInfo -> statusFromCustomerInfo(customerInfo, "Subscription status refreshed.") },
            onFailure = { error -> configuredErrorStatus("Could not refresh subscription: ${error.message}") },
        )
    }

    suspend fun purchaseTheme(activity: Activity): RevenueCatStatus {
        if (!Purchases.isConfigured) return initialize()
        return runCatching {
            val offering = if (offeringId.isBlank()) {
                Purchases.sharedInstance.awaitOfferings().current
            } else {
                Purchases.sharedInstance.awaitOfferings().get(offeringId)
            } ?: error("No RevenueCat offering found. Configure an offering that contains $productId.")

            val packageToBuy = offering.availablePackages.firstOrNull { it.product.id == productId }
                ?: offering.monthly
                ?: offering.availablePackages.firstOrNull()
                ?: error("No package found in offering ${offering.identifier}.")

            purchasePackage(activity, packageToBuy)
        }.fold(
            onSuccess = { customerInfo -> statusFromCustomerInfo(customerInfo, "Purchase completed.") },
            onFailure = { error -> configuredErrorStatus("Purchase failed or was cancelled: ${error.message}") },
        )
    }

    suspend fun restorePurchases(): RevenueCatStatus {
        if (!Purchases.isConfigured) return initialize()
        return runCatching {
            Purchases.sharedInstance.awaitRestore()
        }.fold(
            onSuccess = { customerInfo -> statusFromCustomerInfo(customerInfo, "Purchases restored.") },
            onFailure = { error -> configuredErrorStatus("Restore failed: ${error.message}") },
        )
    }

    private suspend fun purchasePackage(activity: Activity, packageToBuy: Package): CustomerInfo {
        val params = PurchaseParams.Builder(activity, packageToBuy).build()
        return Purchases.sharedInstance.awaitPurchase(params).customerInfo
    }

    private fun statusFromCustomerInfo(customerInfo: CustomerInfo, message: String): RevenueCatStatus {
        val entitlement = customerInfo.entitlements[entitlementId]
        return RevenueCatStatus(
            isConfigured = true,
            hasThemeAccess = entitlement?.isActive == true,
            message = message,
        )
    }

    private fun configuredErrorStatus(message: String): RevenueCatStatus {
        return RevenueCatStatus(isConfigured = true, hasThemeAccess = false, message = message)
    }

}

data class RevenueCatStatus(
    val isConfigured: Boolean,
    val hasThemeAccess: Boolean,
    val message: String,
)
