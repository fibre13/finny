package ru.onefortwo.finny.ui.screens

import ru.onefortwo.finny.content.Accessories
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.ChevronIcon
import ru.onefortwo.finny.ui.common.fillRemaining
import ru.onefortwo.finny.ui.common.Eyebrow
import ru.onefortwo.finny.ui.common.HeaderIconButton
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.LineGlyph
import ru.onefortwo.finny.ui.common.LineIcon
import ru.onefortwo.finny.ui.common.LocalMutedColor
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.rememberPulse
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SceneStats
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Промежуток между блоками главного экрана. */
private val BlockGap = 8.dp

/**
 * Главный экран (ТЗ 2.5.3): питомец, баланс, накопления, текущая цель,
 * показатели состояния и активное задание видны одновременно.
 *
 * Экран помещается целиком, без прокрутки, на телефоне 360×640 dp при
 * обычном размере шрифта (макет Figma «Главная — один экран», 42:2 и
 * состояния под ним). Разделы открываются значками сверху, как вкладки
 * снизу; «Как играть» и «Для взрослого» — кнопками в шапке. Сцена с
 * питомцем забирает оставшуюся высоту и видна целиком: увеличение
 * подбирается под место, «Забота» и «Радость» — панелью в небе сцены.
 * При крупном шрифте экран прокручивается, ничего не обрезается.
 *
 * Состояния не добавляют блоков: демонстрационный режим отмечен в шапке,
 * время — в карточке задания, прожитый день — причиной у «Закончить день».
 */
@Composable
fun MainScreen(
    state: AppState,
    parts: PetPartsContent,
    activeTask: TaskContent?,
    goalTitle: String?,
    onDismissMessage: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAdult: () -> Unit,
    onFinishPeriod: () -> Unit,
    today: String,
    /** Открыть задание сразу, минуя список. */
    onOpenTask: (String) -> Unit = {},
    /**
     * Реакция питомца на последнее событие игры: покупка, копилка,
     * награда, рост. Проигрывается при возвращении на главный экран.
     */
    reaction: PetReaction? = null,
    onReactionPlayed: (Long) -> Unit = {},
    /** Нажатие на сцену открывает вкладку «Питомец». */
    onOpenPet: () -> Unit = {},
) {
    val profile = state.profile ?: return
    val game = state.game
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }

    ScreenScaffold(
        eyebrow = "День ${game.period.number}" + if (state.isDemo) " · демо" else "",
        title = profile.petName,
        balance = game.balance,
        onBack = null,
        message = state.message,
        onDismissMessage = onDismissMessage,
        headerActions = {
            HeaderIconButton(LineGlyph.HELP, "Как играть", onOpenHelp)
            HeaderIconButton(LineGlyph.ADULT, "Для взрослого", onOpenAdult)
        },
        compactHeader = true,
        fillViewport = true,
        bottomPadding = 8.dp,
        singleLineTitle = true,
    ) {
        SectionRow(
            planMissing = !game.period.isPlanConfirmed,
            onOpenPlan = onOpenPlan,
            onOpenShop = onOpenShop,
            onOpenSavings = onOpenSavings,
            onOpenGlossary = onOpenGlossary,
        )
        Spacer(modifier = Modifier.height(BlockGap))

        val careLabel = Explanations.statLabel(PetStatKind.CARE, game.pet.care)
        val joyLabel = Explanations.statLabel(PetStatKind.JOY, game.pet.joy)
        SceneCard(onOpenPet = onOpenPet) {
            PetFigure(
                petName = profile.petName,
                speciesId = profile.appearance.speciesId,
                speciesTitle = species?.title ?: "Питомец",
                accessoryId = profile.appearance.accessoryId,
                accessoryTitle = Accessories.title(parts, profile.appearance.accessoryId),
                colorHex = color?.hex ?: "#CCCCCC",
                stage = game.stage,
                care = game.pet.care.level,
                joy = game.pet.joy.level,
                scene = true,
                fillArea = true,
                house = state.hasScenery("house"),
                stickers = state.hasScenery("stickers"),
                // В сцене одно место под предмет цели: последняя полученная.
                goalId = state.achievedGoalIds.lastOrNull(),
                caption = false,
                reaction = reaction,
                onReactionEnd = onReactionPlayed,
                stats = SceneStats(
                    care = game.pet.care.value,
                    careLow = game.pet.care.level == StatLevel.LOW,
                    joy = game.pet.joy.value,
                    joyLow = game.pet.joy.level == StatLevel.LOW,
                ),
                description = "${profile.petName} дома. " +
                    "${Explanations.statName(PetStatKind.CARE)}: $careLabel, ${game.pet.care.value} из 100. " +
                    "${Explanations.statName(PetStatKind.JOY)}: $joyLabel, ${game.pet.joy.value} из 100.",
            )
        }
        Spacer(modifier = Modifier.height(BlockGap))

        MoneyCard(state = state, goalTitle = goalTitle)
        Spacer(modifier = Modifier.height(BlockGap))

        TaskCard(state = state, today = today, activeTask = activeTask, onOpenTask = onOpenTask)
        Spacer(modifier = Modifier.height(BlockGap))

        val dayFinished = state.isDayFinished(today)
        // Причина — коротко, не длиннее двух строк рядом с кнопкой: для
        // неактивной кнопки она обязательна текстом, а не только серым
        // цветом (ТЗ 3.6).
        val reason = when {
            dayFinished -> "Закончить день можно завтра."
            !game.period.isPlanConfirmed -> "Сначала составь план."
            !game.period.canFinish -> "Сначала купи или отложи монеты."
            else -> null
        }
        FinishDayRow(reason = reason, onFinish = onFinishPeriod)
    }
}

