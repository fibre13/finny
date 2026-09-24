package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.ui.common.CardSpacing
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.ChevronIcon
import ru.onefortwo.finny.ui.common.IconTile
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.MenuIcon
import ru.onefortwo.finny.ui.common.MenuSection
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.NavIcon
import ru.onefortwo.finny.ui.common.NavSection
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.StatBar
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.petCaption
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Главный экран (ТЗ 2.5.3): питомец, баланс, накопления, текущая цель,
 * показатели состояния и активное задание видны одновременно.
 *
 * Задания, гардероб и прогресс открываются вкладками навигации; остальные
 * разделы — из списка «Что можно сделать».
 */
@Composable
fun MainScreen(
    state: AppState,
    parts: PetPartsContent,
    activeTask: TaskContent?,
    /** Название товара по идентификатору для списка сегодняшних покупок. */
    titleOf: (String) -> String,
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
) {
    val profile = state.profile ?: return
    val game = state.game
    val purchases = game.period.purchases
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    val accessory = parts.accessories.firstOrNull { it.id == profile.appearance.accessoryId }
    val accessoryTitle = accessory?.title ?: "без украшения"

    ScreenScaffold(
        eyebrow = "День ${game.period.number}",
        title = "Проведаем ${profile.petName}",
        balance = game.balance,
        onBack = null,
        message = state.message,
        onDismissMessage = onDismissMessage,
    ) {
        Column {
            if (state.isDemo) {
                SectionCard(title = "Демонстрационный режим", tone = CardTone.Sage) {
                    SupportingText(
                        "Идёт проверка на тестовом профиле. Сбросить его можно " +
                            "в разделе для взрослого.",
                    )
                }
            }

            if (state.isTimeUp(today)) {
                SectionCard(title = "На сегодня хватит", tone = CardTone.Warning) {
                    Column {
                        SupportingText(
                            "20 минут прошли, глазам нужен отдых. Прогресс сохранён, " +
                                "приходи завтра.",
                        )
                        SupportingText(
                            "Это правило можно изменить в разделе для взрослого.",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else if (state.isTimeRunningOut(today)) {
                SectionCard(title = "Время заканчивается", tone = CardTone.Warning) {
                    SupportingText(
                        "Осталось ${Explanations.minutes(state.minutesLeft(today))} " +
                            "на сегодня. Успей закончить то, что начал.",
                    )
                }
            }

            if (state.isDayFinished(today)) {
                // Закончив день, ребёнок сразу получает монеты следующего и
                // может составить на него план. Завтра остаётся только
                // закончить этот день — так и сказано, без обещания монет и
                // плана «завтра»: они уже есть.
                SectionCard(title = "На сегодня день закончен", tone = CardTone.Sage) {
                    Column {
                        // Предложение составить план уместно, только пока плана
                        // нет: утверждённый план второй раз не составляется.
                        Text(
                            text = "День ${game.period.number - 1} закончен. " +
                                if (game.period.isPlanConfirmed) {
                                    "План на день ${game.period.number} составлен: " +
                                        "можно делать покупки."
                                } else {
                                    "Монеты на день ${game.period.number} уже у тебя: " +
                                        "можно составить план и сделать покупки."
                                },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        SupportingText(
                            "Закончить день ${game.period.number} получится завтра: " +
                                "один игровой день — в одни сутки.",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            SectionCard(tone = CardTone.Sage) {
                Column {
                    Text(profile.petName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = petCaption(game.stage, accessoryTitle),
                        style = MaterialTheme.typography.titleMedium,
                        color = FinnyTheme.colors.onSurfaceMuted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    PetFigure(
                        petName = profile.petName,
                        speciesId = profile.appearance.speciesId,
                        speciesTitle = species?.title ?: "Питомец",
                        accessoryId = profile.appearance.accessoryId,
                        accessoryTitle = accessoryTitle,
                        colorHex = color?.hex ?: "#CCCCCC",
                        stage = game.stage,
                        care = game.pet.care.level,
                        joy = game.pet.joy.level,
                        scene = true,
                        house = state.hasScenery("house"),
                        caption = false,
                    )
                }
            }

            SectionCard(title = "Как себя чувствует ${profile.petName}") {
                Column {
                    StatBar(
                        name = Explanations.statName(PetStatKind.CARE),
                        value = game.pet.care.value,
                        label = Explanations.statLabel(PetStatKind.CARE, game.pet.care),
                        level = game.pet.care.level,
                    )
                    StatBar(
                        name = Explanations.statName(PetStatKind.JOY),
                        value = game.pet.joy.value,
                        label = Explanations.statLabel(PetStatKind.JOY, game.pet.joy),
                        level = game.pet.joy.level,
                    )
                }
            }

            // Задание открывается прямо отсюда. Заголовок говорит, что это:
            // следующее новое или, когда новых не осталось, предложение
            // решить одно из пройденных ещё раз. «Задание на сегодня» здесь
            // не пишется: задания не привязаны к дню.
            val isRepeat = activeTask != null && activeTask.id in state.completedTaskIds
            SectionCard(
                title = if (isRepeat) "Новых заданий нет" else "Следующее задание",
                tone = CardTone.Coin,
                icon = {
                    NavIcon(section = NavSection.TASKS, color = FinnyTheme.colors.attention)
                },
            ) {
                Column {
                    if (activeTask != null) {
                        if (isRepeat) {
                            SupportingText(
                                "Можно решить ещё раз:",
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Text(activeTask.title, style = MaterialTheme.typography.titleMedium)
                        SupportingText(
                            activeTask.topic.displayName,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        if (isRepeat) {
                            SupportingText(
                                repeatNote(activeTask),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        PrimaryButton(
                            text = if (isRepeat) "Решить ещё раз" else "Начать задание",
                            onClick = { onOpenTask(activeTask.id) },
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    } else {
                        SupportingText("Заданий пока нет.")
                    }
                }
            }

            SectionCard(title = "Монеты", tone = CardTone.Primary) {
                Column {
                    LabeledValue("Можно потратить", Explanations.coins(game.balance))
                    LabeledValue("В копилке", Explanations.coins(game.savings.saved))

                    if (goalTitle != null) {
                        val goal = game.savings.goal
                        LabeledValue("Цель", goalTitle)
                        if (goal != null) {
                            LabeledValue("Осталось накопить", Explanations.coins(game.savings.remaining))
                            ProgressBar(
                                fraction = game.savings.saved.amount.toFloat() / goal.price.amount,
                                color = FinnyTheme.colors.coin,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        SupportingText(
                            text = Explanations.forecast(game.goalForecast()),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    } else {
                        SupportingText(
                            text = "Цель пока не выбрана. Загляни в копилку.",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // Покупки дня перечислены списком, а не одним сообщением:
            // сообщение показывает только последнее действие, и после
            // нескольких покупок подряд предыдущие пропадали из виду.
            if (purchases.isNotEmpty()) {
                SectionCard(title = "Сегодня куплено") {
                    Column {
                        purchases.forEach { record ->
                            LabeledValue(
                                label = titleOf(record.itemId),
                                value = Explanations.coins(record.price),
                            )
                        }
                        LabeledValue(
                            label = "Всего потрачено",
                            value = Explanations.coins(
                                purchases.fold(Coins.ZERO) { sum, it -> sum + it.price },
                            ),
                        )
                    }
                }
            }

            Text(
                text = "Что можно сделать",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = CardSpacing),
            ) {
                MenuRow(
                    text = if (game.period.isPlanConfirmed) "План на день" else "Составить план",
                    section = MenuSection.PLAN,
                    onClick = onOpenPlan,
                )
                MenuRow(text = "Покупки", section = MenuSection.SHOP, onClick = onOpenShop)
                MenuRow(text = "Копилка и цель", section = MenuSection.SAVINGS, onClick = onOpenSavings)
                MenuRow(text = "Словарик", section = MenuSection.GLOSSARY, onClick = onOpenGlossary)
                MenuRow(text = "Как играть", section = MenuSection.HELP, onClick = onOpenHelp)
                MenuRow(text = "Для взрослого", section = MenuSection.ADULT, onClick = onOpenAdult)
            }

            val dayFinished = state.isDayFinished(today)

            PrimaryButton(
                text = "Закончить день",
                onClick = onFinishPeriod,
                enabled = game.period.canFinish && !dayFinished,
            )

            // Причина выводится для каждого случая, когда кнопка неактивна,
            // в том числе для прожитого дня: рядом с неактивной кнопкой
            // ребёнок иначе видел бы только серый цвет (ТЗ 3.6).
            val disabledReason = when {
                dayFinished -> "Закончить этот день получится завтра: один игровой день — в одни сутки."
                !game.period.isPlanConfirmed -> "Чтобы закончить день, сначала составь план."
                !game.period.canFinish ->
                    "Чтобы закончить день, купи что-нибудь или отложи монеты в копилку."
                else -> null
            }

            if (disabledReason != null) {
                SupportingText(
                    text = disabledReason,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Пункт списка разделов: плитка с пиктограммой, название и шеврон.
 * Нажимается вся строка.
 */
@Composable
private fun MenuRow(
    text: String,
    section: MenuSection,
    onClick: () -> Unit,
) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = maxOf(MinTouchTarget, 64.dp))
            .softShadow(shape, lift = false)
            .clip(shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(container = colors.appBackground) {
            MenuIcon(section)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        ChevronIcon(color = colors.onSurfaceMuted)
    }
}
