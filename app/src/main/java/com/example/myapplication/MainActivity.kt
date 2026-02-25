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
            // アプリ全体のテーマを適用
            BBTouchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // メインのゲーム画面を呼び出し
                    BabyGame()
                }
            }
        }
    }
}

@Composable
fun BabyGame() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // 非同期処理（アニメーションなど）を動かすためのスコープ
    
    // SoundPool: 短い音を遅延なく鳴らすための仕組み
    val soundPool = remember {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(5) // 同時に鳴らせる最大数
            .setAudioAttributes(attributes)
            .build()
    }

    // 音源リソースID(R.raw.xxx)と、SoundPoolでの読み込み後IDを紐付けるマップ
    val soundIdMap = remember { mutableStateMapOf<Int, Int>() }
    
    // GameItems.kt で定義したアイテムリストを取得
    val items = GameConfig.items

    // LaunchedEffect: 画面が表示された時に一度だけ実行される「準備」の処理
    LaunchedEffect(items) {
        items.forEach { item ->
            // 音源をメモリにロードしておく（これでタップ時にすぐ鳴るようになる）
            if (item.soundRes != 0 && !soundIdMap.containsKey(item.soundRes)) {
                soundIdMap[item.soundRes] = soundPool.load(context, item.soundRes, 1)
            }
        }
    }

    // DisposableEffect: 画面が閉じられる時などに実行される「後片付け」の処理
    DisposableEffect(Unit) {
        onDispose {
            soundPool.release() // メモリ解放
        }
    }

    // --- 状態（データ）の管理 ---
    // 現在選ばれているアイテム
    var currentItem by remember { mutableStateOf(items.random()) }
    // 背景色（アニメーション用）
    var bgColor by remember { mutableStateOf(Color.White) }
    val animatedBgColor by animateColorAsState(targetValue = bgColor, label = "bgColor")
    
    // 画面サイズを取得して、アイコンが画面外に出ないように計算に使う
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp.dp.value
    val screenHeightPx = configuration.screenHeightDp.dp.value
    
    // 位置とサイズのアニメーション用変数（Animatable）
    val xAnim = remember { Animatable(Random.nextFloat() * (screenWidthPx - 150)) }
    val yAnim = remember { Animatable(Random.nextFloat() * (screenHeightPx - 250)) }
    val scaleAnim = remember { Animatable(1f) }

    // --- 動作の定義 ---

    // 1. ランダムな位置へ移動する処理
    val moveRandomly = {
        scope.launch {
            xAnim.animateTo(
                Random.nextFloat() * (screenWidthPx - 150).coerceAtLeast(0f),
                spring(stiffness = Spring.StiffnessLow) // ゆっくりふわっと動くバネの動き
            )
        }
        scope.launch {
            yAnim.animateTo(
                Random.nextFloat() * (screenHeightPx - 250).coerceAtLeast(0f),
                spring(stiffness = Spring.StiffnessLow)
            )
        }
    }

    // 2. タップされた時の処理
    val handleTap = {
        // ロードしておいた音を再生
        val soundId = soundIdMap[currentItem.soundRes]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
        
        // 演出アニメーション（色を少し変えて、アイコンを大きくする）
        scope.launch {
            bgColor = currentItem.color.copy(alpha = 0.1f)
            scaleAnim.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scaleAnim.animateTo(1f)
            bgColor = Color.White
        }
        // 位置を移動
        moveRandomly()
    }

    // 3. スワイプされた時の処理
    val handleSwipe = {
        // 現在のアイテム以外のものからランダムに選ぶ
        val nextItems = items.filter { it != currentItem }
        if (nextItems.isNotEmpty()) {
            currentItem = nextItems.random()
        }
        // 位置を移動
        moveRandomly()
    }

    // --- 画面のレイアウト（UI） ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            // 画面全体のタップを検知
            .pointerInput(Unit) {
                detectTapGestures(onTap = { handleTap() })
            }
            // 画面全体の左右スワイプを検知
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount // どのくらいスワイプしたか蓄積
                    },
                    onDragEnd = {
                        // 一定距離（100px）以上スワイプして指を離したらアイテム変更
                        if (kotlin.math.abs(totalDrag) > 100f) {
                            handleSwipe()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
    ) {
        // アイコンを表示するBox
        Box(
            modifier = Modifier
                .offset { IntOffset(xAnim.value.dp.roundToPx(), yAnim.value.dp.roundToPx()) }
                .size(150.dp)
                .scale(scaleAnim.value) // アニメーションする大きさを適用
                .clip(CircleShape) // 円形に切り抜き
                .background(currentItem.color.copy(alpha = 0.2f)) // ほんのり背景色
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { handleTap() }) // アイコン自体をタップした時も反応
                },
            contentAlignment = Alignment.Center
        ) {
            // 画像の表示
            Image(
                painter = painterResource(id = currentItem.imageRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }
    }
}
