plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.srbd.testrc"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.srbd.testrc"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // RevenueCat dashboard defaults. Replace these if your dashboard uses different IDs.
        buildConfigField("String", "REVENUECAT_PRODUCT_ID", "\"premium_monthly\"")
        buildConfigField("String", "REVENUECAT_ENTITLEMENT_ID", "\"premium\"")
        // Leave blank to use RevenueCat's current offering. Set to e.g. "default" if you want one explicit offering.
        buildConfigField("String", "REVENUECAT_OFFERING_ID", "\"\"")
    }

    // RevenueCat Public SDK Keys
    val playStorePublicKey = "\"PLAY_STORE_PUBLIC_KEY\""
    val galaxyStorePublicKey = "\"GALAXY_STORE_PUBLIC_KEY\""
    flavorDimensions += "store"
    productFlavors {
        create("playStore") {
            dimension = "store"
            buildConfigField("String", "STORE_NAME", "\"Google Play Store\"")
            buildConfigField("String", "REVENUECAT_STORE", "\"PLAY_STORE\"")
            // Replace with the Google Play public SDK key from RevenueCat, not a secret key.
            buildConfigField("String", "REVENUECAT_PUBLIC_KEY", playStorePublicKey)
        }
        create("galaxyStore") {
            dimension = "store"
            buildConfigField("String", "STORE_NAME", "\"Galaxy Store\"")
            buildConfigField("String", "REVENUECAT_STORE", "\"GALAXY_STORE\"")
            // Replace with the Galaxy Store / Samsung IAP public SDK key from RevenueCat, not a secret key.
            buildConfigField("String", "REVENUECAT_PUBLIC_KEY", galaxyStorePublicKey)
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)


    // RevenueCat is intentionally flavor-scoped:
    // - Play Store keeps the existing working Purchases SDK version.
    // - Galaxy Store uses the RevenueCat 10.x core SDK plus the Galaxy Store module.
    // Do not replace these with a single common implementation, because Galaxy support
    // requires Purchases Android SDK 10.7.0+ and purchases-store-galaxy.
    add("playStoreImplementation", libs.revenuecat.purchases.play)
    add("galaxyStoreImplementation", libs.revenuecat.purchases.galaxy.core)
    add("galaxyStoreImplementation", libs.revenuecat.purchases.galaxy)

    // Kept only because the generated XML app theme uses Material Components.
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
