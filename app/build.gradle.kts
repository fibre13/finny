// Модуль приложения: экраны Compose, навигация, точка входа.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Состояние сезона хранится в профиле одной строкой JSON.
    alias(libs.plugins.kotlin.serialization)
}

// Ключ подписи и пароли хранятся вне репозитория в keystore.properties
// (см. .gitignore) и не попадают ни в исходный код, ни в систему контроля
// версий. Файл создаётся один раз по инструкции docs/05-подпись-релиза.md.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val hasSigningConfig = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "ru.onefortwo.finny"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.onefortwo.finny"
        minSdk = 26
        targetSdk = 36
        versionCode = 18
        versionName = "0.9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // Обфускация отключена: ТЗ 7.2 требует полный исходный код без обфускации.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Без keystore.properties релиз собирается неподписанным — это
            // не ломает сборку у тех, у кого нет доступа к ключу.
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
        // Замечания Lint не должны молча накапливаться: предупреждение ломает сборку.
        warningsAsErrors = true
        abortOnError = true
    }

    testOptions {
        unitTests {
            // Требуется Robolectric: тесты отрисовки экранов выполняются на JVM.
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Закрепление разрешённых версий для того, что попадает в APK.
 *
 * Объявленные версии закреплены точно в каталоге версий, но транзитивные
 * выводятся разрешением: их в проекте 108 против 9 объявленных, и в
 * изменениях они не видны. Файл блокировок записывает их явно, поэтому
 * состав релиза становится частью истории, а не следствием разрешения.
 *
 * Закрепляется только `releaseRuntimeClasspath` — ровно то, что уходит
 * пользователю. `lockAllConfigurations()` захватил бы у Android-проекта
 * десятки конфигураций, включая тестовые и вспомогательные, и ломался бы
 * чаще, чем приносил пользы.
 *
 * После изменения зависимостей сборка остановится с сообщением о том,
 * что разрешена версия вне записанного состояния. Пересобрать состояние:
 *
 *     gradlew :app:dependencies --configuration releaseRuntimeClasspath --write-locks
 */
configurations.matching { it.name == "releaseRuntimeClasspath" }.configureEach {
    resolutionStrategy.activateDependencyLocking()
}

dependencies {
    implementation(project(":core-economy"))
    implementation(project(":content"))
    implementation(project(":data"))
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(project(":content"))
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    // Room нужен тестам напрямую: база создаётся в памяти.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
