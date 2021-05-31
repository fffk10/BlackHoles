package com.example.blackholes

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SensorEventListener, SurfaceHolder.Callback {
    private val tag = "Black Holes"
    private val matrixSize = 16
    private val numOfBlackHole = 5
    private val radius = 30f   // ボールの半径
    private val limitOfBlackHole = 100

    private var mgValues = FloatArray(3)
    private var acValues = FloatArray(3)
    private var startTime: Long = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var ballX = 0f
    private var ballY = 0f
    private var isGoal = false
    private var isGone = false

    private val blackHolesList = ArrayList<BlackHole>()

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    /**
     * センサの変更を検知して実行
     */
    override fun onSensorChanged(event: SensorEvent?) {
        val inR = FloatArray(matrixSize)
        val outR = FloatArray(matrixSize)
        val I = FloatArray(matrixSize)
        val orValues = FloatArray(3)

        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> acValues = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> mgValues = event.values.clone()
        }

        SensorManager.getRotationMatrix(inR, I, acValues, mgValues)

        // 携帯を水平に持ち、アクティビティはポートレイト
        SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR)
        SensorManager.getOrientation(outR, orValues)

        // ラジアンを角度に変えて、pitchとrollを取得
        val pitch = rad2Deg(orValues[1])
        val roll = rad2Deg(orValues[2])
        Log.v(tag, "pitch${pitch}")
        Log.v(tag, "pitch${roll}")

        // ゴールしていないかブラックホールに落ちていない場合に盤面を描画
        if (!isGoal && !isGone) {
            drawGameBoard(pitch, roll)
        }
    }

    private fun rad2Deg(rad: Float): Int {
        return Math.floor(Math.toDegrees(rad.toDouble())).toInt()
    }

    /**
     * surfaceCreated()メソッドの次に呼び出される
     * 引数としてサーフェスビューの幅（width）と高さ（height）が渡ってくる
     * それぞれの変数をsurfaceWidthとsurfaceHeightに保存した後、ボールの座標を求める使う
     * その後onSensorChanged()でセンサーの変更を検知
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        ballX = (width / 2).toFloat()
        ballY = (height - radius).toFloat()
        // 経過時間を計るために開始時間に現在時間をセット
        startTime = System.currentTimeMillis()
        // ブラックホールを生成
        createBlackHoles()
    }

    /**
     * ブラックホールを作成する
     */
    private fun createBlackHoles() {
        for (i in 1..numOfBlackHole) {
            val x: Float = (limitOfBlackHole..surfaceWidth - limitOfBlackHole).random().toFloat()
            val y: Float = (limitOfBlackHole..surfaceHeight - limitOfBlackHole).random().toFloat()
            val speed: Int = (2..11).random()
            val bh = BlackHole(x, y, speed)
            blackHolesList.add(bh)
        }
    }

    /**
     * ゲームの盤面とボールを描画
     */
    private fun drawGameBoard(pitch: Int, roll: Int) {
        ballX += roll
        ballY -= pitch
        // 左右は逆から
        if (ballX < 0) {    // 左 -> 右
            ballX = surfaceWidth - radius
        } else if (ballX > surfaceWidth) {    // 右 -> 左
            ballX = radius
        }
        // 上はゴール、下は落ない
        if (ballY + radius < 0) {
            isGoal = true
        } else if (ballY + radius > surfaceHeight) {
            ballY = surfaceHeight - radius
        }
        // ブラックホールに飲み込まれたか
        for (bh in blackHolesList) {
            if (checkGone(bh.x, bh.y, bh.r)) {
                isGone = true
            }
        }

        // SurfaceViewをロック
        val canvas = surfaceView.holder.lockCanvas()

        val paint = Paint()
        // 背景を塗る
        canvas.drawColor(Color.BLUE)
        paint.color = Color.BLACK
        for (bh in blackHolesList) {
            canvas.drawCircle(bh.x, bh.y, bh.r, paint)
            bh.grow()
        }
        paint.color = Color.RED
        if (!isGone) {
            canvas.drawCircle(ballX, ballY, radius, paint)
        }

        // ボールがゴールに到達した時は経過時間を描画
        if (isGoal) {
            paint.textSize = 80f
            canvas.drawText(goaled(), 10f, (surfaceHeight - 60).toFloat(), paint)
        }

        // アンロックしてSurfaceViewを更新
        // lockCanvas()とunlockCanvasAndPost()はペアで実行する必要がある
        surfaceView.holder.unlockCanvasAndPost(canvas)
    }

    /**
     * ブラックホールに飲み込まれたかを判定
     */
    private fun checkGone(x0: Float, y0: Float, r: Float): Boolean {
        return (x0 - ballX) * (x0 - ballX) + (y0 - ballY) * (y0 - ballY) < r * r
    }

    private fun goaled(): String {
        // 経過時間
        val elapsedTime = System.currentTimeMillis() - startTime
        val secTime = (elapsedTime / 1000).toInt()
        return "Goal!! ${secTime}秒"
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)
    }

    /**
     * 加速度センサーと地磁気センサーを取得してリスナー登録
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, magField, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // インスタンスを変数に格納
        val holder = surfaceView.holder

        // this(自分自身)をsurfaceViewイベントの通知先に指定
        // 次にsurfaceCreated()メソッドが呼び出される
        holder.addCallback(this)
    }

}