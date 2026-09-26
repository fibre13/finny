package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.common.rememberPulse
import ru.onefortwo.finny.ui.theme.FinnyTheme

/*
 * ТЕСТ (ветка test/kopilka-a): знакомство начинается с подарка. У ребёнка
 * день рождения, в коробке с бантом — будущий питомец. Карточка правил
 * называет цель игры и три решения (ТЗ 2.5.1): нужное, приятное, копилка.
 * Вернуться к правилам можно кнопкой «?» на дворе — там экран «Как играть».
 */

/** Ширина колонки знакомства на планшете. */
private val ONBOARDING_MAX_WIDTH = 480.dp

/** Точки шагов знакомства: текущий — зелёный. */
@Composable
internal fun StepDots(current: Int, modifier: Modifier = Modifier, total: Int = 3) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Шаг ${current + 1} из $total" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(total) { i ->
            val fill = if (i == current) colors.green else colors.cardLight
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 12.dp)
                    .pixelPanel(fill, fill, fill, colors.outline),
            )
        }
    }
}

/**
 * Объёмная пиксельная кнопка: светлая кромка сверху, тёмная снизу.
 * Неактивная — серая. [pulse] — мягкая пульсация, когда кнопку пора нажать.
 */
@Composable
internal fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pulse: Boolean = false,
) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val scale = rememberPulse()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer {
                val s = if (enabled && pulse) scale.value else 1f
                scaleX = s
                scaleY = s
            }
            .then(
                if (enabled) {
                    Modifier.pixelPanel(colors.green, colors.greenLight, colors.greenDark, colors.outline, 3)
                } else {
                    Modifier.pixelPanel(colors.cardShadow, colors.card, colors.cardShadow, colors.outline, 2)
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) colors.card else colors.outline,
        )
    }
}

/** Небо сверху, луг снизу и пиксельные конфетти — фон подарка и финала знакомства. */
@Composable
internal fun MeadowBackground(art: PixelArt, grassFrom: Float, content: @Composable () -> Unit) {
    val colors = remember(art) { YardColors(art) }
    Box(modifier = Modifier.fillMaxSize().background(colors.sky)) {
        Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val top = size.height * grassFrom
            drawRect(colors.grass, Offset(0f, top), Size(size.width, size.height - top))
            val c = 2.dp.toPx()
            // Пологие холмы по краю луга.
            var x = 0f
            while (x < size.width) {
                val h = (3 + 2 * kotlin.math.sin(x / (18 * c))) * c
                drawRect(colors.grassDark, Offset(x, top - h), Size(c, h))
                x += c
            }
            // Конфетти в небе: одни и те же места при каждом запуске.
            val confetti = listOf(0.06f to 0.07f, 0.17f to 0.03f, 0.29f to 0.11f, 0.83f to 0.05f, 0.92f to 0.14f,
                0.71f to 0.02f, 0.49f to 0.06f, 0.11f to 0.25f, 0.88f to 0.27f, 0.61f to 0.17f, 0.39f to 0.21f)
            val tints = listOf(colors.coral, colors.coin, colors.blue, colors.green, colors.pink)
            confetti.forEachIndexed { i, (fx, fy) ->
                val p = Offset(size.width * fx, size.height * fy)
                drawRect(tints[i % tints.size], p, Size(2 * c, c))
                drawRect(tints[i % tints.size], p + Offset(c, c), Size(c, c))
            }
        }
        content()
    }
}

@Composable
fun GiftScreen(onOpen: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val motion = motionAllowed()
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }
    var opening by remember { mutableStateOf(false) }
    val breathe = rememberPulse()

    fun open() {
        if (opening) return
        if (!motion) {
            onOpen()
            return
        }
        opening = true
        scope.launch {
            // Коробка подпрыгивает и чуть раздувается — вот-вот откроется.
            hop.animateTo(1f, tween(260))
            hop.animateTo(0f, tween(200))
            onOpen()
            opening = false
        }
    }

    MeadowBackground(art, grassFrom = 0.42f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = ONBOARDING_MAX_WIDTH).fillMaxWidth()) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "С днём рождения!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                    Sprite(art, "yard_balloon_red", Modifier.align(Alignment.TopStart).offset(x = 6.dp, y = 44.dp), cell = 3.dp)
                    Sprite(art, "yard_balloon_blue", Modifier.align(Alignment.TopStart).offset(x = 44.dp, y = 8.dp), cell = 3.dp)
                    Sprite(art, "yard_balloon_yellow", Modifier.align(Alignment.TopEnd).offset(x = (-44).dp, y = 16.dp), cell = 3.dp)
                    Sprite(art, "yard_balloon_green", Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 56.dp), cell = 3.dp)
                    Sprite(
                        art, "yard_gift",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .graphicsLayer {
                                val s = breathe.value + 0.12f * hop.value
                                scaleX = s
                                scaleY = s
                                translationY = -28.dp.toPx() * hop.value
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            }
                            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = ::open)
                            .semantics { contentDescription = "Подарок: коробка с бантом. Открыть подарок" },
                        cell = 4.dp,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Тебе подарили кое-кого особенного. Внутри — твой питомец, теперь ты его хозяин.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    )
                    Text(
                        text = "Каждый день у тебя будут монеты на его содержание. Трать с умом:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinnyTheme.colors.onSurfaceMuted,
                    )
                    RuleRow(art, "icon_care", "на нужное — чтобы был сыт")
                    RuleRow(art, "yard_ball", "на развлечения — чтобы радовался")
                    RuleRow(art, "coin_0", "в копилку — на большую мечту")
                    Text(
                        text = "Ошибиться не страшно: исправить можно завтра.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinnyTheme.colors.onSurfaceMuted,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                StepDots(current = 0, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(12.dp))
                PixelButton(text = "Открыть подарок", onClick = ::open, pulse = true)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RuleRow(art: PixelArt, icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) { Sprite(art, icon, cell = 3.dp) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
