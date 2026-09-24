package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.ChevronIcon
import ru.onefortwo.finny.ui.common.fillRemaining
import ru.onefortwo.finny.ui.common.Eyebrow
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.HeaderIconButton
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.LineGlyph
import ru.onefortwo.finny.ui.common.LineIcon
import ru.onefortwo.finny.ui.common.LocalMutedColor
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.StatLine
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Промежуток между блоками главного экрана. */
private val BlockGap = 8.dp

/** Пределы фигуры питомца: она уступает высоту остальным блокам. */
private val FigureMax = 96.dp
private val FigureMin = 72.dp

/**
 * Главный экран (ТЗ 2.5.3): питомец, баланс, накопления, текущая цель,
 * показатели состояния и активное задание видны одновременно.
 *
 * Экран помещается целиком, без прокрутки, на телефоне 360×640 dp при
 * обычном размере шрифта (макет Figma «Главная — один экран», 42:2 и
 * состояния под ним). Разделы открываются значками сверху, как вкладки
 * снизу; «Как играть» и «Для взрослого» — кнопками в шапке. Карточка
 * питомца забирает оставшуюся высоту: фигура уменьшается от 96 до 72 dp,
 * а при нехватке места строка «стадия · украшение» скрывается. При
 * крупном шрифте экран прокручивается, ничего не обрезается.
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
) {
    val profile = state.profile ?: return
    val game = state.game
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    val accessory = parts.accessories.firstOrNull { it.id == profile.appearance.accessoryId }
    // «Без украшения» в подписи не пишется: подпись должна помещаться
    // в одну строку рядом с фигурой.
    val caption = if (accessory == null || accessory.id == "none") {
        game.stage.displayName
    } else {
        "${game.stage.displayName} · ${accessory.title.lowercase()}"
    }
    var showHome by rememberSaveable { mutableStateOf(false) }

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

        PetCard(
            petName = profile.petName,
            caption = caption,
            figure = { size ->
                PetFigure(
                    petName = profile.petName,
                    speciesId = profile.appearance.speciesId,
                    speciesTitle = species?.title ?: "Питомец",
                    accessoryId = profile.appearance.accessoryId,
                    accessoryTitle = accessory?.title ?: "без украшения",
                    colorHex = color?.hex ?: "#CCCCCC",
                    stage = game.stage,
                    care = game.pet.care.level,
                    joy = game.pet.joy.level,
                    size = size,
                    caption = false,
                    reaction = reaction,
                    onReactionEnd = onReactionPlayed,
                )
            },
            onOpenHome = { showHome = true },
            stats = {
                StatLine(
                    name = Explanations.statName(PetStatKind.CARE),
                    value = game.pet.care.value,
                    label = Explanations.statLabel(PetStatKind.CARE, game.pet.care),
                    level = game.pet.care.level,
                )
                StatLine(
                    name = Explanations.statName(PetStatKind.JOY),
                    value = game.pet.joy.value,
                    label = Explanations.statLabel(PetStatKind.JOY, game.pet.joy),
                    level = game.pet.joy.level,
                )
            },
        )
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

    if (showHome) {
        FinnyDialog(
            title = "${profile.petName} дома",
            onDismiss = { showHome = false },
            content = {
                PetFigure(
                    petName = profile.petName,
                    speciesId = profile.appearance.speciesId,
                    speciesTitle = species?.title ?: "Питомец",
                    accessoryId = profile.appearance.accessoryId,
                    accessoryTitle = accessory?.title ?: "без украшения",
                    colorHex = color?.hex ?: "#CCCCCC",
                    stage = game.stage,
                    care = game.pet.care.level,
                    joy = game.pet.joy.level,
                    scene = true,
                    house = state.hasScenery("house"),
                    stickers = state.hasScenery("stickers"),
                    // В сцене одно место под предмет цели: последняя полученная.
                    goalId = state.achievedGoalIds.lastOrNull(),
                    caption = false,
                )
                SupportingText(caption, modifier = Modifier.padding(top = 12.dp))
            },
            actions = {
                PrimaryButton(text = "Понятно", onClick = { showHome = false })
            },
        )
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
 * Карточка питомца и самочувствия. Забирает свободную высоту экрана;
 * фигура — существующий рисунок, по нажатию открывается сцена с домом.
 */
@Composable
private fun PetCard(
    petName: String,
    caption: String,
    figure: @Composable (Dp) -> Unit,
    onOpenHome: () -> Unit,
    stats: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.medium

    // Крупный шрифт: экран всё равно прокручивается, фигура остаётся полной.
    val minHeight = if (LocalDensity.current.fontScale >= 1.3f) FigureMax + 24.dp else FigureMin + 24.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillRemaining(minHeight)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.selectedContainer)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val inner = if (constraints.hasBoundedHeight) maxHeight else FigureMax
        val figureSize = inner.coerceIn(FigureMin, FigureMax)
        // Подпись — строка 24 dp над двумя показателями по 34 dp.
        val showCaption = inner >= 96.dp || LocalDensity.current.fontScale > 1f

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(figureSize)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Посмотреть, где живёт $petName",
                        onClick = onOpenHome,
                    ),
            ) {
                figure(figureSize)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (showCaption) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                stats()
            }
        }
    }
}

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
        PrimaryButton(text = "Закончить день", onClick = onFinish)
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
