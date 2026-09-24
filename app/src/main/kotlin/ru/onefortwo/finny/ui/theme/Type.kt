package ru.onefortwo.finny.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.onefortwo.finny.R

/**
 * Шрифт Inter (SIL Open Font License 1.1, см. docs/11-лицензии.md).
 *
 * Лежит в приложении, а не подгружается: приложение работает без сети.
 * Три статических начертания (400, 700, 800), полученные из вариативного
 * Inter с подмножеством знаков для русского и латиницы. Вариативный файл
 * не используется: Android 8.0 меняет форму знаков по оси насыщенности,
 * но не их ширину, и в жирном тексте появляются разрывы между буквами.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
)

/**
 * Типографика Material 3 по спецификации оформления, с одной поправкой:
 * основной текст не мельче 16 sp (ТЗ 3.6). В макете он 13–15 sp; здесь
 * взяты его начертание и соотношение заголовков, а размеры основного
 * текста подняты до порога.
 *
 * Мельче основного только короткие служебные надписи 14 sp жирным:
 * надзаголовки карточек, подписи вкладок навигации, отметка «выбрано».
 * Смысл ни одной из них не держится только на ней: рядом всегда стоит
 * заголовок или знак.
 */
val FinnyTypography = Typography(
    // Крупный заголовок экрана итогов.
    headlineLarge = style(32, 38, FontWeight.ExtraBold),
    // Заголовок экрана.
    headlineMedium = style(26, 32, FontWeight.ExtraBold),
    // Крупное число: сумма, ответ.
    headlineSmall = style(24, 30, FontWeight.ExtraBold),
    // Заголовок карточки.
    titleLarge = style(20, 26, FontWeight.ExtraBold),
    // Заголовок внутри карточки, пункт списка.
    titleMedium = style(18, 24, FontWeight.ExtraBold),
    // Крупный текст: условие задания.
    bodyLarge = style(18, 26),
    // Основной текст.
    bodyMedium = style(16, 24),
    // Надпись на кнопке.
    labelLarge = style(16, 22, FontWeight.ExtraBold),
    // Подпись вкладки, отметка состояния.
    labelMedium = style(14, 18, FontWeight.ExtraBold),
    // Надзаголовок прописными.
    labelSmall = style(14, 18, FontWeight.ExtraBold).copy(letterSpacing = 0.06.em),
)
