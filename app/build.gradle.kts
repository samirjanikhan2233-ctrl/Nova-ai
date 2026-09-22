import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

fun loadProps(name: String) = Properties().apply {
    val f = rootProject.file(name)
    if (f.exists()) f.inputStream().use { load(it) }
}
val keystoreProps = loadProps("keystore.properties")
val nova = loadProps("nova.properties")
fun q(s: String) = "\"$s\""

android {
    namespace = "com.muhammdsamirkabirkhan.novaai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.muhammdsamirkabirkhan.novaai"
        minSdk = 26
        targetSdk = 36            // Google Play requires API 36 for new apps/updates from 31 Aug 2026
        versionCode = 1           // increase for EVERY upload to Play
        versionName = "1.0.0"

        buildConfigField("String", "PREMIUM_PRODUCT_ID", q("nova_premium"))
        buildConfigField("String", "PLAN_MONTHLY", q("monthly"))
        buildConfigField("String", "PLAN_YEARLY", q("yearly"))
        buildConfigField("String", "PRIVACY_URL", q(nova.getProperty("PRIVACY_URL", "https://example.com/privacy.html")))
        buildConfigField("String", "TERMS_URL", q(nova.getProperty("TERMS_URL", "https://example.com/terms.html")))
        buildConfigField("String", "SUPPORT_EMAIL", q(nova.getProperty("SUPPORT_EMAIL", "support@example.com")))
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Google's official TEST ad units: safe to click, never use real IDs in debug.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "AD_BANNER", q("ca-app-pub-3940256099942544/9214589741"))
            buildConfigField("String", "AD_INTERSTITIAL", q("ca-app-pub-3940256099942544/1033173712"))
            buildConfigField("String", "AD_REWARDED", q("ca-app-pub-3940256099942544/5224354917"))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["admobAppId"] = nova.getProperty("ADMOB_APP_ID", "")
            buildConfigField("String", "AD_BANNER", q(nova.getProperty("AD_BANNER_ID", "")))
            buildConfigField("String", "AD_INTERSTITIAL", q(nova.getProperty("AD_INTERSTITIAL_ID", "")))
            buildConfigField("String", "AD_REWARDED", q(nova.getProperty("AD_REWARDED_ID", "")))
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    bundle { language { enableSplit = false } }   // keep all UI languages in the AAB
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

// Refuse to build a release without real AdMob IDs, legal URLs and a signing key.
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    doFirst {
        val need = listOf("ADMOB_APP_ID", "AD_BANNER_ID", "AD_INTERSTITIAL_ID", "AD_REWARDED_ID", "PRIVACY_URL", "TERMS_URL")
        val missing = need.filter { nova.getProperty(it).isNullOrBlank() }
        require(missing.isEmpty()) { "nova.properties is missing: $missing (see nova.properties.example)" }
        require(keystoreProps.isNotEmpty()) { "keystore.properties not found (see docs/PUBLISHING_GUIDE.md)" }
        require(file("google-services.json").exists()) { "app/google-services.json missing (download from Firebase)" }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")

    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.android.billingclient:billing-ktx:8.0.0")
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
