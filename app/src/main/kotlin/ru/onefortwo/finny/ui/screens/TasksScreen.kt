package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.TaskQueue
import ru.onefortwo.finny.content.TaskTopic
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.Explanations
import kotlin.math.cos
import kotlin.math.sin

/**
 * Список финансовых заданий (ТЗ 2.5.8). Все задания доступны сразу, без
 * привязки к реальному времени.
 *
 * Сверху звёзды «как у отеля» — сколько
 * заданий уровня решено в этом круге; когда решены все, следующий ответ
 * начинает новый круг. Ниже — короткие карточки: картинка, название, тема,
 * «Начать →». Задание помощи питомцу — сверху после неудачного дня, иначе
 * в конце.
 *
 * @param onBack возврат; `null`, когда экран открыт вкладкой.
 * @param recoveryFirst прошлый день не удался — задание помощи сверху.
 */
@Composable
fun TasksScreen(
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    onOpenTask: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    recoveryFirst: Boolean = false,
    /** Сколько заданий решено всего и сколько до следующего сюрприза. */
    solved: Int? = null,
    toSurprise: Int? = null,
) {
    val sections = TaskQueue.sections(tasks, completedIds)
    val regular = sections.fresh + sections.solved
    val solvedInRound = regular.count { it.id in completedIds }
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }

    ScreenScaffold(
        eyebrow = "Учимся с монетами",
        title = "Задания",
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Stars(colors = colors, solved = solvedInRound, total = regular.size)
            if (solved != null) {
                // Монет за задания нет — каждые три открывают сюрприз в лавке.
                Text(
                    text = "Решено заданий: $solved. " + if (toSurprise != null) {
                        "До сюрприза в лавке — ${Explanations.tasks(toSurprise)}."
                    } else {
                        "Все сюрпризы открыты!"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (recoveryFirst) {
                sections.recovery.forEach { TaskRow(art, colors, it, highlight = true) { onOpenTask(it.id) } }
            }
            // Решённые — в конце, с кнопкой «Повтор»: числа в них будут другие.
            regular.forEach { task ->
                TaskRow(art, colors, task, highlight = false, repeat = task.id in completedIds) { onOpenTask(task.id) }
            }
            if (!recoveryFirst) {
                sections.recovery.forEach { TaskRow(art, colors, it, highlight = false) { onOpenTask(it.id) } }
            }
        }
    }
}

/** Звёзды прогресса — золотые решены, серые впереди; не нажимаются. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stars(colors: YardColors, solved: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Звёзды: $solved из $total" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        ) {
            repeat(total) { index ->
                Star(fill = if (index < solved) colors.coin else colors.cardShadow, outline = colors.outline)
            }
        }
        Text(
            text = "$solved из $total",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).clearAndSetSemantics { },
        )
    }
}

@Composable
private fun Star(fill: Color, outline: Color) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val r = size.minDimension / 2f
        val c = Offset(r, r)
        val path = Path()
        for (k in 0 until 10) {
            val radius = if (k % 2 == 0) r * 0.95f else r * 0.42f
            val a = Math.toRadians(k * 36.0 - 90.0)
            val p = Offset(c.x + (cos(a) * radius).toFloat(), c.y + (sin(a) * radius).toFloat())
            if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, fill)
        drawPath(path, outline, style = Stroke(width = 2.dp.toPx()))
    }
}

/**
 * Короткая карточка задания в пиксельной рамке. При крупном шрифте кнопка
 * «Начать →» стоит под названием: рядом с ним слова рвались посередине.
 */
@Composable
private fun TaskRow(
    art: PixelArt,
    colors: YardColors,
    task: TaskContent,
    highlight: Boolean,
    repeat: Boolean = false,
    onOpen: () -> Unit,
) {
    val large = LocalDensity.current.fontScale >= LARGE_FONT
    val start: @Composable () -> Unit = {
        // Повтор — жёлтая кнопка с тёмным текстом: светлый текст на жёлтом не читался бы.
        Text(
            text = if (repeat) "Повтор ↻" else "Начать →",
            style = MaterialTheme.typography.labelLarge,
            color = if (repeat) colors.outline else colors.card,
            modifier = Modifier
                .then(
                    if (repeat) {
                        Modifier.pixelPanel(colors.coin, colors.paper, colors.paperShadow, colors.outline, 2)
                    } else {
                        Modifier.pixelPanel(colors.green, colors.greenLight, colors.greenDark, colors.outline, 2)
                    },
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(
                if (highlight) {
                    Modifier.pixelPanel(colors.coin, colors.coin, colors.cardShadow, colors.outline, 2)
                } else {
                    Modifier.pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                },
            )
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics(mergeDescendants = true) {
                contentDescription = "${task.title}, ${task.topic.displayName}. " + if (repeat) "Повтор" else "Начать"
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaskSprite(art, task.icon ?: defaultIcon(task.topic), 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                SupportingText(task.topic.displayName)
            }
            if (!large) {
                Spacer(modifier = Modifier.width(8.dp))
                start()
            }
        }
        if (large) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) { start() }
        }
    }
}

/** Масштаб шрифта, с которого кнопка переносится под название. */
private const val LARGE_FONT = 1.3f

private fun defaultIcon(topic: TaskTopic): String = when (topic) {
    TaskTopic.BUDGET_PLANNING -> "yard_plan"
    TaskTopic.SAVINGS -> "yard_savings"
    TaskTopic.PAYMENTS -> "yard_shop"
    TaskTopic.TIME -> "item_clock"
    TaskTopic.SAFETY -> "yard_icon_adult"
    TaskTopic.RECOVERY -> "icon_care"
}