/**
 * Ряд разделов: четыре значка с подписями, как вкладки снизу. Нажимается
 * весь столбец. При крупном шрифте ряд перестраивается в сетку 2 × 2,
 * подписи не обрезаются.
 */
@Composable
private fun SectionRow(
    planMissing: Boolean,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenGlossary: () -> Unit,
) {
    val items = listOf(
        Triple("План", LineGlyph.PLAN, onOpenPlan),
        Triple("Покупки", LineGlyph.SHOP, onOpenShop),
        Triple("Копилка", LineGlyph.SAVINGS, onOpenSavings),
        Triple("Словарик", LineGlyph.GLOSSARY, onOpenGlossary),
    )
    AdaptiveGrid(count = items.size, minItemWidth = 72.dp, spacing = 0.dp) { index ->
        val (label, glyph, onClick) = items[index]
        SectionCell(
            label = label,
            glyph = glyph,
            badge = index == 0 && planMissing,
            onClick = onClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionCell(
    label: String,
    glyph: LineGlyph,
    badge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinnyTheme.colors
    val tileShape = MaterialTheme.shapes.small

    Column(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                // Отметка «!» передаётся словами, а не только знаком.
                if (badge) stateDescription = "не составлен"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .softShadow(tileShape, lift = false)
                    .clip(tileShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                LineIcon(glyph, colors.attention)
            }
            if (badge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .border(2.dp, colors.surface, CircleShape)
                        .clip(CircleShape)
                        .background(colors.attentionText)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("!", style = MaterialTheme.typography.labelMedium, color = colors.surface)
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Сцена питомца на главном экране: забирает свободную высоту, сцена
 * видна целиком при любой высоте. «Забота» и «Радость» — панелью в небе
 * сцены. Нажатие открывает вкладку «Питомец».
 */
@Composable
private fun SceneCard(onOpenPet: () -> Unit, content: @Composable () -> Unit) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Box(
        modifier = Modifier
            .fillRemaining(if (landscape) SceneMinHeightLandscape else SceneMinHeight)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClickLabel = "Открыть питомца", onClick = onOpenPet),
    ) {
        content()
    }
}

/** Наименьшая высота сцены: при меньшей экран прокручивается. */
private val SceneMinHeight = 72.dp

/**
 * Наименьшая высота сцены в альбомной ориентации. На телефоне высота
 * окна около 360 dp, и сцене доставалось 72 dp — питомец выходил
 * крохотным. 190 dp хватает на сцену вдвое крупнее (93 строки после
 * среза неба × 2); ниже экран прокручивается.
 */
private val SceneMinHeightLandscape = 190.dp

/** Монеты и цель на тёмной карточке. */
@Composable
private fun MoneyCard(state: AppState, goalTitle: String?) {
    val colors = FinnyTheme.colors
    val game = state.game
    val light = colors.onPrimary
    val muted = colors.onPrimary.copy(alpha = 0.82f)
    val largeFont = LocalDensity.current.fontScale >= 1.3f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.primary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides light,
            LocalMutedColor provides muted,
        ) {
            if (largeFont) {
                LabeledValue("Можно потратить", Explanations.coins(game.balance))
                LabeledValue("В копилке", Explanations.coins(game.savings.saved))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyColumn("Можно потратить", Explanations.coins(game.balance), muted, Modifier.weight(1.4f))
                    MoneyColumn("В копилке", Explanations.coins(game.savings.saved), muted, Modifier.weight(1f))
                }
            }

            val goal = game.savings.goal
            if (goal == null || goalTitle == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(LineGlyph.GOAL, muted, size = 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Цель пока не выбрана.", style = MaterialTheme.typography.bodyMedium, color = muted)
                }
            } else {
                val saved = game.savings.saved.amount
                val price = goal.price.amount
                val reached = saved >= price
                val counter = "$saved из $price" + if (reached) " ✓" else ""
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Цель: $goalTitle, накоплено $saved из $price" +
                            if (reached) ", цель накоплена" else ""
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineIcon(LineGlyph.GOAL, muted, size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = goalTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = counter,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.coin,
                            softWrap = false,
                        )
                    }
                    ProgressBar(
                        fraction = saved.toFloat() / price,
                        color = colors.coin,
                        trackColor = colors.onPrimary.copy(alpha = 0.22f),
                        height = 8.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyColumn(label: String, value: String, muted: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Карточка задания. Надзаголовок по приоритету: время заканчивается →
 * повтор → следующее задание. Когда время вышло, задание не предлагается
 * (ТЗ 8.1): карточка становится предупреждением без перехода.
 */
@Composable
private fun TaskCard(
    state: AppState,
    today: String,
    activeTask: TaskContent?,
    onOpenTask: (String) -> Unit,
) {
    val colors = FinnyTheme.colors
    val timeUp = state.isTimeUp(today)
    val isRepeat = activeTask != null && activeTask.id in state.completedTaskIds
    val clickable = !timeUp && activeTask != null

    val (glyph, eyebrow, accent) = when {
        timeUp -> Triple(LineGlyph.CLOCK, "! На сегодня хватит", colors.warningText)
        activeTask == null -> Triple(LineGlyph.NEXT_TASK, "Задания", colors.attentionText)
        state.isTimeRunningOut(today) -> Triple(
            LineGlyph.CLOCK,
            "! Осталось ${Explanations.minutes(state.minutesLeft(today))}",
            colors.warningText,
        )
        isRepeat -> Triple(LineGlyph.NEXT_TASK, "Повтор · половина монет", colors.attentionText)
        else -> Triple(LineGlyph.NEXT_TASK, "Следующее задание", colors.attentionText)
    }
    val title = when {
        timeUp -> "Прогресс сохранён. Приходи завтра!"
        activeTask == null -> "Заданий пока нет."
        else -> activeTask.title
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (timeUp) colors.warningContainer else colors.coinContainer)
            .then(
                if (clickable) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = if (isRepeat) "Решить ещё раз" else "Начать задание",
                        onClick = { onOpenTask(activeTask!!.id) },
                    )
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { }
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon(glyph, accent, size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Eyebrow(eyebrow, color = accent)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (clickable) {
            Spacer(modifier = Modifier.width(8.dp))
            ChevronIcon(color = colors.onSurface)
        }
    }
}

/**
 * «Закончить день». Неактивная — по ширине подписи, причина справа не
 * длиннее двух строк; при крупном шрифте причина уходит под кнопку.
 */
@Composable
private fun FinishDayRow(reason: String?, onFinish: () -> Unit) {
    if (reason == null) {
        // Всё для дня сделано — кнопка мягко пульсирует, подсказывая шаг.
        val pulse = rememberPulse()
        PrimaryButton(
            text = "Закончить день",
            onClick = onFinish,
            modifier = Modifier.graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
        )
        return
    }
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    if (largeFont) {
        PrimaryButton(text = "Закончить день", onClick = onFinish, enabled = false)
        SupportingText(reason, modifier = Modifier.padding(top = 8.dp))
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                text = "Закончить день",
                onClick = onFinish,
                enabled = false,
                fillWidth = false,
                horizontalPadding = 18.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = FinnyTheme.colors.onSurfaceMuted,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
