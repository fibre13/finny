package ru.onefortwo.finny

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Единственная активность приложения. Экраны реализованы на Compose,
 * навигация между ними описана в [FinnyApp].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Содержимое рисуется под системными панелями, а отступы от них
        // задаёт каркас экрана. Тема светлая, поэтому значки системных
        // панелей тёмные при любой теме устройства.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val model: GameViewModel = viewModel(factory = GameViewModel.Factory(this))

            FinnyTheme {
                FinnyApp(viewModel = model)
            }
        }
    }
}
