package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Число шагов знакомства: правила, питомец, задания и имя, проверка. */
const val ONBOARDING_STEPS = 4

/**
 * Знакомство с целью игры и тремя типами решений (ТЗ 2.5.1).
 *
 * При первом запуске это первый из четырёх шагов знакомства. Экран
 * доступен и позже, с главного экрана, как подсказка «Как играть»:
 * тогда шагов над заголовком нет, а внизу кнопка возврата.
 *
 * @param step номер шага знакомства либо `null`, если экран открыт
 * как подсказка.
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    continueText: String = "Дальше",
    step: Int? = null,
) {
    ScreenScaffold(
        title = "Как играть",
        onBack = onBack,
        top = step?.let { { StepProgress(step = it) } },
    ) {
        Column {
            SupportingText(
                text = "У тебя есть питомец Финни и монеты на каждый день. " +
                    "Монет мало, а хочется многого — поэтому каждый день ты решаешь, на что их потратить.",
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SectionCard(
                title = "Решение 1. Купить нужное",
                tone = CardTone.Sage,
                icon = { BudgetDirectionIcon(BudgetCategory.NEEDS, size = 32.dp) },
            ) {
                SupportingText(
                    "Корм, вода, уход. Без этого Финни грустит и голодает. " +
                        "Нужное покупают первым.",
                )
            }

            SectionCard(
                title = "Решение 2. Купить желаемое",
                tone = CardTone.Coin,
                icon = { BudgetDirectionIcon(BudgetCategory.WANTS, size = 32.dp) },
            ) {
                SupportingText(
                    "Игрушки и украшения. Они радуют Финни. " +
                        "Если монет мало, желаемое можно отложить на завтра — это не ошибка.",
                )
            }

            SectionCard(
                title = "Решение 3. Отложить в копилку",
                icon = { BudgetDirectionIcon(BudgetCategory.SAVINGS, size = 32.dp) },
            ) {
                SupportingText(
                    "Копилка растёт понемногу и приближает цель — например, самокат для Финни.",
                )
            }

            SectionCard(tone = CardTone.Primary) {
                Text(
                    text = "Ошибиться не страшно: прогресс не пропадёт, а исправить всё можно " +
                        "на следующий день.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PrimaryButton(
                text = continueText,
                onClick = onContinue,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Шаги знакомства: полоски по числу шагов и надпись «2/4». Программа
 * чтения с экрана произносит «Шаг 2 из 4».
 */
@Composable
fun StepProgress(step: Int, total: Int = ONBOARDING_STEPS) {
    val colors = FinnyTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Шаг $step из $total" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index < step) colors.primary else colors.track),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$step/$total",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceMuted,
        )
    }
}
