package com.example.myapplication

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.BBTouchTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

// MainActivity: アプリが起動したときに最初に実行されるクラスです。
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 画面の端（ステータスバーなど）までアプリが表示されるようにします。
        enableEdgeToEdge()
        // アプリの画面内容を定義します。
        setContent {
            // テーマ名を BBTouchTheme に変更しました。
            BBTouchTheme {
                // Surfaceは背景などの土台となるコンテナです。
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 赤ちゃん向けゲームの本体を呼び出します。
                    BabyGame()
                }
            }
        }
    }
}

// 楽器アイテムのデータを表すクラスです（アイコン、色、音の種類）。
data class GameItem(
    val icon: ImageVector,
    val color: Color,
    val tone: Int
)

@Composable
fun BabyGame() {
    // remember: 画面が描き直されてもデータを捨てずに保持します。
    // ToneGenerator: Android標準の音を鳴らす機能です。
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    // アニメーションなどの非同期処理を行うためのスコープです。
    val scope = rememberCoroutineScope()
    
    // 表示するアイコンのリストです。
    val items = listOf(
        GameItem(Icons.Default.Star, Color(0xFFFFD700), ToneGenerator.TONE_DTMF_1),
        GameItem(Icons.Default.Favorite, Color(0xFFFF69B4), ToneGenerator.TONE_DTMF_2),
        GameItem(Icons.Default.Notifications, Color(0xFF00CED1), ToneGenerator.TONE_DTMF_3),
        GameItem(Icons.Default.PlayArrow, Color(0xFF32CD32), ToneGenerator.TONE_DTMF_4),
        GameItem(Icons.Default.Build, Color(0xFFFFA500), ToneGenerator.TONE_DTMF_5),
        GameItem(Icons.Default.Face, Color(0xFF9370DB), ToneGenerator.TONE_DTMF_6),
        GameItem(Icons.Default.Check, Color(0xFF4169E1), ToneGenerator.TONE_DTMF_7)
    )

    // 現在表示中のアイテムを保持する状態です。mutableStateOfを使うと、中身が変わった時に画面が自動で更新されます。
    var currentItem by remember { mutableStateOf(items.random()) }
    // 背景色の状態です。
    var bgColor by remember { mutableStateOf(Color.White) }
    // 背景色がなめらかに変わるようにアニメーション化します。
    val animatedBgColor by animateColorAsState(targetValue = bgColor, label = "bgColor")
    
    // 画面のサイズ（幅と高さ）を取得して、アイコンがはみ出さないように計算に使います。
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp.dp.value
    val screenHeightPx = configuration.screenHeightDp.dp.value
    
    // アイコンの位置 (x, y) と大きさ (scale) をアニメーションさせるための変数です。
    val xAnim = remember { Animatable(Random.nextFloat() * (screenWidthPx - 150)) }
    val yAnim = remember { Animatable(Random.nextFloat() * (screenHeightPx - 250)) }
    val scaleAnim = remember { Animatable(1f) }

    // タップされた時の動作を定義します。
    val playAndMove = {
        // ピッという音を鳴らします。
        toneGenerator.startTone(currentItem.tone, 150)
        
        scope.launch {
            // 背景を一瞬だけアイテムの色にして、すぐに白に戻します（キラキラ演出）。
            bgColor = currentItem.color.copy(alpha = 0.1f)
            // ポヨンと跳ねるような動き（Spring）で大きくしてから元に戻します。
            scaleAnim.animateTo(1.5f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scaleAnim.animateTo(1f)
            bgColor = Color.White
        }

        scope.launch {
            // アイテムをランダムに変更します。
            currentItem = items.random()
            // アイコンを新しいランダムな位置へ移動させます。
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

    // アプリが終了したり画面が切り替わったりしたときに、音の機能を安全に停止させます。
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    // 画面全体のレイアウト
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            // 画面のどこをタップしても反応するようにします。
            .pointerInput(Unit) {
                detectTapGestures(onTap = { playAndMove() })
            }
            // 画面をなぞった（スワイプした）時の動きです。
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    // なぞっている間、ランダムな確率で音を鳴らし続けます。
                    if (Random.nextInt(15) == 0) {
                         toneGenerator.startTone(currentItem.tone, 50)
                    }
                }
            }
    ) {
        // アイコンを表示する小さな箱です。
        Box(
            modifier = Modifier
                // x, y 座標をアニメーション変数と連動させます。
                .offset { IntOffset(xAnim.value.dp.roundToPx(), yAnim.value.dp.roundToPx()) }
                .size(150.dp)
                .scale(scaleAnim.value)
                .clip(CircleShape) // アイコンの周りを丸くします。
                .background(currentItem.color.copy(alpha = 0.2f))
                // アイコン自体を触ったときも反応させます。
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { playAndMove() })
                },
            contentAlignment = Alignment.Center
        ) {
            // 実際に表示されるアイコン画像です。
            Icon(
                imageVector = currentItem.icon,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = currentItem.color
            )
        }
    }
}
