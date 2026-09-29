package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.ShopItemContent
import ru.onefortwo.finny.content.effectFor
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.PetState
import ru.onefortwo.finny.economy.PurchaseRecord
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.IconTile
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PixelIcon
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.FinnyTheme
import ru.onefortwo.finny.ui.theme.PillShape

/**
 * Каталог покупок (ТЗ 2.5.6).
 *
 * Товар занимает одну строку: значок, название и кнопка с ценой. Влияние
 * на питомца показывается в окне подтверждения, до списания монет: в строке
 * шириной 360 dp оно переносилось на вторую строку и вдвое удлиняло список.
 * Покупка требует отдельного подтверждения. Покупка при недостатке монет
 * не выполняется, вместо этого приложение объясняет, чего не хватает.
 */
@Composable
fun ShopScreen(
    items: List<ShopItemContent>,
    /** Имя питомца для пояснений в окне покупки. */
    petName: String = "Финни",
    /** Состояние питомца: нужно, чтобы предупредить о покупке без пользы. */
    pet: PetState,
    balance: Coins,
    /**
     * Покупки текущего дня: купленное отмечается в строке товара, а сумма
     * выводится в сводке. Сообщение после покупки показывает только
     * последнее действие, поэтому покупки дня видны здесь.
     */
    todayPurchases: List<PurchaseRecord> = emptyList(),
    /** План дня утверждён; иначе мягко напоминается о нём. */
    planConfirmed: Boolean = true,
    /** Сколько в банках «Нужное» и «Хочу»; `null` — не показывать. */
    jars: Pair<Int, Int>? = null,
    message: FeedbackMessage?,
    onDismissMessage: () -> Unit,
    onBuy: (String) -> Unit,
    onBack: () -> Unit,
) {
    // Хранится идентификатор, а не сама позиция: строка переживает поворот
    // экрана, позиция восстанавливается из каталога.
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    val boughtIds = todayPurchases.map { it.itemId }.toSet()

    ScreenScaffold(
        eyebrow = "Для питомца",
        title = "Покупки",
        balance = balance,
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
        bottomPadding = 16.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Покупка до плана не запрещена, но порядок ТЗ 2.5.5 — план раньше.
            if (!planConfirmed) {
                SectionCard(
                    tone = CardTone.Warning,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    bottomSpacing = 0.dp,
                ) {
                    Text(
                        text = "Сначала разложи монеты по банкам в «Плане» — так проще не потратить лишнего.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            SectionCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                bottomSpacing = 0.dp,
            ) {
                Column {
                    LabeledValue("Можно потратить", Explanations.coins(balance))
                    if (jars != null) {
                        LabeledValue("В банке «Нужное»", Explanations.coins(jars.first))
                        LabeledValue("В банке «Хочу»", Explanations.coins(jars.second))
                    }
                    if (todayPurchases.isNotEmpty()) {
                        LabeledValue(
                            label = "Сегодня потрачено",
                            value = Explanations.coins(
                                todayPurchases.fold(Coins.ZERO) { sum, it -> sum + it.price },
                            ),
                        )
                    }
                }
            }

            ShopSection(
                title = "Нужное",
                hint = "покупай первым",
                tone = CardTone.Sage,
                items = items.filter { it.category == ItemCategory.NEEDS },
                balance = balance,
                boughtIds = boughtIds,
                onBuy = { pendingId = it },
            )
            ShopSection(
                title = "Хочу",
                hint = "можно и завтра",
                tone = CardTone.Coin,
                items = items.filter { it.category == ItemCategory.WANTS },
                balance = balance,
                boughtIds = boughtIds,
                onBuy = { pendingId = it },
            )
        }
    }

    pendingId?.let { id ->
        items.firstOrNull { it.id == id }?.let { item ->
            PurchaseConfirmation(
                item = item,
                petName = petName,
                statAtMax = pet.isAtMax(item.stat.toDomain()),
                onConfirm = {
                    onBuy(item.id)
                    pendingId = null
                },
                onCancel = { pendingId = null },
            )
        }
    }
}

/**
 * Раздел каталога: название и подсказка в одной строке, ниже товары через
 * разделитель. Подложка обозначает учебную категорию, а название раздела
 * называет её словом (ТЗ 3.6).
 */
@Composable
private fun ShopSection(
    title: String,
    hint: String,
    tone: CardTone,
    items: List<ShopItemContent>,
    balance: Coins,
    boughtIds: Set<String>,
    onBuy: (String) -> Unit,
) {
    SectionCard(
        tone = tone,
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
        bottomSpacing = 0.dp,
    ) {
        Column {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinnyTheme.colors.onSurfaceMuted,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = FinnyTheme.colors.onSurface.copy(alpha = 0.1f))
                }
                ShopItemRow(
                    item = item,
                    balance = balance,
                    boughtToday = item.id in boughtIds,
                    onClick = { onBuy(item.id) },
                )
            }
        }
    }
}

