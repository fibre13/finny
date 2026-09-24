package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.ShopItemContent
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.PetState
import ru.onefortwo.finny.economy.PurchaseRecord
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Каталог покупок (ТЗ 2.5.6).
 *
 * Перед покупкой видны цена, категория и предполагаемое влияние на питомца.
 * Покупка требует отдельного подтверждения. Покупка при недостатке монет
 * не выполняется, вместо этого приложение объясняет, чего не хватает.
 */
@Composable
fun ShopScreen(
    items: List<ShopItemContent>,
    /** Состояние питомца: нужно, чтобы предупредить о покупке без пользы. */
    pet: PetState,
    balance: Coins,
    /**
     * Покупки текущего дня. Перечислены списком, а не одним сообщением:
     * сообщение показывает только последнее действие. Список перенесён
     * сюда с главного экрана, чтобы главная помещалась без прокрутки.
     */
    todayPurchases: List<PurchaseRecord> = emptyList(),
    message: FeedbackMessage?,
    onDismissMessage: () -> Unit,
    onBuy: (String) -> Unit,
    onBack: () -> Unit,
) {
    // Хранится идентификатор, а не сама позиция: строка переживает поворот
    // экрана, позиция восстанавливается из каталога.
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold(
        eyebrow = "Для питомца",
        title = "Покупки",
        balance = balance,
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
    ) {
        Column {
            SectionCard {
                LabeledValue("Можно потратить", Explanations.coins(balance))
            }

            if (todayPurchases.isNotEmpty()) {
                SectionCard(title = "Сегодня куплено") {
                    Column {
                        todayPurchases.forEach { record ->
                            LabeledValue(
                                label = items.firstOrNull { it.id == record.itemId }?.title ?: record.itemId,
                                value = Explanations.coins(record.price),
                            )
                        }
                        LabeledValue(
                            label = "Всего потрачено",
                            value = Explanations.coins(
                                todayPurchases.fold(Coins.ZERO) { sum, it -> sum + it.price },
                            ),
                        )
                    }
                }
            }

            Text(
                text = "Нужное",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            SupportingText(
                text = "Без этого питомец грустит. Покупай это в первую очередь.",
                modifier = Modifier.padding(bottom = 12.dp),
            )
            items.filter { it.category == ItemCategory.NEEDS }.forEach { item ->
                ShopItemCard(item = item, balance = balance, onClick = { pendingId = item.id })
            }

            Text(
                text = "Хочу",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
            )
            SupportingText(
                text = "Это радует питомца. Можно купить сейчас, а можно отложить на завтра.",
                modifier = Modifier.padding(bottom = 12.dp),
            )
            items.filter { it.category == ItemCategory.WANTS }.forEach { item ->
                ShopItemCard(item = item, balance = balance, onClick = { pendingId = item.id })
            }
        }
    }

    pendingId?.let { id ->
        items.firstOrNull { it.id == id }?.let { item ->
            PurchaseConfirmation(
                item = item,
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

/** Карточка позиции: цена, категория и влияние на питомца до покупки. */
@Composable
private fun ShopItemCard(
    item: ShopItemContent,
    balance: Coins,
    onClick: () -> Unit,
) {
    val affordable = balance.amount >= item.price
    val needs = item.category == ItemCategory.NEEDS

    // Подложка обозначает учебную категорию; подпись «Это — нужное»
    // в карточке остаётся: цвет только помогает её узнать (ТЗ 3.6).
    SectionCard(
        title = item.title,
        tone = if (needs) CardTone.Sage else CardTone.Coin,
        trailing = {
            BudgetDirectionIcon(
                category = if (needs) BudgetCategory.NEEDS else BudgetCategory.WANTS,
                size = 32.dp,
            )
        },
    ) {
        Column {
            Text(
                text = Explanations.coins(item.price),
                style = MaterialTheme.typography.headlineSmall,
            )
            LabeledValue(
                label = "Это",
                value = if (needs) "нужное" else "желаемое",
            )
            SupportingText(item.effect)

            if (!affordable) {
                Text(
                    text = "Не хватает ${Explanations.coins(item.price - balance.amount)}.",
                    style = MaterialTheme.typography.titleMedium,
                    color = FinnyTheme.colors.warningText,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            PrimaryButton(
                text = "Купить",
                onClick = onClick,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/** Подтверждение покупки: последнее слово за пользователем. */
@Composable
private fun PurchaseConfirmation(
    item: ShopItemContent,
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
                item.effect,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (statAtMax) {
                Text(
                    text = "Но этот показатель у Финни уже полный: монеты " +
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
