package ru.onefortwo.finny.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetAppearance
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import ru.onefortwo.finny.content.Accessories
import ru.onefortwo.finny.content.NameFilter
import ru.onefortwo.finny.ui.state.PetSpeech
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.CheckOption
import ru.onefortwo.finny.ui.common.ColorSwatch
import ru.onefortwo.finny.ui.common.FinnyTextField
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SelectButton
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.parseColor
import ru.onefortwo.finny.ui.common.petCaption
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Предел длины игрового имени. */
private const val NAME_MAX_LENGTH = 15

/** Пауза в наборе имени, после которой проверяются короткие слова. */
private const val NAME_PAUSE_MS = 1000L

/** Масштаб шрифта, с которого варианты выстраиваются сеткой, а не в строку. */
private const val LARGE_FONT_SCALE = 1.3f

/**
 * Шаги 2–4 знакомства: внешность питомца, уровень заданий с игровым
 * именем и проверка выбора перед началом (ТЗ 2.5.2, 2.5.8).
 *
 * Выбор держится на этом экране и переживает поворот; кнопка «Назад»
 * и системный жест возвращают на предыдущий шаг, а не сбрасывают выбор.
 *
 * Реальное имя, телефон и почта не запрашиваются: профиль остаётся
 * локальным и обезличенным (ТЗ 3.5, Приложение А шаг 2). Спрашивается
 * уровень заданий, а не класс и не возраст: для подбора заданий этого
 * достаточно, и о ребёнке ничего не собирается.
 *
 * На телефоне 360 × 800 dp при обычном шрифте каждый шаг помещается
 * целиком вместе с кнопкой; при крупном шрифте экран прокручивается,
 * а варианты выстраиваются сеткой.
 */
@Composable
fun PetSetupScreen(
    parts: PetPartsContent,
    onDone: (String, PetAppearance, Difficulty) -> Unit,
    onBack: (() -> Unit)? = null,
    initialDifficulty: Difficulty? = null,
    nameFilter: NameFilter = NameFilter.NONE,
) {
    var step by rememberSaveable { mutableIntStateOf(2) }
    var speciesId by rememberSaveable { mutableStateOf(parts.species.first().id) }
    var colorId by rememberSaveable { mutableStateOf(parts.colors.first().id) }
    var accessoryId by rememberSaveable { mutableStateOf(parts.accessories.first().id) }
    var name by rememberSaveable { mutableStateOf("") }
    var difficultyName by rememberSaveable { mutableStateOf(initialDifficulty?.name) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }

    fun rejectName() {
        name = ""
        nameError = NameFilter.MESSAGE
    }

    // Короткие слова («лох», «попа») проверяются, когда ребёнок перестал
    // печатать: иначе стирались бы «Лохматик» и «Попугай» на полпути.
    LaunchedEffect(name) {
        if (name.isNotBlank()) {
            delay(NAME_PAUSE_MS)
            if (nameFilter.isForbiddenWord(name)) rejectName()
        }
    }
    val difficulty = difficultyName?.let(Difficulty::ofName)

    val species = parts.species.first { it.id == speciesId }
    val color = parts.colors.first { it.id == colorId }
    val accessoryTitle = Accessories.title(parts, accessoryId)

    // Системный жест «назад» на шагах 3 и 4 возвращает на шаг раньше.
    BackHandler(enabled = step > 2) { step -= 1 }
    val back: (() -> Unit)? = if (step > 2) ({ step -= 1 }) else onBack

    val title = when (step) {
        2 -> "Кто будет твоим другом?"
        else -> "Как назовём?"
    }

    // Профиля ещё нет: питомец показывается на начальной стадии и
    // в спокойном состоянии.
    val figure: @Composable (Dp) -> Unit = { size ->
        PetFigure(
            petName = name.ifBlank { "Пока без имени" },
            speciesId = species.id,
            speciesTitle = species.title,
            accessoryId = accessoryId,
            accessoryTitle = accessoryTitle,
            colorHex = color.hex,
            stage = GrowthStage.BABY,
            size = size,
            caption = false,
            modifier = Modifier.width(size),
        )
    }

    // ТЕСТ: финал знакомства — питомец выпрыгивает на лугу.
    if (step == 4) {
        ReadyStep(
            name = name.trim(),
            figure = { size, reaction, onEnd ->
                PetFigure(
                    petName = name.trim(),
                    speciesId = species.id,
                    speciesTitle = species.title,
                    accessoryId = accessoryId,
                    accessoryTitle = accessoryTitle,
                    colorHex = color.hex,
                    stage = GrowthStage.BABY,
                    care = StatLevel.HIGH,
                    joy = StatLevel.HIGH,
                    size = size,
                    caption = false,
                    plain = true,
                    reaction = reaction,
                    onReactionEnd = onEnd,
                    description = "${name.trim()}, ${species.title.lowercase()}",
                    modifier = Modifier.width(size),
                )
            },
            onBack = { step = 3 },
            onStart = {
                onDone(name.trim(), PetAppearance(speciesId, colorId, accessoryId), difficulty ?: Difficulty.SIMPLE)
            },
        )
        return
    }

    ScreenScaffold(
        title = title,
        onBack = back,
        top = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StepDots(current = step - 1)
            }
        },
        // Каждый шаг открывается сверху, а не с той же прокрутки, где
        // остался предыдущий.
        scrollKey = step,
        bottomPadding = 16.dp,
        centerOnTablet = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
                2 -> AppearanceStep(
                    parts = parts,
                    speciesId = speciesId,
                    colorId = colorId,
                    accessoryId = accessoryId,
                    figure = figure,
                    onSpecies = { speciesId = it },
                    onColor = { colorId = it },
                    // ТЕСТ: украшения надеваются и снимаются по одному.
                    onAccessory = { accessoryId = Accessories.toggle(accessoryId, it, parts.accessories.map { a -> a.id }) },
                    onNext = { step = 3 },
                )

                else -> NameStep(
                    preview = {
                        PreviewRow(
                            figure = { figure(64.dp) },
                            name = name.trim(),
                            caption = "${species.title} · ${color.title.lowercase()} · $accessoryTitle",
                            large = false,
                            onEdit = { step = 2 },
                        )
                    },
                    difficulty = difficulty,
                    onDifficulty = { difficultyName = it.name },
                    name = name,
                    nameError = nameError,
                    // Предел длины держится молча, без счётчика на экране.
                    // Запрещённый корень стирает поле сразу (ТЗ 3.5).
                    onName = {
                        val typed = it.take(NAME_MAX_LENGTH)
                        if (nameFilter.hasForbiddenRoot(typed)) {
                            rejectName()
                        } else {
                            name = typed
                            // ТЕСТ: сверх предела — короткое пояснение, лишнее не вводится.
                            nameError = when {
                                it.length > NAME_MAX_LENGTH -> "Слишком длинное имя"
                                typed.isNotEmpty() -> null
                                else -> nameError
                            }
                        }
                    },
                    onNext = { if (nameFilter.isAllowed(name)) step = 4 else rejectName() },
                )

            }
        }
    }
}

