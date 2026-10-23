package id.rdpfread.remote.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.rdpfread.remote.R
import id.rdpfread.remote.ui.theme.XyDisplay
import id.rdpfread.remote.ui.theme.XyThemeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen RDPfread XD (soft neobrutalism): kartu ikon launcher kuning
 * di atas kertas, wordmark "RDPfread", bilah progres kuning. Mekanik tampil
 * sekali saat cold start sama seperti sebelumnya.
 */
@Composable
internal fun XySplashScreen(
    ready: Boolean = true,
    dark: Boolean = when (XyThemeState.mode) {
        XyThemeState.DARK -> true
        XyThemeState.LIGHT -> false
        else -> isSystemInDarkTheme()
    },
    onDone: () -> Unit = {},
    onFinished: () -> Unit = onDone,
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.95f) }
    val barWidth = remember { Animatable(0f) }
    val minTimeDone = remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
        }
        launch {
            barWidth.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }
        delay(360)
        minTimeDone.value = true
    }

    LaunchedEffect(ready, minTimeDone.value) {
        if (ready && minTimeDone.value) {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            )
            onFinished()
        }
    }

    val paper = Color(0xFFFFFDF7)
    val ink = Color(0xFF161616)
    val yellow = Color(0xFFFFD02E)
    val bg = if (dark) Color(0xFF14161A) else paper
    val wordColor = if (dark) paper else ink
    val subColor = if (dark) paper.copy(alpha = 0.55f) else ink.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha.value)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(yellow)
                    .border(3.dp, ink, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                )
            }
            Text(
                text = "RDPfread",
                style = TextStyle(
                    fontFamily = XyDisplay,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = wordColor,
                ),
            )
            Text(
                text = "REMOTE DESKTOP",
                style = TextStyle(
                    fontFamily = XyDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.5.sp,
                    color = subColor,
                ),
            )
            Box(
                modifier = Modifier
                    .width((72f * barWidth.value).dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(yellow),
            )
        }
    }
}
