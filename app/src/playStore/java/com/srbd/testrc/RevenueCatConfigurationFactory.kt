package com.srbd.testrc

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration

object RevenueCatConfigurationFactory {
    fun build(context: Context, publicSdkKey: String): PurchasesConfiguration {
        // Google Play Store guide: regular Purchases SDK configuration.
        return PurchasesConfiguration.Builder(context, publicSdkKey).build()
    }
}
