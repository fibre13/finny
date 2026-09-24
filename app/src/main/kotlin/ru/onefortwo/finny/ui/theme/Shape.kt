package ru.onefortwo.finny.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Скругления по спецификации: 12, 18, 24, 28 dp и «таблетка».
 *
 * Роли подобраны под то, откуда их берёт Material 3: карточка читает
 * `medium`, поле ввода — `small`.
 */
val FinnyShapes = Shapes(
    // Образец цвета, внутренняя плитка.
    extraSmall = RoundedCornerShape(12.dp),
    // Поле ввода, плитка пиктограммы, кнопка шага.
    small = RoundedCornerShape(18.dp),
    // Карточка.
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    // Диалог.
    extraLarge = RoundedCornerShape(28.dp),
)

/** Кнопки, плашки и полосы прогресса скругляются полностью. */
val PillShape = RoundedCornerShape(percent = 50)
