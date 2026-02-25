package com.example.myapplication

import androidx.compose.ui.graphics.Color

/**
 * ゲームで使用する1つのアイテム（楽器など）のデータを保持するクラス
 * @param imageRes 画像リソースのID（R.drawable.xxx）
 * @param color タップした時の背景色や円の色
 * @param soundRes 音源リソースのID（R.raw.xxx）
 */
data class GameItem(
    val imageRes: Int,
    val color: Color,
    val soundRes: Int
)

/**
 * アプリ全体のアイテムリストを管理する設定オブジェクト
 */
object GameConfig {
    // 表示したいアイテムをこのリストに追加していきます
    val items = listOf(
        GameItem(R.drawable.tanbarin, Color(0xFF8BC34A), R.raw.tanbarin),
        GameItem(R.drawable.guiter, Color(0xFFF44336), R.raw.guiter_scrach)
    )
}
