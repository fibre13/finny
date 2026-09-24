package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.TaskTopic
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard

/**
 * Список финансовых заданий по темам (ТЗ 2.5.8).
 * Все задания доступны сразу, без привязки к реальному времени.
 */
@Composable
fun TasksScreen(
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    onOpenTask: (String) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Задания", onBack = onBack) {
        Column {
            Text(
                text = "За каждое задание дают монеты. Даже если ошибёшься, монеты всё равно дадут " +
                    "и подскажут, как было правильно.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            TaskTopic.entries.forEach { topic ->
                val topicTasks = tasks.filter { it.topic == topic }
                if (topicTasks.isEmpty()) return@forEach

                Text(
                    text = topic.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )

                topicTasks.forEach { task ->
                    val done = task.id in completedIds
                    SectionCard(title = task.title) {
                        Column {
                            Text(
                                // Отметка означает, что задание уже решали,
                                // а не что ответ был верным: в пройденные
                                // попадает любой ответ. Поэтому «уже решал»,
                                // а не «выполнено». Про повтор сказано прямо:
                                // награда половинная, чтобы повторение не
                                // стало источником монет вместо учёбы. Новые
                                // числа обещаны только тем заданиям, где они
                                // действительно меняются.
                                text = if (done) repeatNote(task) else "Ещё не решал",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            SecondaryButton(
                                text = if (done) "Решить ещё раз" else "Начать",
                                onClick = { onOpenTask(task.id) },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Пояснение к заданию, которое уже решали. Новые числа обещаются только
 * заданиям с переменными числами: у остальных условие при повторе то же.
 */
fun repeatNote(task: TaskContent): String =
    if (task.vary != null) {
        "Уже решал. Можно ещё раз с новыми числами — за повтор дают половину монет."
    } else {
        "Уже решал. Можно ещё раз — за повтор дают половину монет."
    }
