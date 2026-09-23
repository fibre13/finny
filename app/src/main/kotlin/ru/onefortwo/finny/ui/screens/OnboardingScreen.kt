package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SelectButton
import ru.onefortwo.finny.ui.theme.DisplaySettings
import ru.onefortwo.finny.ui.theme.ThemeMode

/**
 * Выбор сложности и настроек отображения на экране знакомства.
 *
 * Передаётся только при первом запуске. С главного экрана тот же экран
 * открывается как подсказка «Как играть»: сложность там уже выбрана,
 * а настройки отображения лежат в разделе для взрослого.
 */
data class OnboardingSetup(
    /** Выбранная сложность либо `null`, пока выбора не было. */
    val difficulty: Difficulty?,
    val onDifficulty: (Difficulty) -> Unit,
    val display: DisplaySettings,
    val onDisplay: (DisplaySettings) -> Unit,
)

/**
 * Знакомство с целью игры и тремя типами решений (ТЗ 2.5.1).
 *
 * При первом запуске здесь же выбирается сложность заданий: задания
 * начинаются сразу после создания питомца, значит выбор обязан быть
 * сделан раньше. Рядом стоят настройки отображения:
 * взрослый включает тёмную пару или чёрно-белый режим до того, как
 * ребёнок начнёт играть, а не после.
 *
 * Экран доступен и позже, с главного экрана: к подсказке можно вернуться
 * в любой момент.
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    continueText: String = "Дальше",
    setup: OnboardingSetup? = null,
) {
    ScreenScaffold(title = "Как играть", onBack = onBack) {
        Column {
            Text(
                text = "У тебя есть питомец Финни и монеты на каждый день. " +
                    "Монет мало, а хочется многого — поэтому каждый день ты решаешь, на что их потратить.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (setup != null) {
                DifficultyCard(selected = setup.difficulty, onSelect = setup.onDifficulty)
            }

            SectionCard(title = "Решение 1. Купить нужное") {
                Text(
                    "Корм, вода, уход. Без этого Финни грустит и голодает. " +
                        "Нужное покупают первым.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "Решение 2. Купить желаемое") {
                Text(
                    "Игрушки и украшения. Они радуют Финни. " +
                        "Если монет мало, желаемое можно отложить на завтра — это не ошибка.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "Решение 3. Отложить в копилку") {
                Text(
                    "Копилка растёт понемногу и приближает цель — например, самокат для Финни.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = "Ошибиться не страшно: прогресс не пропадёт, а исправить всё можно на следующий день.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            if (setup != null) {
                DisplayCard(settings = setup.display, onChange = setup.onDisplay)
            }

            PrimaryButton(
                text = continueText,
                enabled = setup == null || setup.difficulty != null,
                onClick = onContinue,
            )

            // Причина недоступности названа текстом: по одному виду кнопки
            // непонятно, чего она ждёт.
            if (setup != null && setup.difficulty == null) {
                Text(
                    text = "Выбери сложность, и кнопка станет доступной.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Выбор сложности заданий (ТЗ 2.5.8).
 *
 * Спрашивается сложность, а не класс и не возраст: для подбора заданий
 * этого достаточно, и тогда приложение не собирает о ребёнке никаких
 * сведений. Выбор можно поменять в разделе для взрослого.
 */
@Composable
private fun DifficultyCard(selected: Difficulty?, onSelect: (Difficulty) -> Unit) {
    SectionCard(title = "Какие задания тебе по силам") {
        Column {
            Text(
                text = "Выбери, с каких начать. Это можно поменять потом — " +
                    "ничего о тебе мы не спрашиваем.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Difficulty.entries.forEach { option ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SelectButton(
                        text = option.displayName,
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = option.hint,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Настройки отображения для взрослого.
 *
 * Стоят на первом экране, а не только в разделе для взрослого: ребёнку,
 * которому нужен чёрно-белый режим, он нужен с самого начала, а не после
 * того, как он пройдёт создание питомца в неподходящем оформлении.
 */
@Composable
private fun DisplayCard(settings: DisplaySettings, onChange: (DisplaySettings) -> Unit) {
    SectionCard(title = "Для взрослого") {
        Column {
            Text(
                text = "Оформление можно поменять сейчас или позже — в разделе для взрослого.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "Тема",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            ThemeMode.entries.forEach { mode ->
                SelectButton(
                    text = mode.displayName,
                    selected = mode == settings.themeMode,
                    onClick = { onChange(settings.copy(themeMode = mode)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }

            // Выбор пары остаётся доступным и в чёрно-белом режиме: он
            // сохраняется и вернётся, когда режим выключат. Неактивным
            // его делать нельзя — пунктирный контур означает «нажать
            // нельзя», а рядом стоит подпись «выбрано».
            if (settings.highContrast) {
                Text(
                    text = "Пока включён чёрно-белый режим, пара не видна. " +
                        "Выбор сохранится и вернётся, когда режим выключат.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Text(
                text = "Чёрно-белый режим",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            Text(
                text = "Чёрный текст на белом, толще контуры, крупнее текст и кнопки. " +
                    "Направления плана различаются штриховкой, а не цветом.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SelectButton(
                text = if (settings.highContrast) "Включён" else "Выключен",
                selected = settings.highContrast,
                onClick = { onChange(settings.copy(highContrast = !settings.highContrast)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