/**
 * Строка товара. Под названием — только то, что меняет решение: сколько
 * не хватает или что товар уже куплен сегодня.
 */
@Composable
private fun ShopItemRow(
    item: ShopItemContent,
    balance: Coins,
    boughtToday: Boolean,
    onClick: () -> Unit,
) {
    val colors = FinnyTheme.colors
    val shortBy = item.price - balance.amount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(size = 44.dp) {
            PixelIcon(item.icon ?: spriteOf(item.id), cell = 2.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // После дефиса — точка переноса нулевой ширины: без неё при
            // крупном шрифте на Android 8.0 «Домик-палатка» разрывалось
            // посреди второй части слова.
            Text(item.title.replace("-", "-\u200B"), style = MaterialTheme.typography.labelLarge)
            when {
                shortBy > 0 -> Text(
                    text = "Не хватает $shortBy",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.warningText,
                )
                // Без галочки: вместе с ней надпись при ширине 360 dp
                // переносилась на вторую строку.
                boughtToday -> Text(
                    text = "куплено сегодня",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.successText,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        PriceButton(
            price = item.price,
            description = "Купить ${item.title} за ${Explanations.coinsAccusative(item.price)}",
            affordable = shortBy <= 0,
            onClick = onClick,
        )
    }
}

/**
 * Кнопка покупки с ценой. Когда монет не хватает, кнопка остаётся
 * нажимаемой, но с обводкой вместо заливки: нажатие объясняет, чего
 * не хватает, а не молчит.
 */
@Composable
private fun PriceButton(
    price: Int,
    description: String,
    affordable: Boolean,
    onClick: () -> Unit,
) {
    val colors = FinnyTheme.colors
    val container = if (affordable) colors.primary else colors.surface
    val content = if (affordable) colors.onPrimary else colors.onSurface

    Button(
        onClick = onClick,
        shape = PillShape,
        elevation = null,
        border = if (affordable) null else BorderStroke(1.5.dp, colors.outline),
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = Modifier
            .widthIn(min = 76.dp)
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clearAndSetSemantics { },
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.coin),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(price.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Значок товара — пиксельный спрайт 16 × 16 `item_<id>` (art/pixel,
 * tools/make_icons.py). Для товара без спрайта плитка остаётся пустой:
 * название написано рядом.
 */
private fun spriteOf(itemId: String): String = "item_$itemId"

/** Подтверждение покупки: последнее слово за пользователем. */
@Composable
private fun PurchaseConfirmation(
    item: ShopItemContent,
    petName: String,
    /**
     * Показатель уже на пределе, и покупка его не сдвинет. Покупку это
     * не запрещает: ограниченность ресурсов и цена необдуманной траты —
     * часть того, чему учит приложение. Но сказать об этом до списания
     * монет обязательно (ТЗ 2.2, объяснимость).
     */
    statAtMax: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    FinnyDialog(
        title = "Купить «${item.title}»?",
        onDismiss = onCancel,
        content = {
            Text(
                "Цена: ${Explanations.coins(item.price)}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Это ${if (item.category == ItemCategory.NEEDS) "нужное" else "желаемое"}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                item.effectFor(petName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (statAtMax) {
                Text(
                    text = "Но $petName и так этим доволен: монеты " +
                        "потратятся, а лучше не станет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinnyTheme.colors.warningText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        actions = {
            // Обе надписи короткие, поэтому помещаются в строку; отказ
            // слева, покупка справа — как в канве.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Отмена",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Купить",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}
