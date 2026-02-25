package com.example.myapplication

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.BBTouchTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BBTouchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BabyGame()
                }
            }
        }
    }
}

@Composable
fun BabyGame() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val soundPool = remember {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()
    }

    val soundIdMap = remember { mutableStateMapOf<Int, Int>() }
    val items = GameConfig.items

    LaunchedEffect(items) {
        items.forEach { item ->
            if (item.soundRes != 0 && !soundIdMap.containsKey(item.soundRes)) {
                soundIdMap[item.soundRes] = soundPool.load(context, item.soundRes, 1)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPool.release()
        }
    }

    // --- 状態管理 ---
    var currentItem by remember { mutableStateOf(items.random()) }
    var bgColor by remember { mutableStateOf(Color.White) }
    val animatedBgColor by animateColorAsState(targetValue = bgColor, label = "bgColor")
    
    var currentStreamId by remember { mutableIntStateOf(0) }
    
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp.dp.value
    val screenHeightPx = configuration.screenHeightDp.dp.value
    
    val xAnim = remember { Animatable(Random.nextFloat() * (screenWidthPx - 150)) }
    val yAnim = remember { Animatable(Random.nextFloat() * (screenHeightPx - 250)) }
    val scaleAnim = remember { Animatable(1f) }

    val moveRandomly = {
        scope.launch {
            xAnim.animateTo(
                Random.nextFloat() * (screenWidthPx - 150).coerceAtLeast(0f),
                spring(stiffness = Spring.StiffnessLow)
            )
        }
        scope.launch {
            yAnim.animateTo(
                Random.nextFloat() * (screenHeightPx - 250).coerceAtLeast(0f),
                spring(stiffness = Spring.StiffnessLow)
            )
        }
    }

    val handleTap = {
        if (currentStreamId != 0) {
            soundPool.stop(currentStreamId)
        }
        val soundId = soundIdMap[currentItem.soundRes]
        if (soundId != null && soundId != 0) {
            currentStreamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
        
        scope.launch {
            bgColor = currentItem.color.copy(alpha = 0.1f)
            scaleAnim.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scaleAnim.animateTo(1f)
            bgColor = Color.White
        }
        moveRandomly()
    }

    val handleSwipe = {
        if (currentStreamId != 0) {
            soundPool.stop(currentStreamId)
        }
        val nextItems = items.filter { it != currentItem }
        if (nextItems.isNotEmpty()) {
            currentItem = nextItems.random()
        }
        moveRandomly()
    }

    // --- レイアウト ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            // 背景部分でスワイプ（ドラッグ）を検知するように変更
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(totalDrag) > 100f) {
                            handleSwipe()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
    ) {
        // アイコン（GameItem）
        Box(
            modifier = Modifier
                .offset { IntOffset(xAnim.value.dp.roundToPx(), yAnim.value.dp.roundToPx()) }
                .size(150.dp)
                .scale(scaleAnim.value)
                .clip(CircleShape)
                .background(currentItem.color.copy(alpha = 0.2f))
                // アイコンの範囲内ではタップのみを検知
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { handleTap() })
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = currentItem.imageRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }
    }
}
