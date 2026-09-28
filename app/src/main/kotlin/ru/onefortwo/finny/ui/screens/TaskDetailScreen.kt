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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import ru.onefortwo.finny.content.ClockSpec
import ru.onefortwo.finny.content.CoinsSpec
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.ui.common.rememberPixelArt
import kotlin.math.cos
import kotlin.math.sin

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
    @Suppress("UNUSED_PARAMETER") balance: Coins? = null,
    /** ТЕСТ: питомец ребёнка для реакции на ответ; `true` — радуется. */
    petFigure: (@Composable (happy: Boolean) -> Unit)? = null,
) {
    var result by rememberSaveable(stateSaver = AnsweredTaskSaver) {
        mutableStateOf<AnsweredTask?>(null)
    }

    ScreenScaffold(
        eyebrow = task.topic.displayName,
        title = task.title,
        onBack = onBack,
    ) {
        Column {
            // ТЕСТ: игровое событие над условием — зачем решать задание.
            task.context?.let {
                SectionCard(tone = CardTone.Coin) {
                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                }
            }
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
                    petFigure = petFigure,
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
    petFigure: (@Composable (happy: Boolean) -> Unit)?,
) {
    val check = answered.check
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    Column {
        SectionCard(
            // ТЕСТ: мягче — «Почти получилось!», а не «Почти».
            title = if (check.isCorrect) "Верно!" else "Почти получилось!",
            tone = if (check.isCorrect) CardTone.Success else CardTone.Warning,
            icon = { TaskResultIcon(correct = check.isCorrect) },
        ) {
            Column {
                if (check.outcome != null) {
                    Text(
                        text = check.outcome!!,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text(text = check.explanation, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = Explanations.reward(
                        source = check.reward,
                        amount = answered.credited,
                        repeat = answered.isRepeat,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinnyTheme.colors.onSurfaceMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // ТЕСТ: питомец отвечает облачком — радуется или поддерживает.
        if (petFigure != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpeechBubble(colors = colors, text = check.pet)
                Row(verticalAlignment = Alignment.Bottom) {
                    petFigure(check.isCorrect)
                    check.petIcon?.let { icon ->
                        Spacer(modifier = Modifier.width(8.dp))
                        TaskSprite(art, icon, 56.dp)
                    }
                }
            }
        }

        // После ответа — одна карточка следующего задания и выход.
        if (nextTask != null) {
            NextTaskCard(art = art, task = nextTask, onStart = onNextTask)
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

/** ТЕСТ: карточка следующего задания с короткой кнопкой «Начать →». */
@Composable
private fun NextTaskCard(art: PixelArt, task: TaskContent, onStart: () -> Unit) {
    SectionCard(eyebrow = "Следующее задание", bottomSpacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            task.icon?.let {
                TaskSprite(art, it, 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                SupportingText(task.topic.displayName)
            }
            Spacer(modifier = Modifier.width(8.dp))
            PrimaryButton(text = "Начать →", onClick = onStart, modifier = Modifier.width(132.dp))
        }
    }
}

/** ТЕСТ: картинка из `art/pixel`, вписанная в квадрат [box]; декоративная. */
@Composable
internal fun TaskSprite(art: PixelArt, id: String, box: Dp) {
    val sprite = art.sprite(id) ?: return
    val cell = box / maxOf(sprite.width, sprite.height)
    Box(modifier = Modifier.size(box), contentAlignment = Alignment.Center) {
        Sprite(art, id, cell = cell)
    }
}

/** ТЕСТ: подсказка мелким шрифтом под вариантами: «Сравни цену с планом». */
@Composable
private fun TaskHint(text: String?) {
    if (text != null) {
        SupportingText("Подсказка: $text", modifier = Modifier.padding(top = 10.dp))
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
        // ТЕСТ: «+» останавливается, когда монеты кончились: раздать больше,
        // чем есть, нельзя — и сообщения о переборе не нужно.
        TaskAmount(planLabel(PlanCategory.NEEDS), needs, needs + left) { needs = it }
        TaskAmount(planLabel(PlanCategory.WANTS), wants, wants + left) { wants = it }
        TaskAmount(planLabel(PlanCategory.SAVINGS), savings, savings + left) { savings = it }

        SupportingText(
            text = if (left == 0) {
                "Все монеты распределены."
            } else {
                "Осталось распределить ${Explanations.coinsAccusative(left)}."
            },
            modifier = Modifier.padding(vertical = 12.dp),
        )

        PrimaryButton(
            text = "Ответить",
            enabled = left == 0,
            onClick = { onSubmit(TaskAnswer.Allocation(needs, wants, savings)) },
        )
        if (left > 0) TaskHint(task.hint)
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

/**
 * ТЕСТ: выбор крупными карточками с картинками вместо галочек. Если верный
 * ответ один — выбирается одна карточка; иначе карточки отмечаются.
 */
@Composable
private fun PickForm(task: PickTask, onSubmit: (TaskAnswer) -> Unit) {
    val picked = rememberSaveable { mutableStateListOf<String>() }
    val single = task.correct.size == 1
    val art = rememberPixelArt()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        task.options.forEach { option ->
            val checked = option.id in picked
            OptionCard(
                art = art,
                title = if (option.price != null) {
                    "${option.title} — ${Explanations.coins(option.price!!)}"
                } else {
                    option.title
                },
                note = option.note,
                icon = option.icon,
                selected = checked,
                onClick = {
                    if (single) {
                        picked.clear()
                        picked.add(option.id)
                    } else if (checked) {
                        picked.remove(option.id)
                    } else {
                        picked.add(option.id)
                    }
                },
            )
        }
        TaskHint(task.hint)

        PrimaryButton(
            text = "Ответить",
            enabled = picked.isNotEmpty(),
            onClick = { onSubmit(TaskAnswer.Picked(picked.toSet())) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** ТЕСТ: крупная карточка варианта: картинка, название, пометка. */
@Composable
private fun OptionCard(
    art: PixelArt,
    title: String,
    note: String?,
    icon: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectButton(
        text = title,
        supporting = note,
        selected = selected,
        onClick = onClick,
        leading = icon?.let { { TaskSprite(art, it, 48.dp) } },
    )
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

    val clock = task.clock
    // ТЕСТ: если на циферблате есть варианты — ответ выбирается на часах.
    if (clock != null && clock.choices.isNotEmpty()) {
        var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
        Column {
            Text("Выбери на часах:", style = MaterialTheme.typography.titleMedium)
            ClockFace(
                spec = clock,
                selected = chosen,
                onSelect = { chosen = it },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp),
            )
            TaskHint(task.hint)
            PrimaryButton(
                text = "Ответить",
                enabled = chosen != null,
                onClick = { chosen?.let { onSubmit(TaskAnswer.Number(it)) } },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        return
    }

    Column {
        if (clock != null) {
            ClockFace(
                spec = clock,
                selected = null,
                onSelect = {},
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
            )
        }
        task.coins?.let { CoinsPicture(it) }
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

    val art = rememberPixelArt()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        task.options.forEach { option ->
            OptionCard(
                art = art,
                title = option.title,
                note = option.note,
                icon = option.icon,
                selected = selected == option.id,
                onClick = { selected = option.id },
            )
        }
        TaskHint(task.hint)

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
                check.pet,
                check.petIcon ?: "",
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
                    pet = items[7] as String,
                    petIcon = (items[8] as String).ifEmpty { null },
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
    PlanCategory.WANTS -> "Развлечения"
    PlanCategory.SAVINGS -> "Копилка"
}

/** Подпись направления плана в задании: как в плане дня. */
private fun planLabel(category: PlanCategory): String = when (category) {
    PlanCategory.NEEDS -> BudgetCategory.NEEDS.displayName
    PlanCategory.WANTS -> BudgetCategory.WANTS.displayName
    PlanCategory.SAVINGS -> BudgetCategory.SAVINGS.displayName
}

/**
 * ТЕСТ: циферблат. Стрелка показывает «сейчас» ([ClockSpec.from]); отрезок
 * до [ClockSpec.to] — сколько ждать. Если заданы варианты ответа, эти часы
 * нажимаются (круги 48 dp), остальные — нет.
 */
@Composable
private fun ClockFace(spec: ClockSpec, selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = FinnyTheme.colors
    val until = selected ?: spec.to.takeIf { spec.choices.isEmpty() }
    val size = 248.dp
    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = spec.choices.isEmpty()) {
                if (spec.choices.isEmpty()) {
                    contentDescription = "Часы: сейчас ${spec.from}:00" + (until?.let { ", отмечено до $it:00" } ?: "")
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = Offset(r, r)
            drawCircle(colors.surface, r - 4.dp.toPx(), c)
            drawCircle(colors.outline, r - 4.dp.toPx(), c, style = Stroke(3.dp.toPx()))
            if (until != null) {
                val start = spec.from % 12 * 30f - 90f
                val sweep = ((until - spec.from + 12) % 12) * 30f
                drawArc(
                    color = colors.coin.copy(alpha = 0.55f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f),
                    size = Size(r * 1.24f, r * 1.24f),
                )
            }
            fun hand(hour: Float, length: Float, width: Float, color: Color) {
                val a = Math.toRadians((hour % 12 * 30f - 90f).toDouble())
                drawLine(
                    color,
                    c,
                    Offset(c.x + (cos(a) * r * length).toFloat(), c.y + (sin(a) * r * length).toFloat()),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )
            }
            hand(spec.from.toFloat(), 0.5f, 6.dp.toPx(), colors.onSurface)
            hand(0f, 0.72f, 3.dp.toPx(), colors.onSurface)
            drawCircle(colors.onSurface, 5.dp.toPx(), c)
        }
        for (h in 1..12) {
            val a = Math.toRadians((h * 30f - 90f).toDouble())
            val dist = size / 2 - 28.dp
            val clickable = h in spec.choices
            val isSelected = h == selected
            Box(
                modifier = Modifier
                    .offset(
                        x = size / 2 - 24.dp + dist * cos(a).toFloat(),
                        y = size / 2 - 24.dp + dist * sin(a).toFloat(),
                    )
                    .size(48.dp)
                    .then(
                        if (clickable) {
                            Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) colors.selectedContainer else colors.surface)
                                .border(if (isSelected) 3.dp else 2.dp, colors.primary, CircleShape)
                                .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(h) })
                                .semantics { contentDescription = "$h:00" }
                        } else {
                            Modifier.clearAndSetSemantics { }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$h",
                    style = if (clickable) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                    color = if (clickable) colors.onSurface else colors.onSurfaceMuted,
                    fontWeight = if (clickable) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** ТЕСТ: монеты рисунком: сколько стоит покупка и сколько дали. */
@Composable
private fun CoinsPicture(spec: CoinsSpec) {
    val art = rememberPixelArt()
    Column(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Цена — ${Explanations.coins(spec.price)}. Ты дал ${Explanations.coins(spec.paid)}."
            },
    ) {
        CoinsRow(art, "Цена", spec.price)
        CoinsRow(art, "Ты дал", spec.paid)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoinsRow(art: PixelArt, label: String, count: Int) {
    Column(modifier = Modifier.padding(vertical = 4.dp).clearAndSetSemantics { }) {
        Text(text = "$label:", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count) { TaskSprite(art, "coin_0", 24.dp) }
        }
    }
}
