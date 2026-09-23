package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import ru.onefortwo.finny.content.GoalContent
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.WithdrawalPreview
import ru.onefortwo.finny.ui.common.AppliqueDialog
import ru.onefortwo.finny.ui.common.AppliqueTextField
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.LocalBudgetColors

/**
 * Копилка и финансовая цель (ТЗ 2.5.7).
 *
 * Видны стоимость цели, накопленная сумма, остаток и понятный срок
 * достижения. Снятие выполняется только после отдельного подтверждения,
 * до которого показывается, как изменятся сумма и срок.
 */
@Composable
fun SavingsScreen(
    game: GameState,
    goals: List<GoalContent>,
    message: FeedbackMessage?,
    onDismissMessage: () -> Unit,
    onChooseGoal: (String) -> Unit,
    onClaimGoal: () -> Unit,
    onDeposit: (Int) -> Unit,
    onPreviewWithdrawal: (Int) -> WithdrawalPreview,
    onWithdraw: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var depositText by rememberSaveable { mutableStateOf("") }
    var withdrawText by rememberSaveable { mutableStateOf("") }
    // Хранится сумма, а не объект предпросмотра: примитив переживает
    // поворот экрана, а сам предпросмотр пересчитывается из неё.
    var previewAmount by rememberSaveable { mutableStateOf<Int?>(null) }

    val goal = game.savings.goal
    val goalTitle = goals.firstOrNull { it.id == goal?.id }?.title

    ScreenScaffold(
        title = "Копилка",
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
    ) {
        Column {
            if (goal == null) {
                SectionCard(title = "Выбери цель") {
                    Column {
                        Text(
                            "Цель — это то, ради чего копят. Выбери, что хочешь накопить.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        goals.forEach { option ->
                            SecondaryButton(
                                text = "${option.title} — ${Explanations.coins(option.price)}",
                                onClick = { onChooseGoal(option.id) },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
            } else {
                SectionCard(
                    title = goalTitle ?: "Моя цель",
                    edgeColor = LocalBudgetColors.current.savings,
                ) {
                    Column {
                        LabeledValue("Стоимость", Explanations.coins(goal.price))
                        LabeledValue("Уже накоплено", Explanations.coins(game.savings.saved))
                        LabeledValue("Осталось накопить", Explanations.coins(game.savings.remaining))
                        ProgressBar(
                            fraction = game.savings.saved.amount.toFloat() / goal.price.amount,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = LocalBudgetColors.current.savings,
                        )
                        Text(
                            text = Explanations.forecast(game.goalForecast()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // Накопленную цель нужно получить, иначе экран
                        // становится тупиковым: копилка растёт, а выбрать
                        // следующую цель нельзя (ТЗ 2.5.7, 3.4).
                        if (game.savings.saved >= goal.price) {
                            PrimaryButton(
                                text = "Получить: ${goalTitle ?: "цель"}",
                                onClick = onClaimGoal,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }

                SectionCard(title = "Отложить монеты") {
                    Column {
                        LabeledValue("Можно потратить", Explanations.coins(game.balance))
                        AmountField(
                            label = "Сколько отложить",
                            value = depositText,
                            onChange = { depositText = it },
                        )
                        val canDeposit = depositText.toIntOrNull()?.let { it > 0 } == true
                        PrimaryButton(
                            text = "Отложить в копилку",
                            enabled = canDeposit,
                            onClick = {
                                depositText.toIntOrNull()?.let(onDeposit)
                                depositText = ""
                            },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        // Причина недоступности названа текстом: по одному
                        // виду кнопки непонятно, чего она ждёт.
                        if (!canDeposit) {
                            Text(
                                "Впиши число, и кнопка станет доступной.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                SectionCard(title = "Взять из копилки") {
                    Column {
                        Text(
                            "Монеты из копилки можно забрать, но цель станет дальше.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        AmountField(
                            label = "Сколько забрать",
                            value = withdrawText,
                            onChange = { withdrawText = it },
                        )
                        val canWithdraw = withdrawText.toIntOrNull()?.let { it > 0 } == true
                        SecondaryButton(
                            text = "Посмотреть, что изменится",
                            enabled = canWithdraw,
                            onClick = {
                                withdrawText.toIntOrNull()?.let { previewAmount = it }
                            },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (!canWithdraw) {
                            Text(
                                "Впиши число, и кнопка станет доступной.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    previewAmount?.let { amount ->
        WithdrawalConfirmation(
            preview = onPreviewWithdrawal(amount),
            onConfirm = {
                onWithdraw(amount)
                withdrawText = ""
                previewAmount = null
            },
            onCancel = { previewAmount = null },
        )
    }
}

/** Поле ввода суммы: только цифры. */
@Composable
private fun AmountField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    AppliqueTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(4)) },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.padding(top = 8.dp),
    )
}

/**
 * Подтверждение снятия. До подтверждения показано, как уменьшится
 * накопленная сумма и как изменится срок достижения цели (ТЗ 2.5.7).
 */
@Composable
private fun WithdrawalConfirmation(
    preview: WithdrawalPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AppliqueDialog(
        title = "Забрать ${Explanations.coins(preview.amount)} из копилки?",
        onDismiss = onCancel,
        content = {
            LabeledValue("Сейчас в копилке", Explanations.coins(preview.savedBefore))
            LabeledValue("Станет", Explanations.coins(preview.savedAfter))
            Text(
                text = "Сейчас: ${Explanations.forecast(preview.forecastBefore)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Станет: ${Explanations.forecast(preview.forecastAfter)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        actions = {
            // В столбец: «Оставить в копилке» в строку рядом с «Забрать»
            // не помещается.
            PrimaryButton(
                text = "Забрать",
                onClick = onConfirm,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SecondaryButton(text = "Оставить в копилке", onClick = onCancel)
        },
    )
}
