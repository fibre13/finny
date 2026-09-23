package ru.onefortwo.finny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Единственная активность приложения. Экраны реализованы на Compose,
 * навигация между ними описана в [FinnyApp].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Модель создаётся до темы, а не внутри неё: оформление
            // выбирается взрослым и хранится на устройстве, поэтому тема
            // зависит от прочитанных настроек.
            val model: GameViewModel = viewModel(factory = GameViewModel.Factory(this))
            val display by model.display.collectAsStateWithLifecycle()

            FinnyTheme(settings = display) {
                FinnyApp(viewModel = model)
            }
        }
    }
}
