package ru.onefortwo.finny

import android.graphics.Color
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.onefortwo.finny.ui.common.TouchMarks
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
        // Отметки касаний для видеозаписи: только в отладочной сборке и при
        // включённом системном параметре «Показывать нажатия».
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val touchMarks = debuggable && Settings.System.getInt(contentResolver, "show_touches", 0) == 1
        setContent {
            val model: GameViewModel = viewModel(factory = GameViewModel.Factory(this))

            FinnyTheme {
                TouchMarks(enabled = touchMarks, modifier = Modifier.fillMaxSize()) {
                    FinnyApp(viewModel = model)
                }
            }
        }
    }

    private companion object {
        const val TABLET_MIN_WIDTH_DP = 600
    }
}
