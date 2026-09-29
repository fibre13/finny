package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.GlossaryEntry
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.ui.common.CardSpacing
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.IconTile
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Закрытая карточка, которую можно открыть. */
private val CardOpenable = Color(0xFFFFF3D6)

/** Закрытая карточка, которая откроется в следующие игровые дни. */
private val CardLocked = Color(0xFFFBF8F1)

/** Длительность переворота карточки. */
private const val FLIP_MS = 400

/**
 * Словарик — игра с карточками (ТЗ 2.5.11). Сверху сетка в три колонки:
 * карточка со знаком вопроса открывается нажатием, с замком — откроется
 * в следующие игровые дни. Открытая карточка переворачивается, термин
 * показывается окном, а после закрытия окна уходит в список под сеткой.
 * Когда открыты все карточки, остаётся только список.
 *
 * Открытое окно термина и прокрутка сохраняются: после перехода по ссылке
 * «Назад» возвращает в словарик на то же место с тем же окном.
 */
@Composable
fun GlossaryScreen(
    entries: List<GlossaryEntry>,
    onBack: () -> Unit,
    /** Термины, карточки которых уже можно открыть; по умолчанию — все. */
    available: Set<String> = entries.map { it.term }.toSet(),
    /** Открытые карточки — они в списке под сеткой. */
    opened: Set<String> = emptySet(),
    /** Окно термина закрыто: карточка открыта и уходит в список. */
    onOpenTerm: (String) -> Unit = {},
    /** Ссылка термина на раздел игры (см. [GlossaryEntry.link]). */
    onOpenLink: (String) -> Unit = {},
) {
    val art = rememberPixelArt()
    val closed = entries.filter { it.term !in opened }
    val list = entries.filter { it.term in opened }
    // Окно термина переживает переход по ссылке и возврат назад.
    var shown by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold(
        eyebrow = "Финансовые слова — просто",
        title = "Словарик",
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (closed.isNotEmpty()) {
                val canOpen = closed.any { it.term in available }
                SupportingText(
                    if (canOpen) {
                        "Нажми на карточку со знаком вопроса"
                    } else {
                        "Новое слово откроется в следующий игровой день"
                    },
                )
                closed.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { entry ->
                            WordCard(
                                entry = entry,
                                art = art,
                                openable = entry.term in available,
                                onFlipped = { shown = entry.term },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                if (list.isNotEmpty()) Spacer(modifier = Modifier.padding(top = 6.dp))
            }
            list.forEach { entry -> GlossaryRow(entry, art, onOpenLink) }
            Spacer(modifier = Modifier.padding(bottom = CardSpacing))
        }
    }

    val entry = shown?.let { term -> entries.firstOrNull { it.term == term } }
    if (entry != null) {
        val close = {
            shown = null
            onOpenTerm(entry.term)
        }
        FinnyDialog(
            title = entry.term,
            onDismiss = close,
            content = {
                entry.icon?.let { icon ->
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clearAndSetSemantics { },
                        contentAlignment = Alignment.Center,
                    ) {
                        Sprite(art, icon, cell = 4.dp)
                    }
                }
                Text(entry.explanation, style = MaterialTheme.typography.bodyLarge)
                entry.link?.let { link ->
                    LinkText(
                        link,
                        onClick = {
                            // Двор — не вложенный раздел: словарик закрывается, и карточка
                            // считается открытой. Из остальных разделов окно ждёт возврата.
                            if (link == "main") close()
                            onOpenLink(link)
                        },
                    )
                }
            },
            actions = { PrimaryButton(text = "Понятно", onClick = close, modifier = Modifier.fillMaxWidth()) },
        )
    }
}

/** Надпись ссылки термина на раздел игры: «Открыть … →». */
internal fun glossaryLinkText(link: String): String = when {
    link == "budget" -> "Открыть план бюджета →"
    link == "plan" -> "Открыть план →"
    link == "fact" -> "Открыть план и факт →"
    link == "shop" -> "Открыть покупки →"
    link == "savings" -> "Открыть копилку →"
    link == "goal" -> "Открыть выбор цели →"
    link == "main" -> "Открыть главный экран →"
    link == "task:change_count" -> "Открыть задание «Посчитай сдачу» →"
    link.startsWith("task:") -> "Открыть задание →"
    else -> "Открыть →"
}

@Composable
private fun LinkText(link: String, onClick: () -> Unit) {
    Text(
        text = glossaryLinkText(link),
        style = MaterialTheme.typography.labelLarge,
        color = FinnyTheme.colors.attentionText,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/**
 * Закрытая карточка сетки. Доступная — кремовая со знаком вопроса,
 * недоступная — бледнее, с замком. Нажатие переворачивает доступную
 * карточку, после переворота открывается окно термина.
 */
@Composable
private fun WordCard(
    entry: GlossaryEntry,
    art: PixelArt,
    openable: Boolean,
    onFlipped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val motion = motionAllowed()
    val scope = rememberCoroutineScope()
    val turn = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }
    val face = turn.value > 90f
    val icon = entry.icon

    Box(
        modifier = modifier
            .aspectRatio(0.8f)
            .graphicsLayer {
                rotationY = turn.value
                cameraDistance = 12f * density
            }
            .softShadow(shape, lift = false)
            .clip(shape)
            .background(if (openable) CardOpenable else CardLocked)
            .then(
                if (openable) {
                    Modifier.clickable(role = Role.Button) {
                        if (busy) return@clickable
                        busy = true
                        scope.launch {
                            if (motion) turn.animateTo(180f, tween(FLIP_MS))
                            onFlipped()
                            turn.snapTo(0f)
                            busy = false
                        }
                    }
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (openable) "Закрытая карточка, можно открыть" else "Карточка откроется позже"
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            // Обратная сторона видна зеркально — разворачиваем её обратно.
            face && icon != null ->
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) { Sprite(art, icon, cell = 3.dp) }
            openable -> QuestionMark(color = FinnyTheme.colors.attentionText)
            else -> Box(modifier = Modifier.alpha(0.55f)) { Sprite(art, "yard_icon_adult", cell = 3.dp) }
        }
    }
}

/** Крупный пиксельный знак вопроса. */
@Composable
private fun QuestionMark(color: Color) {
    val rows = listOf(
        ".KKK.",
        "K...K",
        "....K",
        "...K.",
        "..K..",
        ".....",
        "..K..",
    )
    Canvas(modifier = Modifier.size(30.dp, 42.dp).clearAndSetSemantics { }) {
        val cell = size.width / 5f
        rows.forEachIndexed { y, line ->
            line.forEachIndexed { x, c ->
                if (c == 'K') drawRect(color, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

/**
 * Открытое слово словарика: пиксельный значок, термин и объяснение.
 * Значок — только украшение и не озвучивается.
 */
@Composable
private fun GlossaryRow(entry: GlossaryEntry, art: PixelArt, onOpenLink: (String) -> Unit) {
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
            val icon = entry.icon
            if (icon != null) {
                Box(modifier = Modifier.clearAndSetSemantics { }) { Sprite(art, icon, cell = 2.dp) }
            } else {
                Text(
                    text = entry.term.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.attentionText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.term,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            SupportingText(entry.explanation, modifier = Modifier.padding(top = 2.dp))
            entry.link?.let { link -> LinkText(link, onClick = { onOpenLink(link) }) }
        }
    }
}
