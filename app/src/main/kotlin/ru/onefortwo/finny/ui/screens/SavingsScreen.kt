package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.GoalContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.WithdrawalPreview
import ru.onefortwo.finny.ui.common.ButtonTone
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.CoinStepper
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.FinnyTheme

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
    balance: Coins? = null,
) {
    // Суммы выставляются кнопками шага, как на экране плана: печатать
    // число с клавиатуры не нужно (замечание тестировщика о вводе).
    var depositAmount by rememberSaveable { mutableIntStateOf(0) }
    var withdrawAmount by rememberSaveable { mutableIntStateOf(0) }
    // Хранится сумма, а не объект предпросмотра: примитив переживает
    // поворот экрана, а сам предпросмотр пересчитывается из неё.
    var previewAmount by rememberSaveable { mutableStateOf<Int?>(null) }

    val goal = game.savings.goal
    val goalTitle = goals.firstOrNull { it.id == goal?.id }?.title

    ScreenScaffold(
        eyebrow = "Моя цель",
        title = "Копилка",
        balance = balance,
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
    ) {
        Column {
            if (goal == null) {
                SectionCard(title = "Выбери цель", tone = CardTone.Sage) {
                    Column {
                        SupportingText(
                            "Цель — это то, ради чего копят. Выбери, что хочешь накопить.",
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        goals.forEach { option ->
                            SecondaryButton(
                                text = "${option.title} — ${Explanations.coins(option.price)}",
                                onClick = { onChooseGoal(option.id) },
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                    }
                }
            } else {
                SectionCard(
                    eyebrow = goalTitle ?: "Моя цель",
                    title = "${game.savings.saved.amount} из ${Explanations.coins(goal.price)}",
                    tone = CardTone.Primary,
                ) {
                    Column {
                        ProgressBar(
                            fraction = game.savings.saved.amount.toFloat() / goal.price.amount,
                            color = FinnyTheme.colors.coin,
                            trackColor = FinnyTheme.colors.onPrimary.copy(alpha = 0.22f),
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                        LabeledValue("Стоимость", Explanations.coins(goal.price))
                        LabeledValue("Уже накоплено", Explanations.coins(game.savings.saved))
                        LabeledValue("Осталось накопить", Explanations.coins(game.savings.remaining))
                        SupportingText(
                            text = Explanations.forecast(game.goalForecast()),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        // Накопленную цель нужно получить, иначе экран
                        // становится тупиковым: копилка растёт, а выбрать
                        // следующую цель нельзя (ТЗ 2.5.7, 3.4).
                        if (game.savings.saved >= goal.price) {
                            PrimaryButton(
                                text = "Получить: ${goalTitle ?: "цель"}",
                                tone = ButtonTone.Coin,
                                onClick = onClaimGoal,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }

                SectionCard(title = "Отложить монеты", tone = CardTone.Coin) {
                    Column {
                        LabeledValue("Можно потратить", Explanations.coins(game.balance))
                        // Больше баланса отложить нельзя: верхняя граница — баланс.
                        val depositMax = game.balance.amount
                        val deposit = depositAmount.coerceAtMost(depositMax)
                        AmountPicker(
                            label = "Сколько отложить",
                            value = deposit,
                            max = depositMax,
                            onChange = { depositAmount = it },
                        )
                        PrimaryButton(
                            text = "Отложить в копилку",
                            enabled = deposit > 0,
                            onClick = {
                                onDeposit(deposit)
                                depositAmount = 0
                            },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        // Причина недоступности названа текстом: по одному
                        // виду кнопки непонятно, чего она ждёт.
                        if (deposit == 0) {
                            SupportingText(
                                text = if (depositMax > 0) {
                                    "Выбери сумму кнопками, и кнопка станет доступной."
                                } else {
                                    "Монет на балансе нет — откладывать пока нечего."
                                },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                SectionCard(title = "Взять из копилки") {
                    Column {
                        SupportingText("Монеты из копилки можно забрать, но цель станет дальше.")
                        // Забрать можно не больше, чем накоплено.
                        val withdrawMax = game.savings.saved.amount
                        val withdraw = withdrawAmount.coerceAtMost(withdrawMax)
                        AmountPicker(
                            label = "Сколько забрать",
                            value = withdraw,
                            max = withdrawMax,
                            onChange = { withdrawAmount = it },
                        )
                        SecondaryButton(
                            text = "Посмотреть, что изменится",
                            enabled = withdraw > 0,
                            onClick = { previewAmount = withdraw },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (withdraw == 0) {
                            SupportingText(
                                text = if (withdrawMax > 0) {
                                    "Выбери сумму кнопками, и кнопка станет доступной."
                                } else {
                                    "В копилке пока пусто — забирать нечего."
                                },
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
                withdrawAmount = 0
                previewAmount = null
            },
            onCancel = { previewAmount = null },
        )
    }
}

/**
 * Сумма кнопками шага: подпись, крупное число и «−5», «−1», «+1», «+5».
 * Число объявляется программой чтения с экрана при каждом изменении.
 */
@Composable
private fun AmountPicker(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        SupportingText(label)
        Text(
            text = Explanations.coins(value),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = 2.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        CoinStepper(
            label = label,
            value = value,
            max = max,
            onChange = onChange,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
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
    FinnyDialog(
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
