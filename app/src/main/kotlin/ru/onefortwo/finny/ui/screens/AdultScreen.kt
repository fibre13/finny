package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.FinnyTextField
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.StatusPill
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.state.ScreenTime
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Раздел для взрослого (ТЗ 2.5.12).
 *
 * Отделён от детского интерфейса простым барьером — арифметическим примером.
 * Внутри видны просветительские цели приложения, пройденные темы и общий
 * прогресс без оценок ребёнка. Сброс и удаление профиля доступны взрослому
 * без обращения к разработчику (ТЗ 3.5) и требуют подтверждения (ТЗ 3.6).
 */
@Composable
fun AdultScreen(
    game: GameState,
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    isDemo: Boolean,
    timeLimitEnabled: Boolean,
    minutesUsedToday: Int,
    onResetProfile: () -> Unit,
    onStartDemo: () -> Unit,
    onResetDemo: () -> Unit,
    onSetTimeLimit: (Boolean) -> Unit,
    onResetTodayUsage: () -> Unit,
    onBack: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }

    if (!unlocked) {
        AdultGate(onUnlock = { unlocked = true }, onBack = onBack)
    } else {
        AdultContent(
            game = game,
            tasks = tasks,
            completedIds = completedIds,
            isDemo = isDemo,
            timeLimitEnabled = timeLimitEnabled,
            minutesUsedToday = minutesUsedToday,
            onResetProfile = onResetProfile,
            onStartDemo = onStartDemo,
            onResetDemo = onResetDemo,
            onSetTimeLimit = onSetTimeLimit,
            onResetTodayUsage = onResetTodayUsage,
            onBack = onBack,
        )
    }
}

