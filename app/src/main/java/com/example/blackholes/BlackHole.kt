package com.example.blackholes

/**
 * ブラックホールのクラス
 * @param x x座標
 * @param y y座標
 * @param speed 拡大縮小するスピード
 */
class BlackHole(val x: Float, val y: Float, val speed: Int) {
    /**
     * 半径の最大値
     */
    val MAX = 400f

    /**
     * 半径の最小値
     */
    val MIN = 30f

    /**
     * 半径
     */
    var r = 30f

    /**
     * 正負を表す
     */
    var sign = 1

    /**
     * ブラックホールの大きさを変更する
     */
    fun grow() {
        if (r > MAX) {
            sign = -1
        } else if (r < MIN) {
            sign = 1
        }
        r += (speed * sign).toFloat()
    }
}