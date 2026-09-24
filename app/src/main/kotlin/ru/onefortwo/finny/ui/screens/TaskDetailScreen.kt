package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.CoinStepper
import ru.onefortwo.finny.ui.common.ControlHeight
import ru.onefortwo.finny.ui.common.DigitPad
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SelectButton
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.TaskResultIcon
import ru.onefortwo.finny.ui.theme.FinnyTheme
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
    balance: Coins? = null,
) {
    var result by rememberSaveable(stateSaver = AnsweredTaskSaver) {
        mutableStateOf<AnsweredTask?>(null)
    }

    ScreenScaffold(
        eyebrow = task.topic.displayName,
        title = task.title,
        balance = balance,
        onBack = onBack,
    ) {
        Column {
            SectionCard(tone = CardTone.Sage) {
                Text(
                    text = task.prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
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
            tone = if (check.isCorrect) CardTone.Success else CardTone.Warning,
            icon = { TaskResultIcon(correct = check.isCorrect) },
        ) {
            Column {
                Text(
                    text = Explanations.reward(
                        source = check.reward,
                        amount = answered.credited,
                        repeat = answered.isRepeat,
                    ) + ".",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (check.isCorrect) {
                        FinnyTheme.colors.successText
                    } else {
                        FinnyTheme.colors.warningText
                    },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                if (check.outcome != null) {
                    Text(
                        text = check.outcome!!,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                SupportingText(check.explanation)
            }
        }
        if (nextTask != null) {
            SectionCard(eyebrow = "Следующее задание", title = nextTask.title) {
                SupportingText(nextTask.topic.displayName)
            }
            PrimaryButton(text = "Следующее задание: ${nextTask.title}", onClick = onNextTask)
            SecondaryButton(
                text = "Готово",
                onClick = onBack,
                modifier = Modifier.padding(top = 10.dp),
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
        TaskAmount("Нужное", needs, task.amount) { needs = it }
        TaskAmount("Хочу", wants, task.amount) { wants = it }
        TaskAmount("Копилка", savings, task.amount) { savings = it }

        SupportingText(
            text = if (left == 0) {
                "Все монеты распределены."
            } else if (left > 0) {
                "Осталось распределить ${Explanations.coinsAccusative(left)}."
            } else {
                "Ты раздал больше, чем есть, на ${Explanations.coinsAccusative(-left)}."
            },
            modifier = Modifier.padding(vertical = 12.dp),
        )

        PrimaryButton(
            text = "Ответить",
            enabled = left == 0,
            onClick = { onSubmit(TaskAnswer.Allocation(needs, wants, savings)) },
        )
    }
}

/**
 * Сумма одного направления в задании на распределение: подпись с числом
 * и кнопки шага. Число объявляется программой чтения с экрана при каждом
 * изменении.
 */
@Composable
private fun TaskAmount(label: String, value: Int, max: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "$label: ${Explanations.coins(value)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        CoinStepper(
            label = label,
            value = value,
            max = max,
            onChange = onChange,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Отметка подходящих элементов списка. */
@Composable
private fun PickForm(task: PickTask, onSubmit: (TaskAnswer) -> Unit) {
    val picked = rememberSaveable { mutableStateListOf<String>() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val colors = FinnyTheme.colors
        val shape = MaterialTheme.shapes.small
        task.options.forEach { option ->
            val checked = option.id in picked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxOf(MinTouchTarget, ControlHeight))
                    .clip(shape)
                    .background(if (checked) colors.selectedContainer else colors.surface)
                    .border(
                        width = if (checked) 2.dp else 1.5.dp,
                        color = if (checked) colors.primary else colors.outline,
                        shape = shape,
                    )
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Обработчик снят: нажатие обрабатывает строка целиком,
                // иначе программа чтения с экрана объявит два элемента.
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.primary,
                    ),
                )
                Text(
                    text = if (option.price != null) {
                        "${option.title} — ${Explanations.coins(option.price!!)}"
                    } else {
                        option.title
                    },
                    style = MaterialTheme.typography.titleMedium,
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

/**
 * Ввод числового ответа экранной цифровой панелью.
 *
 * Ответ ребёнок набирает сам, поэтому задание остаётся вычислением, а не
 * выбором из готовых вариантов (ТЗ 2.5.8). Системная клавиатура не нужна:
 * не приходится попадать в поле, а клавиши крупные на любом устройстве.
 * Ответы в заданиях не превышают сотни, поэтому набирается до трёх цифр.
 */
@Composable
private fun NumberForm(task: NumberTask, onSubmit: (TaskAnswer) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.small

    Column {
        Text("Ответ, ${task.unit}:", style = MaterialTheme.typography.titleMedium)
        // Набранное число стоит в рамке поля: видно, куда идёт ввод.
        Text(
            text = text.ifEmpty { "—" },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clip(shape)
                .background(colors.surface)
                .border(2.dp, colors.primary, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    if (text.isEmpty()) contentDescription = "Ответ ещё не набран"
                },
        )

        DigitPad(
            onDigit = { digit ->
                // Ведущий ноль не копится: «0», затем «7» дают «7».
                text = if (text == "0") "$digit" else (text + digit).take(MAX_ANSWER_DIGITS)
            },
            onErase = { text = text.dropLast(1) },
            eraseEnabled = text.isNotEmpty(),
        )

        PrimaryButton(
            text = "Ответить",
            enabled = text.toIntOrNull() != null,
            onClick = { text.toIntOrNull()?.let { onSubmit(TaskAnswer.Number(it)) } },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private const val MAX_ANSWER_DIGITS = 3

/** Выбор решения с описанными последствиями. */
@Composable
private fun ChoiceForm(task: ChoiceTask, onSubmit: (TaskAnswer) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        task.options.forEach { option ->
            SelectButton(
                text = option.title,
                selected = selected == option.id,
                onClick = { selected = option.id },
            )
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
