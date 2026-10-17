package com.srbd.testrc

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.galaxy.GalaxyBillingMode
import com.revenuecat.purchases.galaxy.GalaxyConfiguration

object RevenueCatConfigurationFactory {
    fun build(context: Context, publicSdkKey: String): PurchasesConfiguration {
        // Keep TEST for Samsung IAP test purchases
        return GalaxyConfiguration.Builder(context, publicSdkKey, GalaxyBillingMode.TEST).build()
        //switch to PRODUCTION before beta/production distribution.
        //return GalaxyConfiguration.Builder(context, publicSdkKey, GalaxyBillingMode.   PRODUCTION).build()
    }
}
