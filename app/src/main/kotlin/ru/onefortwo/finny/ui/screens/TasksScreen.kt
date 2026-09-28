package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.TaskQueue
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Список финансовых заданий (ТЗ 2.5.8). Открывается вкладкой «Задания».
 *
 * Все задания доступны сразу, без привязки к реальному времени. Порядок
 * задаёт [TaskQueue]: сверху новые задания в порядке выдачи, первое из них
 * выделено основной кнопкой; ниже — уже решённые, их можно решить ещё раз;
 * в конце — задание восстановления, которое пригождается после неудачного
 * дня. Тема каждого задания подписана на его карточке.
 *
 * @param onBack возврат; `null`, когда экран открыт вкладкой.
 */
@Composable
fun TasksScreen(
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    onOpenTask: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    balance: Coins? = null,
) {
    val sections = TaskQueue.sections(tasks, completedIds)
    // Счёт тот же, что на вкладке «Прогресс»: все задания, включая
    // задание восстановления.
    val total = tasks.size
    val solved = tasks.count { it.id in completedIds }

    ScreenScaffold(
        eyebrow = "Учимся с монетами",
        title = "Задания",
        balance = balance,
        onBack = onBack,
    ) {
        Column {
            SectionCard(
                title = "Решено",
                tone = CardTone.Primary,
                trailing = {
                    Text(
                        text = "$solved из $total",
                        style = MaterialTheme.typography.titleMedium,
                        color = FinnyTheme.colors.coin,
                    )
                },
            ) {
                Column {
                    Text(
                        // ТЕСТ: монеты — за первые два задания дня.
                        text = "За первые два задания дня дают монеты — даже если ошибёшься. " +
                            "Остальные можно решать просто для тренировки.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ProgressBar(
                        fraction = if (total > 0) solved.toFloat() / total else 0f,
                        color = FinnyTheme.colors.coin,
                        trackColor = FinnyTheme.colors.onPrimary.copy(alpha = 0.22f),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            SectionTitle("Новые задания")
            if (sections.fresh.isEmpty()) {
                SectionCard(title = "Новых заданий нет", tone = CardTone.Sage) {
                    SupportingText(
                        // Не «все задания»: задание восстановления в очередь не
                        // входит и может оставаться нерешённым ниже на этом же экране.
                        text = "Новые задания закончились. Любое из тех, что уже решал, " +
                            "можно решить ещё раз — они ниже.",
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
                SupportingText(
                    text = "Это задание помогает питомцу, когда день прошёл неудачно. " +
                        "После такого дня кнопка на экране итогов откроет его сразу.",
                    modifier = Modifier.padding(bottom = 10.dp),
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
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .semantics { heading() },
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
            SupportingText(task.topic.displayName)
            Text(
                // Отметка означает, что задание уже решали, а не что ответ
                // был верным: в решённые попадает любой ответ. Поэтому «уже
                // решал», а не «выполнено».
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (primary) {
                PrimaryButton(
                    text = buttonText,
                    onClick = onOpen,
                    modifier = Modifier.padding(top = 14.dp),
                )
            } else {
                SecondaryButton(
                    text = buttonText,
                    onClick = onOpen,
                    modifier = Modifier.padding(top = 14.dp),
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
