package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.common.PixelImage
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.pixelImage
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.theme.FinnyTheme

/*
 * Экран «Игры»: две большие карточки на выбор. «Сам» — задания, за три
 * задания за один заход в лавке появляется новая покупка. «С питомцем» —
 * игра без условий и без награды.
 */

/** Высота области картинки в карточке: человечек и питомец одного роста. */
private val CARD_PICTURE = 100.dp

@Composable
fun GamesScreen(
    state: AppState,
    parts: PetPartsContent,
    onBack: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenPlay: () -> Unit,
    /** Питомец спит: играть с ним можно будет завтра, задания открыты. */
    sleeping: Boolean = false,
) {
    val petName = state.profile?.petName ?: "питомцем"
    ScreenScaffold(title = "Игры", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "С кем ты хочешь поиграть?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            GameChoiceCard(
                title = "Сам",
                text = "3 задания за один заход — новая покупка для питомца в лавке",
                button = "Начать →",
                pictureDescription = "Человечек",
                onClick = onOpenTasks,
            ) {
                PixelRows(KID_ROWS, cell = 4.dp)
            }
            GameChoiceCard(
                title = "С $petName",
                text = if (sleeping) {
                    "${state.profile?.petName ?: "Питомец"} спит. Поиграем завтра!"
                } else {
                    "Просто поиграть. Ничего не нужно — только веселье!"
                },
                button = "Играть →",
                pictureDescription = petName,
                onClick = onOpenPlay,
                enabled = !sleeping,
            ) {
                ProfilePet(state = state, parts = parts, size = 100.dp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Большая карточка выбора: картинка, заголовок, пояснение и кнопка. */
@Composable
private fun GameChoiceCard(
    title: String,
    text: String,
    button: String,
    pictureDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    picture: @Composable () -> Unit,
) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_PICTURE)
                .clearAndSetSemantics { contentDescription = pictureDescription },
            contentAlignment = Alignment.BottomCenter,
        ) {
            picture()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = FinnyTheme.colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(14.dp))
        PixelButton(text = button, onClick = onClick, enabled = enabled)
    }
}

/**
 * Пиксельный рисунок, заданный строками символов палитры `art/pixel`
 * (точка — прозрачная клетка). Для небольших рисунков экранов игр.
 */
@Composable
internal fun PixelRows(rows: List<String>, cell: Dp, modifier: Modifier = Modifier) {
    val art = rememberPixelArt()
    val width = rows.maxOf { it.length }
    val image = remember(art, rows) {
        val pixels = IntArray(width * rows.size)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch != '.') {
                    val index = art.indexOf(ch)
                    if (index >= 0) pixels[y * width + x] = art.colors[index]
                }
            }
        }
        pixelImage(pixels, width)
    }
    PixelImage(
        image = image,
        modifier = modifier.size(cell * width, cell * rows.size).clearAndSetSemantics { },
    )
}

/** Человечек-ребёнок 16 × 22: волосы, розовые щёки, коралловая футболка, синие штаны. */
internal val KID_ROWS = listOf(
    "................",
    ".....KKKKKK.....",
    "....KttttttK....",
    "...KttttttttK...",
    "...KttnnnnttK...",
    "...KnnnnnnnnK...",
    "...KnKnnnnKnK...",
    "...KnnnnnnnnK...",
    "...KnNnnnnNnK...",
    "....KnnNNnnK....",
    ".....KKKKKK.....",
    "....KRRRRRRK....",
    "...KRRRRRRRRK...",
    "..KRKRRcRRRKRK..",
    "..KnKRRRRRRKnK..",
    "..KKKRRRRRRKKK..",
    "....KrrrrrrK....",
    "....KuuuuuuK....",
    "....KuuKKuuK....",
    "....KuuKKuuK....",
    "...KtttKKtttK...",
    "...KKKKKKKKKK...",
)

/** Клубок 16 × 16: розовые витки нитки и хвостик нитки вправо. */
internal val YARN_ROWS = listOf(
    "................",
    "................",
    ".....KKKKKK.....",
    "...KKPPPPnPKK...",
    "..KPPpPPPPnPPK..",
    "..KPPPpPPPPPPK..",
    ".KPpPPPpPPPPPPK.",
    ".KPPpPPPpPPPpPK.",
    ".KPPPpPPPpPpPPK.",
    ".KPPPPpPPPpPPPK.",
    ".KpPPPPpPpPPPpK.",
    "..KpPPPPpPPPpK..",
    "..KppPPPPPPppK..",
    "...KKppppppKKPPP",
    ".....KKKKKK.....",
    "................",
)
