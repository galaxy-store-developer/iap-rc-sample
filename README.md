# Integrating RevenueCat with Samsung IAP for Galaxy Store Applications

A sample Android expense tracker that adds a RevenueCat-managed subscription to an app distributed on both **Google Play** and **Galaxy Store**. It is intentionally small so the billing integration is the focus.

The subscription unlocks additional themes (Dark, Blue, Green). Purchases, restores, and entitlement checks are handled through the [RevenueCat Purchases SDK](https://www.revenuecat.com/docs/getting-started/installation/android). The full walkthrough from Samsung blog [Integrating RevenueCat with Samsung In-App Purchase for Galaxy Store Applications](https://developer.samsung.com/galaxy-store/blog/en/2026/08/13/integrating-revenuecat-with-samsung-in-app-purchase-for-galaxy-store-applications)

> **Scope:** This project assumes RevenueCat is already integrated for Google Play Billing. This README covers the Galaxy Store specifics and the parts of the project that differ between the two stores. For general RevenueCat setup, see the [RevenueCat Android documentation](https://www.revenuecat.com/docs).

---

## Overview

- Add daily expenses with amount, category (Food, Travel, Other), optional note, and date
- Running total and per-entry list, stored locally with `SharedPreferences`
- Subscription gate for premium themes using RevenueCat entitlements
- Purchase, restore, and "check subscription" actions
- Store-specific builds from a single codebase using product flavors

### Tech Stack

| Component | Version |
| --- | --- |
| Android Gradle Plugin | 9.2.1 |
| Kotlin | 2.2.10 |
| Gradle (wrapper) | 9.4.1 |
| Compile / target SDK | 36 (minor API 1) |
| Min SDK | 24 |
| Java target | 11 |
| Jetpack Compose BOM | 2026.02.01 |
| RevenueCat (Galaxy Store flavor) | `purchases` and `purchases-store-galaxy` 10.10.0 |
| RevenueCat (Play Store flavor) | `purchases` 9.23.1 |

Versions are centralized in [gradle/libs.versions.toml](gradle/libs.versions.toml).

---

## Prerequisites

- Android Studio (current stable release with AGP 9.x support)
- JDK 17 or later for Gradle
- Android SDK Platform 36
- A RevenueCat project with the Google Play app already connected (for the `playStore` flavor)
- For the `galaxyStore` flavor: a Galaxy Store Seller Portal account with Commercial Seller status, and a RevenueCat Galaxy Store app (see [Galaxy Store Integration](#galaxy-store-integration))

---

## Installation

1. Clone the repository and open the project root in Android Studio.
2. Let Gradle sync. The Gradle wrapper downloads the required Gradle version automatically.
3. Select a build variant from **Build Variants**:
   - `playStoreDebug` for Google Play
   - `galaxyStoreDebug` for Galaxy Store
4. Run the app on a device.


> **Emulators:** Samsung IAP is not supported on emulators. Use a physical Samsung Galaxy device, or a device from Remote Test Lab (RTL), for `galaxyStore` purchase testing.

---

## Configuration

RevenueCat settings live in the `defaultConfig` and flavor blocks of [app/build.gradle.kts](app/build.gradle.kts).

| Build config field | Default | Purpose |
| --- | --- | --- |
| `REVENUECAT_PRODUCT_ID` | `premium_monthly` | Product identifier. Must match the product in the store console and RevenueCat. |
| `REVENUECAT_ENTITLEMENT_ID` | `premium` | Entitlement that unlocks themes. |
| `REVENUECAT_OFFERING_ID` | `""` (blank) | Blank uses the current offering. Set a value (for example `default`) to use a specific offering. |


### Product Flavors

The project uses a `store` flavor dimension with two flavors:

| Flavor | Store | RevenueCat store value | Configuration |
| --- | --- | --- | --- |
| `playStore` | Google Play | `PLAY_STORE` | `PurchasesConfiguration` |
| `galaxyStore` | Galaxy Store | `GALAXY_STORE` | `GalaxyConfiguration` |

Each flavor sets its own `STORE_NAME`, and `REVENUECAT_PUBLIC_KEY` build config fields. Each flavor's RevenueCat dependencies are declared in `app/build.gradle.kts`:

```kotlin
add("playStoreImplementation", libs.revenuecat.purchases.play)
add("galaxyStoreImplementation", libs.revenuecat.purchases.galaxy.core)
add("galaxyStoreImplementation", libs.revenuecat.purchases.galaxy)
```

Keep these separate. Galaxy support requires Purchases Android SDK 10.7.0 or later together with `purchases-store-galaxy`, and the Play flavor is pinned to a different SDK version.

---

## Galaxy Store Integration

This section covers what changes when adding Galaxy Store to an existing RevenueCat setup. 

### 1. Samsung Seller Portal

1. Create the Galaxy Store application in Seller Portal. You need a Commercial Seller account and a service account for RevenueCat.
2. Build the `galaxyStore` flavor and upload the binary. The binary must include the RevenueCat Galaxy Store module, or product creation will fail. Review submission is not required for this step.
3. Open the **In-App Purchase** tab, select **Add New Product**, and choose **Subscription**.
4. Use the **same product ID** as the Google Play product. Add a title and description, then set the price and billing cycle.
5. Select the product and click **Activate**. Inactive products are not visible to RevenueCat.

### 2. RevenueCat Dashboard

1. Under **Apps**, add a Galaxy Store app. Enter the application name and the package name.
2. Enter the Seller Portal service account ID and upload the service account private key.
3. Copy the generated **public API key** and place it in the `galaxyStore` flavor as `REVENUECAT_PUBLIC_KEY`.
4. Under **Product catalog → Products**, select the **Galaxy Store** tab, add a new product with the same identifier, and set the type to **Subscription**.
5. Under **Entitlements**, open the entitlement and attach the Galaxy Store product.
6. Add the product to the offering the app reads (the current offering when `REVENUECAT_OFFERING_ID` is blank).

### 3. Code Differences

The Galaxy flavor differs from the Play flavor in two places.

**Configuration factory** ([galaxyStore/RevenueCatConfigurationFactory.kt](app/src/galaxyStore/java/com/srbd/testrc/RevenueCatConfigurationFactory.kt)):

```kotlin
GalaxyConfiguration.Builder(context, publicSdkKey, GalaxyBillingMode.TEST).build()
```

**Billing mode.** The project ships in `TEST` mode so Samsung test purchases work during development. Before any beta or production distribution, change the mode to `GalaxyBillingMode.PRODUCTION`:

```kotlin
GalaxyConfiguration.Builder(context, publicSdkKey, GalaxyBillingMode.PRODUCTION).build()
```

`RevenueCatManager` is shared between flavors. It calls `Purchases.configure(...)` with whichever configuration the active flavor provides, so no store-specific logic is needed there.

---

## Testing

### Galaxy Store

1. Install the `galaxyStore` build on a Samsung device. Use a physical device or a Remote Test Lab (RTL) device. Emulators are not supported.
2. Sign in to a Samsung account that is registered as a tester for the app.
3. Purchase the subscription from the app. Use the Subscribe button in the **Theme Subscription** card.
4. Confirm that the status changes to **Active** and that the Dark, Blue, and Green themes become selectable.
5. To test cancellation, open Galaxy Store, go to **Menu → Subscriptions**, and unsubscribe.

---

## Project Structure

```text
app/
├── build.gradle.kts                     # Product flavors, RevenueCat keys, BuildConfig fields
└── src/
    ├── main/
    │   ├── java/com/srbd/testrc/
    │   │   ├── MainActivity.kt          # Compose UI and subscription flow
    │   │   ├── ExpenseRepository.kt     # Local expense and theme storage
    │   │   └── RevenueCatManager.kt     # Store-agnostic RevenueCat calls
    │   └── AndroidManifest.xml          # Declares BILLING permissions for both stores
    ├── playStore/java/com/srbd/testrc/
    │   └── RevenueCatConfigurationFactory.kt   # PurchasesConfiguration (Google Play)
    └── galaxyStore/java/com/srbd/testrc/
        └── RevenueCatConfigurationFactory.kt   # GalaxyConfiguration (Galaxy Store)
```

The only store-specific code is the `RevenueCatConfigurationFactory` in each flavor source set. `RevenueCatManager` and the UI are shared.

---

## Before Production Release

Work through this list before distributing a build outside of internal testing:

- [ ] `GalaxyBillingMode` is set to `PRODUCTION` in [galaxyStore/RevenueCatConfigurationFactory.kt](app/src/galaxyStore/java/com/srbd/testrc/RevenueCatConfigurationFactory.kt)
- [ ] Production Google Play and Galaxy Store products are active and match the RevenueCat dashboard
- [ ] Public SDK keys in `build.gradle.kts` are the production keys for each store
- [ ] The `versionCode` and `versionName` in `app/build.gradle.kts` are incremented
- [ ] Build artifacts (`.aab`, `.apk`) are not committed to the repository

---

## Resources

- [RevenueCat Android documentation](https://www.revenuecat.com/docs)
- [Integrating RevenueCat with Samsung In-App Purchase for Galaxy Store Applications](https://developer.samsung.com/galaxy-store/blog/en/2026/08/13/integrating-revenuecat-with-samsung-in-app-purchase-for-galaxy-store-applications)

---

## Support

For questions or bug reports, open an issue in this repository.