/**
 * Шаг 2: вид, окрас и украшение.
 *
 * Вид выбирается списком справа от превью: так превью, вид, окрас и
 * украшение помещаются на один экран. При крупном шрифте превью встаёт
 * над вариантами, а варианты — в сетку, чтобы подписи не рвались.
 */
@Composable
private fun AppearanceStep(
    parts: PetPartsContent,
    speciesId: String,
    colorId: String,
    accessoryId: String,
    figure: @Composable (Dp) -> Unit,
    onSpecies: (String) -> Unit,
    onColor: (String) -> Unit,
    onAccessory: (String) -> Unit,
    onNext: () -> Unit,
) {
    val large = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val colorHex = parts.colors.firstOrNull { it.id == colorId }?.hex ?: "#CCCCCC"

    // Крупное превью по центру, меняется сразу при выборе.
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PreviewCard { figure(150.dp) }
    }
    // Виды — картинками; название озвучивается программой чтения с экрана.
    OptionRow(count = parts.species.size, large = large, minItemWidth = 96.dp) { index, modifier ->
        val option = parts.species[index]
        CheckOption(
            title = option.title,
            selected = option.id == speciesId,
            onClick = { onSpecies(option.id) },
            showTitle = false,
            swatch = {
                PetFigure(
                    petName = option.title,
                    speciesId = option.id,
                    speciesTitle = option.title,
                    accessoryId = "none",
                    accessoryTitle = "",
                    colorHex = colorHex,
                    stage = GrowthStage.BABY,
                    size = 64.dp,
                    caption = false,
                    plain = true,
                    modifier = Modifier.width(64.dp).clearAndSetSemantics { },
                )
            },
            modifier = modifier,
        )
    }

    // ТЕСТ: без заголовков групп и без подписей цветов — только кружки;
    // названия цветов по-прежнему озвучивает TalkBack.
    OptionRow(
        count = parts.colors.size,
        large = large,
        minItemWidth = 100.dp,
    ) { index, modifier ->
        val option = parts.colors[index]
        CheckOption(
            title = option.title,
            selected = option.id == colorId,
            onClick = { onColor(option.id) },
            swatch = { ColorSwatch(parseColor(option.hex), size = 32.dp) },
            showTitle = false,
            modifier = modifier.heightIn(min = 64.dp),
        )
    }

    // ТЕСТ: четыре варианта — сеткой по два: в одну строку слова рвались.
    OptionRow(
        count = parts.accessories.size,
        large = true,
        minItemWidth = 150.dp,
    ) { index, modifier ->
        val option = parts.accessories[index]
        CheckOption(
            title = option.title,
            selected = Accessories.isOn(accessoryId, option.id),
            onClick = { onAccessory(option.id) },
            modifier = modifier.heightIn(min = 64.dp),
        )
    }

    PrimaryButton(text = "Далее", onClick = onNext, modifier = Modifier.padding(top = 4.dp))
}

