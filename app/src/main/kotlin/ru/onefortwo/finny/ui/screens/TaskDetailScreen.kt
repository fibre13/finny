package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.AllocateTask
import ru.onefortwo.finny.content.ChoiceTask
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PickTask
import ru.onefortwo.finny.content.PlanCategory
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.TaskCheck
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.IncomeSource
import ru.onefortwo.finny.ui.common.CoinSlider
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.TaskResultIcon
import ru.onefortwo.finny.ui.state.AnsweredTask
import ru.onefortwo.finny.ui.state.Explanations

/**
 * Прохождение задания (ТЗ 2.5.8).
 *
 * Вид взаимодействия определяется типом задания: распределение суммы,
 * отметка в списке, ввод числа или выбор решения с последствиями.
 * После ответа объяснение выдаётся независимо от правильности.
 */
@Composable
fun TaskDetailScreen(
    task: TaskContent,
    onAnswer: (TaskAnswer) -> AnsweredTask,
    onBack: () -> Unit,
    /**
     * Следующее новое задание, которое можно открыть сразу после ответа,
     * либо `null`: новых не осталось или время на сегодня вышло.
     */
    nextTask: TaskContent? = null,
    onNextTask: () -> Unit = {},
) {
    var result by rememberSaveable(stateSaver = AnsweredTaskSaver) {
        mutableStateOf<AnsweredTask?>(null)
    }

    ScreenScaffold(title = task.title, onBack = onBack) {
        Column {
            SectionCard(title = task.topic.displayName) {
                Text(task.prompt, style = MaterialTheme.typography.bodyMedium)
            }

            val current = result
            if (current == null) {
                when (task) {
                    is AllocateTask -> AllocateForm(task) { result = onAnswer(it) }
                    is PickTask -> PickForm(task) { result = onAnswer(it) }
                    is NumberTask -> NumberForm(task) { result = onAnswer(it) }
                    is ChoiceTask -> ChoiceForm(task) { result = onAnswer(it) }
                }
            } else {
                ResultCard(
                    answered = current,
                    nextTask = nextTask,
                    onNextTask = onNextTask,
                    onBack = onBack,
                )
            }
        }
    }
}

/**
 * Итог задания: результат, последствие, объяснение и награда.
 *
 * Сумма берётся фактически начисленная, а не полная награда источника:
 * за повтор начисляется половина, и на карточке должна стоять та же сумма,
 * на которую изменился баланс (ТЗ 2.5.4).
 *
 * Если есть новое задание, оно предлагается сразу основной кнопкой: после
 * ответа ребёнок продолжает, а не ищет следующее задание в списке.
 */
