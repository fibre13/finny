package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.IconTile
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
 * Три решения — строки одной карточки, а не три карточки: на телефоне
 * 360 × 800 dp экран помещается целиком вместе с кнопкой.
 *
 * @param step номер шага знакомства либо `null`, если экран открыт
 * как подсказка.
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    continueText: String = "Далее",
    step: Int? = null,
) {
    ScreenScaffold(
        title = "Как играть",
        onBack = onBack,
        top = step?.let { { StepProgress(step = it) } },
        bottomPadding = 16.dp,
        centerOnTablet = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SupportingText(
                "У тебя есть питомец Финни. В начале каждого сезона — на три дня — приходят монеты. " +
                    "Монет мало, а хочется многого — поэтому ты решаешь, на что их потратить.",
            )

            SectionCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                bottomSpacing = 0.dp,
            ) {
                Column {
                    DecisionRow(
                        category = BudgetCategory.NEEDS,
                        tile = FinnyTheme.colors.selectedContainer,
                        title = "1. Купить нужное",
                        text = "Корм, вода, уход. Без этого Финни грустит. Покупают первым.",
                    )
                    DecisionDivider()
                    DecisionRow(
                        category = BudgetCategory.WANTS,
                        tile = FinnyTheme.colors.coinContainer,
                        title = "2. Купить желаемое",
                        text = "Игрушки и украшения радуют Финни. Их можно отложить на завтра — это не ошибка.",
                    )
                    DecisionDivider()
                    DecisionRow(
                        category = BudgetCategory.SAVINGS,
                        tile = FinnyTheme.colors.appBackground,
                        title = "3. Отложить в копилку",
                        text = "Копилка растёт понемногу и приближает цель — например, самокат.",
                    )
                }
            }

            SectionCard(
                tone = CardTone.Primary,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                bottomSpacing = 0.dp,
            ) {
                Text(
                    text = "Ошибиться не страшно: прогресс не пропадёт, а план сезона можно " +
                        "поправить в любой момент.",
                    style = CompactBody,
                )
            }

            PrimaryButton(text = continueText, onClick = onContinue)
        }
    }
}

/** Основной текст с межстрочным 22 sp: строки решений плотнее обычного абзаца. */
private val CompactBody
    @Composable get() = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)

/** Строка решения: значок направления в плитке его цвета, заголовок, пояснение. */
@Composable
private fun DecisionRow(category: BudgetCategory, tile: Color, title: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp)) {
        IconTile(size = 40.dp, container = tile) {
            BudgetDirectionIcon(category, size = 22.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = text, style = CompactBody, color = FinnyTheme.colors.onSurfaceMuted)
        }
    }
}

@Composable
private fun DecisionDivider() {
    HorizontalDivider(color = FinnyTheme.colors.onSurface.copy(alpha = 0.1f))
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
