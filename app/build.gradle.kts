// ============================================================
//  PermissionLib — Android Runtime Permission Library
//  Author  : Ankit Parmar
//  GitHub  : https://github.com/theankitparmar
//  Email   : codewithankit@gmail.com
// ============================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

// ── Library coordinates ───────────────────────────────────────────────────────
val libGroupId    = "com.github.theankitparmar"
val libArtifactId = "permissionlib"
val libVersion    = "1.0.57"

android {
    namespace  = "com.theankitparmar.permissionlib"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = false
        viewBinding = true
    }

    lint {
        abortOnError       = true
        warningsAsErrors   = false
        checkReleaseBuilds = true
        disable            += "GradleDependency"   // allow pinned versions
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ── Maven publishing (required for JitPack) ───────────────────────────────────
publishing {
    publications {
        register<MavenPublication>("release") {
            // components["release"] is only available after AGP processes variants
            afterEvaluate { from(components["release"]) }

            groupId    = libGroupId
            artifactId = libArtifactId
            version    = libVersion

            pom {
                name.set("PermissionLib")
                description.set("Lightweight Android runtime permission manager with a fluent Kotlin API.")
                url.set("https://github.com/theankitparmar/permissionlib")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("theankitparmar")
                        name.set("Ankit Parmar")
                        email.set("codewithankit@gmail.com")
                    }
                }
            }
        }
    }
}