/**
 * Ряд вариантов: в одну строку с заданными долями ширины, при крупном
 * шрифте — сетка, которая сама подбирает число колонок.
 */
@Composable
private fun OptionRow(
    count: Int,
    large: Boolean,
    minItemWidth: Dp,
    weights: List<Float> = List(count) { 1f },
    item: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    if (large) {
        AdaptiveGrid(count = count, minItemWidth = minItemWidth) { index ->
            item(index, Modifier.weight(1f))
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(count) { index -> item(index, Modifier.weight(weights[index])) }
        }
    }
}

/** Шаг 3: уровень заданий и игровое имя. */
@Composable
private fun NameStep(
    preview: @Composable () -> Unit,
    difficulty: Difficulty?,
    onDifficulty: (Difficulty) -> Unit,
    name: String,
    nameError: String?,
    onName: (String) -> Unit,
    onNext: () -> Unit,
) {
    preview()

    FinnyTextField(
        value = name,
        onValueChange = onName,
        label = "Имя питомца",
        // Имя — не слово словаря: автоисправление подменяло бы его.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrectEnabled = false,
        ),
        // Предупреждение по ТЗ 3.5: о ребёнке ничего не спрашивается.
        supportingText = "Придумай игровое имя — настоящее писать не нужно.",
        errorText = nameError,
    )

    GroupTitle("Какие задания?")
    val large = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    OptionRow(count = Difficulty.entries.size, large = large, minItemWidth = 150.dp) { index, modifier ->
        val option = Difficulty.entries[index]
        SelectButton(
            text = option.displayName,
            supporting = if (option == Difficulty.SIMPLE) "Счёт до 20" else "С делением",
            selected = option == difficulty,
            onClick = { onDifficulty(option) },
            compact = true,
            modifier = modifier,
        )
    }

    val ready = difficulty != null && name.isNotBlank()
    PrimaryButton(text = "Далее", enabled = ready, onClick = onNext, modifier = Modifier.padding(top = 4.dp))

    // Причина недоступности названа текстом: по одному виду кнопки
    // непонятно, чего она ждёт (ТЗ 3.6).
    if (!ready) {
        SupportingText(
            when {
                difficulty == null && name.isBlank() -> "Придумай имя и выбери задания."
                difficulty == null -> "Выбери задания."
                else -> "Придумай имя."
            },
        )
    }
}

/** Превью питомца в салатовой карточке. */
@Composable
private fun PreviewCard(figure: @Composable () -> Unit) {
    SectionCard(
        tone = CardTone.Sage,
        contentPadding = PaddingValues(12.dp),
        bottomSpacing = 0.dp,
        modifier = Modifier.width(164.dp),
    ) {
        figure()
    }
}

/**
 * Превью с именем: фигура слева, имя и подпись справа. Имя обновляется
 * по мере ввода; пока его нет — «Пока без имени».
 */
@Composable
private fun PreviewRow(
    figure: @Composable () -> Unit,
    name: String,
    caption: String,
    large: Boolean,
    onEdit: () -> Unit,
) {
    // Имя в карточке не повторяется: его видно в поле ниже.
    SectionCard(
        tone = CardTone.Sage,
        contentPadding = PaddingValues(start = 10.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
        bottomSpacing = 0.dp,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onEdit)
            .semantics(mergeDescendants = true) { contentDescription = "$caption. Изменить внешность" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            figure()
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = caption,
                    style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "изменить ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = FinnyTheme.colors.successText,
                )
            }
        }
    }
}

/**
 * Финал знакомства: питомец выпрыгивает на лугу и говорит «Привет!»,
 * под ним крупно имя, ниже «Начать игру». Имя не повторяется дважды.
 */
@Composable
private fun ReadyStep(
    name: String,
    figure: @Composable (Dp, PetReaction?, (Long) -> Unit) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    val reaction = if (round < 3) PetReaction(PetReactions.PLAY, id = round + 1L) else null
    MeadowBackground(art, grassFrom = 0.46f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                        .clickable(role = Role.Button, onClick = onBack)
                        .semantics { contentDescription = "Назад" },
                    contentAlignment = Alignment.Center,
                ) {
                    Sprite(art, "yard_icon_back")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Облачко опущено к голове: над ней в рамке питомца пустые ряды
            // для прыжка. Рисуется поверх питомца.
            SpeechBubble(
                colors = colors,
                text = PetSpeech.HELLO,
                modifier = Modifier.offset(y = 52.dp).zIndex(1f),
            )
            figure(192.dp, reaction) { round++ }
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .widthIn(min = 240.dp, max = 360.dp)
                    .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            PixelButton(text = "Начать игру", onClick = onStart, pulse = true, modifier = Modifier.widthIn(max = 480.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}
