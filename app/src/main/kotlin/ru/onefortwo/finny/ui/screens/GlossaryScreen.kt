package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.GlossaryEntry
import ru.onefortwo.finny.ui.common.CardSpacing
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.IconTile
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Справочный раздел с объяснением основных терминов (ТЗ 2.5.11). */
@Composable
fun GlossaryScreen(
    entries: List<GlossaryEntry>,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        eyebrow = "Финансовые слова — просто",
        title = "Словарик",
        onBack = onBack,
    ) {
        Column {
            SectionCard(tone = CardTone.Coin) {
                Text(
                    text = "Короткие объяснения слов, которые встречаются в игре.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.forEach { entry -> GlossaryRow(entry) }
            }
            Spacer(modifier = Modifier.padding(bottom = CardSpacing))
        }
    }
}

/**
 * Слово словарика: плитка с первой буквой, термин и объяснение.
 * Буква — только украшение и не озвучивается.
 */
@Composable
private fun GlossaryRow(entry: GlossaryEntry) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(shape, lift = false)
            .clip(shape)
            .background(colors.surface)
            .semantics(mergeDescendants = true) { }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(container = colors.appBackground) {
            Text(
                text = entry.term.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = colors.attentionText,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.term,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            SupportingText(entry.explanation, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