/** Барьер для взрослого: арифметический пример. */
@Composable
private fun AdultGate(onUnlock: () -> Unit, onBack: () -> Unit) {
    val first = rememberSaveable { Random.nextInt(3, 10) }
    val second = rememberSaveable { Random.nextInt(3, 10) }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(eyebrow = "Только для взрослых", title = "Раздел для взрослого", onBack = onBack) {
        Column {
            SectionCard(title = "Подтвердите, что вы взрослый") {
                Column {
                    SupportingText("Решите пример, чтобы продолжить.")
                    Text(
                        text = "$first × $second = ?",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    FinnyTextField(
                        value = answer,
                        onValueChange = { input ->
                            answer = input.filter { it.isDigit() }.take(3)
                            wrong = false
                        },
                        label = "Ответ",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (wrong) {
                        Text(
                            text = "Ответ неверный, попробуйте ещё раз.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FinnyTheme.colors.errorText,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    PrimaryButton(
                        text = "Продолжить",
                        enabled = answer.isNotBlank(),
                        onClick = {
                            if (answer.toIntOrNull() == first * second) onUnlock() else wrong = true
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    // Причина недоступности названа текстом: по одному
                    // виду кнопки непонятно, чего она ждёт.
                    if (answer.isBlank()) {
                        SupportingText(
                            "Впишите ответ, и кнопка станет доступной.",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Содержимое раздела для взрослого. */
@Composable
private fun AdultContent(
    game: GameState,
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    isDemo: Boolean,
    timeLimitEnabled: Boolean,
    minutesUsedToday: Int,
    onResetProfile: () -> Unit,
    onStartDemo: () -> Unit,
    onResetDemo: () -> Unit,
    onSetTimeLimit: (Boolean) -> Unit,
    onResetTodayUsage: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = FinnyTheme.colors
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDemo by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(eyebrow = "Спокойно о прогрессе", title = "Раздел для взрослого", onBack = onBack) {
        Column {
            SectionCard(eyebrow = "Чему учит приложение", tone = CardTone.Primary) {
                Column {
                    Text(
                        text = "Приложение знакомит ребёнка 7–11 лет с основами управления " +
                            "личными финансами в игровой форме.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    listOf(
                        "Различать обязательные и необязательные расходы",
                        "Планировать покупки при ограниченном бюджете",
                        "Ставить цель и регулярно откладывать часть средств",
                        "Оценивать свои решения и видеть их последствия",
                    ).forEach { item ->
                        Text(
                            text = "— $item",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            SectionCard(title = "Пройденные темы") {
                Column {
                    val topics = tasks.filter { it.id in completedIds }
                        .map { it.topic.displayName }
                        .distinct()

                    if (topics.isEmpty()) {
                        SupportingText("Ребёнок ещё не решал ни одного задания.")
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            topics.forEach { topic ->
                                StatusPill(
                                    text = "✓ $topic",
                                    container = colors.selectedContainer,
                                    content = colors.successText,
                                    uppercase = false,
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Общий прогресс") {
                Column {
                    LabeledValue("Завершено игровых дней", (game.period.number - 1).toString())
                    LabeledValue("Пройдено заданий", "${completedIds.size} из ${tasks.size}")
                    LabeledValue("Стадия развития питомца", game.stage.displayName)
                    SupportingText(
                        text = "Прогресс показан без оценок: приложение не сравнивает ребёнка " +
                            "с другими и не выставляет отметок.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            SectionCard(title = "Данные и приватность", tone = CardTone.Sage) {
                Column {
                    Text(
                        text = "Приложение не запрашивает персональные данные, не использует " +
                            "рекламу, платные подписки и внутриигровые покупки. Игровая валюта " +
                            "не имеет реальной стоимости. Все данные хранятся только на этом " +
                            "устройстве и удаляются вместе с профилем.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SectionCard(
                title = "Экранное время",
                tone = CardTone.Coin,
                trailing = {
                    Text(
                        text = "$minutesUsedToday мин",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
            ) {
                Column {
                    SupportingText(
                        text = "Ограничение в 20 минут в день соответствует санитарным " +
                            "требованиям к непрерывной работе с планшетом для начальной школы. " +
                            "Приложение не обрывает начатое действие: оно предупреждает заранее " +
                            "и не начинает новый игровой день.",
                    )
                    LabeledValue(
                        "Сегодня использовано",
                        "$minutesUsedToday из ${ScreenTime.DAILY_LIMIT_MINUTES} минут",
                    )
                    LabeledValue(
                        "Ограничение",
                        if (timeLimitEnabled) "включено" else "выключено",
                    )
                    // Полоса дублирует те же числа: сколько времени
                    // израсходовано, видно и цифрами, и длиной.
                    ProgressBar(
                        fraction = minutesUsedToday.toFloat() /
                            ScreenTime.DAILY_LIMIT_MINUTES,
                        color = colors.attention,
                        trackColor = colors.surface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    SupportingText(
                        text = "В обычном режиме за календарные сутки проходится один игровой " +
                            "день. В демонстрационном режиме оба ограничения не действуют.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    SecondaryButton(
                        text = if (timeLimitEnabled) "Выключить ограничение" else "Включить ограничение",
                        onClick = { onSetTimeLimit(!timeLimitEnabled) },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    SecondaryButton(
                        text = "Сбросить счётчик на сегодня",
                        onClick = onResetTodayUsage,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            SectionCard(title = "Демонстрационный режим") {
                Column {
                    SupportingText(
                        text = if (isDemo) {
                            "Демонстрационный режим включён: сейчас используется тестовый профиль."
                        } else {
                            "Режим для экспертной проверки. Создаётся тестовый профиль с " +
                                "фиксированными данными и выбранной целью."
                        },
                    )
                    SupportingText(
                        text = "Игровые дни не привязаны к календарю: все этапы проходятся " +
                            "подряд, ждать реального времени не нужно. Все задания доступны сразу.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    SecondaryButton(
                        text = if (isDemo) "Сбросить тестовый профиль" else "Включить демонстрационный режим",
                        onClick = { confirmDemo = true },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            SectionCard(title = "Управление профилем", tone = CardTone.Error) {
                Column {
                    SupportingText(
                        text = "Сброс удалит игровой прогресс: профиль питомца, монеты, " +
                            "копилку и отметки о решённых заданиях.",
                    )
                    SecondaryButton(
                        text = "Сбросить профиль",
                        onClick = { confirmReset = true },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }

    if (confirmDemo) {
        val resetting = isDemo
        FinnyDialog(
            title = if (resetting) {
                "Сбросить тестовый профиль?"
            } else {
                "Включить демонстрационный режим?"
            },
            onDismiss = { confirmDemo = false },
            content = {
                Text(
                    if (resetting) {
                        "Тестовый профиль вернётся к исходному состоянию: первый игровой день, " +
                            "стартовые монеты, пустая копилка."
                    } else {
                        "Текущий игровой прогресс будет заменён тестовым профилем."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        text = "Отмена",
                        onClick = { confirmDemo = false },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = if (resetting) "Сбросить" else "Включить",
                        onClick = {
                            confirmDemo = false
                            if (resetting) onResetDemo() else onStartDemo()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    if (confirmReset) {
        FinnyDialog(
            title = "Сбросить профиль?",
            onDismiss = { confirmReset = false },
            content = {
                Text(
                    "Весь игровой прогресс будет удалён без возможности восстановления.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        text = "Отмена",
                        onClick = { confirmReset = false },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = "Сбросить",
                        onClick = {
                            confirmReset = false
                            onResetProfile()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}
