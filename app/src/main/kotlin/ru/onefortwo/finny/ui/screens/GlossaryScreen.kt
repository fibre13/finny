package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.GlossaryEntry
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard

/** Справочный раздел с объяснением основных терминов (ТЗ 2.5.11). */
@Composable
fun GlossaryScreen(
    entries: List<GlossaryEntry>,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Словарик", onBack = onBack) {
        Column {
            Text(
                text = "Короткие объяснения слов, которые встречаются в игре.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            entries.forEach { entry ->
                SectionCard(title = entry.term) {
                    Text(entry.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
