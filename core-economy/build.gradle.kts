// Модуль правил игровой экономики. Чистый Kotlin, без зависимостей от Android,
// чтобы логику бюджета, накоплений и роста питомца можно было покрыть
// быстрыми модульными тестами без эмулятора (ТЗ 3.4).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
