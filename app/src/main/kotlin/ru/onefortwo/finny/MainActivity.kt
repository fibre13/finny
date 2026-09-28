package ru.onefortwo.finny

import android.graphics.Color
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
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

    // Портрет на телефонах — решение по итогам проверки на устройстве, см. ниже.
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Содержимое рисуется под системными панелями, а отступы от них
        // задаёт каркас экрана. Тема светлая, поэтому значки системных
        // панелей тёмные при любой теме устройства.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // На телефоне — только портрет: в альбомной ориентации двор и
        // экраны растягиваются неудобно. На планшете (от 600 dp по короткой
        // стороне) поворот остаётся: там раскладка проверена в обеих
        // ориентациях, а ТЗ 3.1 считает это преимуществом.
        if (resources.configuration.smallestScreenWidthDp < TABLET_MIN_WIDTH_DP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent {
            val model: GameViewModel = viewModel(factory = GameViewModel.Factory(this))

            FinnyTheme {
                FinnyApp(viewModel = model)
            }
        }
    }

    private companion object {
        const val TABLET_MIN_WIDTH_DP = 600
    }
}
