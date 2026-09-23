// Модуль учебного контента: JSON-файлы в assets и их загрузчик.
// Контент отделён от кода интерфейса, новое задание добавляется
// записью в JSON без изменения логики приложения (ТЗ 2.5.14, 3.4).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ru.onefortwo.finny.content"
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
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
