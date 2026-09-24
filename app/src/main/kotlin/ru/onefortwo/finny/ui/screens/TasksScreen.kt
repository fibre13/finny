package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.TaskQueue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard

/**
 * Список финансовых заданий (ТЗ 2.5.8).
 *
 * Все задания доступны сразу, без привязки к реальному времени. Порядок
 * задаёт [TaskQueue]: сверху новые задания в порядке выдачи, первое из них
 * выделено основной кнопкой; ниже — уже решённые, их можно решить ещё раз;
 * в конце — задание восстановления, которое пригождается после неудачного
 * дня. Тема каждого задания подписана на его карточке.
 */
@Composable
fun TasksScreen(
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    onOpenTask: (String) -> Unit,
    onBack: () -> Unit,
) {
    val sections = TaskQueue.sections(tasks, completedIds)

    ScreenScaffold(title = "Задания", onBack = onBack) {
        Column {
            Text(
                text = "За каждое задание дают монеты. Даже если ошибёшься, монеты всё равно дадут " +
                    "и подскажут, как было правильно.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SectionTitle("Новые задания")
            if (sections.fresh.isEmpty()) {
                SectionCard(title = "Новых заданий нет") {
                    Text(
                        // Не «все задания»: задание восстановления в очередь не
                        // входит и может оставаться нерешённым ниже на этом же экране.
                        text = "Новые задания закончились. Любое из тех, что уже решал, " +
                            "можно решить ещё раз — они ниже.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                sections.fresh.forEachIndexed { index, task ->
                    TaskCard(
                        task = task,
                        status = "Ещё не решал",
                        buttonText = "Начать",
                        // Следующее по очереди задание выделено основной
                        // кнопкой: с него продолжать, не выбирая.
                        primary = index == 0,
                        onOpen = { onOpenTask(task.id) },
                    )
                }
            }

            if (sections.solved.isNotEmpty()) {
                SectionTitle("Уже решал")
                sections.solved.forEach { task ->
                    TaskCard(
                        task = task,
                        status = repeatNote(task),
                        buttonText = "Решить ещё раз",
                        primary = false,
                        onOpen = { onOpenTask(task.id) },
                    )
                }
            }

            if (sections.recovery.isNotEmpty()) {
                SectionTitle("Если день не удался")
                Text(
                    text = "Это задание помогает питомцу, когда день прошёл неудачно. " +
                        "После такого дня кнопка на экране итогов откроет его сразу.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                sections.recovery.forEach { task ->
                    val done = task.id in completedIds
                    TaskCard(
                        task = task,
                        status = if (done) repeatNote(task) else "Ещё не решал",
                        buttonText = if (done) "Решить ещё раз" else "Начать",
                        primary = false,
                        onOpen = { onOpenTask(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

/** Карточка задания: название, тема, состояние и кнопка. */
@Composable
private fun TaskCard(
    task: TaskContent,
    status: String,
    buttonText: String,
    primary: Boolean,
    onOpen: () -> Unit,
) {
    SectionCard(title = task.title) {
        Column {
            Text(
                text = task.topic.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                // Отметка означает, что задание уже решали, а не что ответ
                // был верным: в решённые попадает любой ответ. Поэтому «уже
                // решал», а не «выполнено».
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (primary) {
                PrimaryButton(
                    text = buttonText,
                    onClick = onOpen,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                SecondaryButton(
                    text = buttonText,
                    onClick = onOpen,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/**
 * Пояснение к заданию, которое уже решали. Новые числа обещаются только
 * заданиям с переменными числами: у остальных условие при повторе то же.
 * Про повтор сказано прямо: награда половинная, чтобы повторение не стало
 * источником монет вместо учёбы.
 */
fun repeatNote(task: TaskContent): String =
    if (task.vary != null) {
        "Уже решал. Можно ещё раз с новыми числами — за повтор дают половину монет."
    } else {
        "Уже решал. Можно ещё раз — за повтор дают половину монет."
    }