@Composable
private fun ResultCard(
    answered: AnsweredTask,
    nextTask: TaskContent?,
    onNextTask: () -> Unit,
    onBack: () -> Unit,
) {
    val check = answered.check
    Column {
        SectionCard(
            title = if (check.isCorrect) "Верно" else "Почти",
            icon = { TaskResultIcon(correct = check.isCorrect) },
        ) {
            Column {
                if (check.outcome != null) {
                    Text(
                        text = check.outcome!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(check.explanation, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = Explanations.reward(
                        source = check.reward,
                        amount = answered.credited,
                        repeat = answered.isRepeat,
                    ) + ".",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (nextTask != null) {
            PrimaryButton(text = "Следующее задание: ${nextTask.title}", onClick = onNextTask)
            SecondaryButton(
                text = "Готово",
                onClick = onBack,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            PrimaryButton(text = "Готово", onClick = onBack)
        }
    }
}

/** Распределение суммы по трём направлениям. */
@Composable
private fun AllocateForm(task: AllocateTask, onSubmit: (TaskAnswer) -> Unit) {
    var needs by rememberSaveable { mutableIntStateOf(0) }
    var wants by rememberSaveable { mutableIntStateOf(0) }
    var savings by rememberSaveable { mutableIntStateOf(0) }

    val left = task.amount - (needs + wants + savings)

    Column {
        TaskSlider("Нужное", needs, task.amount) { needs = it }
        TaskSlider("Хочу", wants, task.amount) { wants = it }
        TaskSlider("Копилка", savings, task.amount) { savings = it }

        Text(
            text = if (left == 0) {
                "Все монеты распределены."
            } else if (left > 0) {
                "Осталось распределить ${Explanations.coins(left)}."
            } else {
                "Ты раздал больше, чем есть, на ${Explanations.coins(-left)}."
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        PrimaryButton(
            text = "Ответить",
            enabled = left == 0,
            onClick = { onSubmit(TaskAnswer.Allocation(needs, wants, savings)) },
        )
    }
}

@Composable
private fun TaskSlider(label: String, value: Int, max: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ${Explanations.coins(value)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        CoinSlider(
            value = value,
            max = max,
            // Направления здесь названы подписью над дорожкой, но сами
            // карточки направлений на этом экране не показываются:
            // цвет берётся основной, а не по направлению.
            color = MaterialTheme.colorScheme.primary,
            onChange = onChange,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Отметка подходящих элементов списка. */
@Composable
private fun PickForm(task: PickTask, onSubmit: (TaskAnswer) -> Unit) {
    val picked = rememberSaveable { mutableStateListOf<String>() }

    Column {
        task.options.forEach { option ->
            val checked = option.id in picked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    // Нажатие принимает вся строка, а не только квадратик:
                    // попасть по нему пальцем на телефоне трудно, а подпись
                    // рядом выглядит частью того же переключателя.
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = {
                            if (checked) picked.remove(option.id) else picked.add(option.id)
                        },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Обработчик снят: нажатие обрабатывает строка целиком,
                // иначе программа чтения с экрана объявит два элемента.
                Checkbox(checked = checked, onCheckedChange = null)
                Text(
                    text = if (option.price != null) {
                        "${option.title} — ${Explanations.coins(option.price!!)}"
                    } else {
                        option.title
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        PrimaryButton(
            text = "Ответить",
            enabled = picked.isNotEmpty(),
            onClick = { onSubmit(TaskAnswer.Picked(picked.toSet())) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** Ввод числового ответа. */
@Composable
private fun NumberForm(task: NumberTask, onSubmit: (TaskAnswer) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { input -> text = input.filter { it.isDigit() }.take(4) },
            label = { Text("Ответ, ${task.unit}", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = "Ответить",
            enabled = text.toIntOrNull() != null,
            onClick = { text.toIntOrNull()?.let { onSubmit(TaskAnswer.Number(it)) } },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** Выбор решения с описанными последствиями. */
@Composable
private fun ChoiceForm(task: ChoiceTask, onSubmit: (TaskAnswer) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    Column {
        task.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    .selectable(
                        selected = selected == option.id,
                        role = Role.RadioButton,
                        onClick = { selected = option.id },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option.id, onClick = null)
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        PrimaryButton(
            text = "Ответить",
            enabled = selected != null,
            onClick = { selected?.let { onSubmit(TaskAnswer.Chosen(it)) } },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * Сохранение результата задания при повороте экрана.
 *
 * [AnsweredTask] состоит из простых значений, поэтому раскладывается в
 * список: признак наличия результата, правильность, объяснение, последствие
 * (пустая строка вместо null), название источника награды, начисленная
 * сумма и признак повтора.
 */
private val AnsweredTaskSaver: Saver<AnsweredTask?, Any> = listSaver(
    save = { answered ->
        if (answered == null) {
            listOf(false)
        } else {
            val check = answered.check
            listOf(
                true,
                check.isCorrect,
                check.explanation,
                check.outcome ?: "",
                check.reward.name,
                answered.credited.amount,
                answered.isRepeat,
            )
        }
    },
    restore = { items ->
        if (items.first() == false) {
            null
        } else {
            AnsweredTask(
                check = TaskCheck(
                    isCorrect = items[1] as Boolean,
                    explanation = items[2] as String,
                    outcome = (items[3] as String).ifEmpty { null },
                    reward = IncomeSource.valueOf(items[4] as String),
                ),
                credited = Coins(items[5] as Int),
                isRepeat = items[6] as Boolean,
            )
        }
    },
)

/** Направление плана в задании на распределение. */
private fun PlanCategory.title(): String = when (this) {
    PlanCategory.NEEDS -> "Нужное"
    PlanCategory.WANTS -> "Хочу"
    PlanCategory.SAVINGS -> "Копилка"
}
