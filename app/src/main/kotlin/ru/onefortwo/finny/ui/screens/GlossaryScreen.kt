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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.content.GlossaryEntry
import ru.onefortwo.finny.ui.common.CardSpacing
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.IconTile
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.StatusPill
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Справочный раздел с объяснением основных терминов (ТЗ 2.5.11). */
@Composable
fun GlossaryScreen(
    entries: List<GlossaryEntry>,
    onBack: () -> Unit,
    /** Слова, впервые встреченные в игре и ещё не прочитанные: отмечены «Новое». */
    newTerms: Set<String> = emptySet(),
    /** ТЕСТ 3: «План» и «Факт» ведут на экран плана. */
    onOpenLink: (String) -> Unit = {},
) {
    ScreenScaffold(
        eyebrow = "Финансовые слова — просто",
        title = "Словарик",
        onBack = onBack,
    ) {
        Column {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.forEach { entry ->
                    GlossaryRow(entry, isNew = entry.term in newTerms, onOpenLink = onOpenLink)
                }
            }
            Spacer(modifier = Modifier.padding(bottom = CardSpacing))
        }
    }
}

/**
 * Слово словарика: плитка с первой буквой, термин и объяснение.
 * Буква — только украшение и не озвучивается. Слово, впервые встреченное
 * в игре, отмечено плашкой «Новое» — словом, а не только цветом.
 */
@Composable
private fun GlossaryRow(entry: GlossaryEntry, isNew: Boolean = false, onOpenLink: (String) -> Unit = {}) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.medium
    val art = rememberPixelArt()

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
            // ТЕСТ 3: пиксельный значок термина вместо первой буквы.
            val icon = entry.icon
            if (icon != null) {
                Box(modifier = Modifier.clearAndSetSemantics { }) { Sprite(art, icon, cell = 2.dp) }
            } else {
                Text(
                    text = entry.term.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.attentionText,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (isNew) {
                StatusPill(text = "Новое", modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(
                text = entry.term,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            SupportingText(entry.explanation, modifier = Modifier.padding(top = 2.dp))
            entry.link?.let { link ->
                Text(
                    text = if (link == "fact") "Посмотреть план и факт →" else "Открыть план →",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.attentionText,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = { onOpenLink(link) })
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
    }
}
