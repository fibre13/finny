package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.MenuIcon
import ru.onefortwo.finny.ui.common.MenuSection
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.StatBar
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.Explanations

/**
 * Главный экран (ТЗ 2.5.3): питомец, баланс, накопления, текущая цель,
 * показатели состояния и активное задание видны одновременно.
 * Отсюда доступны все разделы приложения.
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
    onOpenTasks: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAdult: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onFinishPeriod: () -> Unit,
    today: String,
) {
    val profile = state.profile ?: return
    val game = state.game
    val purchases = game.period.purchases
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    val accessory = parts.accessories.firstOrNull { it.id == profile.appearance.accessoryId }

    ScreenScaffold(
        title = "День ${game.period.number}",
        onBack = null,
        message = state.message,
        onDismissMessage = onDismissMessage,
    ) {
        Column {
            if (state.isDemo) {
                SectionCard(title = "Демонстрационный режим") {
                    Text(
                        text = "Идёт проверка на тестовом профиле. Сбросить его можно " +
                            "в разделе для взрослого.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.isTimeUp(today)) {
                SectionCard(title = "На сегодня хватит") {
                    Column {
                        Text(
                            text = "20 минут прошли, глазам нужен отдых. Прогресс сохранён, " +
                                "приходи завтра.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Это правило можно изменить в разделе для взрослого.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else if (state.isTimeRunningOut(today)) {
                SectionCard(title = "Время заканчивается") {
                    Text(
                        text = "Осталось ${Explanations.minutes(state.minutesLeft(today))} " +
                            "на сегодня. Успей закончить то, что начал.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.isDayFinished(today)) {
                SectionCard(title = "День прожит") {
                    Column {
                        Text(
                            text = "Сегодняшний игровой день уже закончен. " +
                                "${profile.petName} отдыхает и ждёт тебя завтра.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Завтра будут новые монеты и новый план.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

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
                modifier = Modifier.padding(bottom = 16.dp),
            )

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

            SectionCard(title = "Монеты") {
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
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            text = Explanations.forecast(game.goalForecast()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        Text(
                            text = "Цель пока не выбрана. Загляни в копилку.",
                            style = MaterialTheme.typography.bodyMedium,
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

            SectionCard(title = "Задание на сегодня") {
                Column {
                    if (activeTask != null) {
                        Text(activeTask.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = activeTask.topic.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (activeTask.id in state.completedTaskIds) {
                            Text(
                                text = "Это задание ты уже проходил. Числа будут новые, " +
                                    "а монет дадут половину.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    } else {
                        Text(
                            "Заданий для твоего класса пока нет.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    SecondaryButton(
                        text = "Открыть задания",
                        onClick = onOpenTasks,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            SectionCard(title = "Что можно сделать") {
                Column {
                    SecondaryButton(
                        text = if (game.period.isPlanConfirmed) "План на день" else "Составить план",
                        onClick = onOpenPlan,
                        icon = { MenuIcon(MenuSection.PLAN) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Покупки",
                        onClick = onOpenShop,
                        icon = { MenuIcon(MenuSection.SHOP) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Копилка и цель",
                        onClick = onOpenSavings,
                        icon = { MenuIcon(MenuSection.SAVINGS) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Гардероб",
                        onClick = onOpenWardrobe,
                        icon = { MenuIcon(MenuSection.WARDROBE) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Мой прогресс",
                        onClick = onOpenHistory,
                        icon = { MenuIcon(MenuSection.PROGRESS) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Словарик",
                        onClick = onOpenGlossary,
                        icon = { MenuIcon(MenuSection.GLOSSARY) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Как играть",
                        onClick = onOpenHelp,
                        icon = { MenuIcon(MenuSection.HELP) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SecondaryButton(
                        text = "Для взрослого",
                        onClick = onOpenAdult,
                        icon = { MenuIcon(MenuSection.ADULT) },
                    )
                }
            }

            val dayFinished = state.isDayFinished(today)

            PrimaryButton(
                text = "Закончить день",
                onClick = onFinishPeriod,
                enabled = game.period.canFinish && !dayFinished,
            )

            // Причина выводится для каждого случая, когда кнопка неактивна,
            // в том числе для прожитого дня. Объяснение есть и в блоке
            // «День прожит», но он остаётся далеко вверху экрана: до кнопки
            // нужно прокрутить, и рядом с ней ребёнок видел бы только
            // приглушённый цвет (ТЗ 3.6).
            val disabledReason = when {
                dayFinished -> "Сегодня день уже закончен. Новый план будет завтра."
                !game.period.isPlanConfirmed -> "Чтобы закончить день, сначала составь план."
                !game.period.canFinish ->
                    "Чтобы закончить день, купи что-нибудь или отложи монеты в копилку."
                else -> null
            }

            if (disabledReason != null) {
                Text(
                    text = disabledReason,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
