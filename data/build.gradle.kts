// Модуль хранения: локальный профиль игрока в Room.
// Данные не покидают устройство, аккаунт и сеть не используются (ТЗ 3.5).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

// Room выгружает снимок схемы каждой версии в data/schemas. Эти файлы
// отслеживаются в репозитории: они нужны как эталон при написании и
// проверке переходов между версиями.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "ru.onefortwo.finny.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core-economy"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
