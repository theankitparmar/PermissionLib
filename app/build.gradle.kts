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
val libVersion    = "1.0.56"

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

    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ── Maven publishing (required for JitPack) ───────────────────────────────────
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
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
}
