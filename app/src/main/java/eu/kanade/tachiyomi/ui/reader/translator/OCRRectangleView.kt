package eu.kanade.tachiyomi.ui.reader.translator

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.max
import kotlin.math.min

class OCRRectangleView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val rectOCR = RectF()
    private val darkPaint = Paint()
    private val bluePaint = Paint()
    private var x1 = 0f
    private var y1 = 0f
    private var x0 = 0f
    private var y0 = 0f
    private var isRectangleDone = false
    private var duration: Long = 0
    private var hasMoved = false
    lateinit var longTapCallback: (RectF) -> Unit

    private enum class MovingCorner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    private var moving: MovingCorner? = null

    init {
        darkPaint.color = Color.BLACK
        darkPaint.alpha = 100
        bluePaint.color = Color.BLUE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, rectOCR.left, canvas.height.toFloat(), darkPaint)
        canvas.drawRect(rectOCR.left, 0f, canvas.width.toFloat(), rectOCR.top, darkPaint)
        canvas.drawRect(rectOCR.right, rectOCR.top, canvas.width.toFloat(), canvas.height.toFloat(), darkPaint)
        canvas.drawRect(rectOCR.left, rectOCR.bottom, rectOCR.right, canvas.height.toFloat(), darkPaint)
        if (isRectangleDone) {
            canvas.drawCircle(rectOCR.left, rectOCR.top, (width / 30).toFloat(), bluePaint)
            canvas.drawCircle(rectOCR.right, rectOCR.top, (width / 30).toFloat(), bluePaint)
            canvas.drawCircle(rectOCR.left, rectOCR.bottom, (width / 30).toFloat(), bluePaint)
            canvas.drawCircle(rectOCR.right, rectOCR.bottom, (width / 30).toFloat(), bluePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isRectangleDone) {
            return when (event.actionMasked) {
                ACTION_DOWN -> {
                    hasMoved = false
                    val x = event.x
                    val y = event.y
                    if (!rectOCR.contains(x, y)) {
                        x1 = x
                        y1 = y
                        return true
                    }
                    false
                }
                ACTION_MOVE -> {
                    val x2 = event.x
                    val y2 = event.y
                    if (!hasMoved && PointF(x2 - x1, y2 - y1).length() > width / 30) {
                        hasMoved = true
                    }
                    rectOCR.set(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
                    invalidate()
                    true
                }
                ACTION_UP -> {
                    if (hasMoved) {
                        isRectangleDone = true
                        invalidate()
                    }
                    true
                }
                else -> false
            }
        } else {
            return when (event.actionMasked) {
                ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y
                    if (PointF(x - rectOCR.left, y - rectOCR.top).length() < width / 30) {
                        moving = MovingCorner.TOP_LEFT
                        return true
                    }

                    if (PointF(x - rectOCR.right, y - rectOCR.top).length() < width / 30) {
                        moving = MovingCorner.TOP_RIGHT
                        return true
                    }

                    if (PointF(x - rectOCR.left, y - rectOCR.bottom).length() < width / 30) {
                        moving = MovingCorner.BOTTOM_LEFT
                        return true
                    }

                    if (PointF(x - rectOCR.right, y - rectOCR.bottom).length() < width / 30) {
                        moving = MovingCorner.BOTTOM_RIGHT
                        return true
                    }

                    if (rectOCR.contains(x, y)) {
                        hasMoved = false
                        duration = System.currentTimeMillis()
                        x1 = x
                        y1 = y
                        x0 = x
                        y0 = y
                        return true
                    }
                    false
                }
                ACTION_MOVE -> {
                    val x2 = event.x
                    val y2 = event.y
                    if (!hasMoved && PointF(x2 - x0, y2 - y0).length() > width / 30) {
                        hasMoved = true
                    }
                    when (moving) {
                        MovingCorner.TOP_LEFT -> {
                            rectOCR.top = min(y2, rectOCR.bottom)
                            rectOCR.left = min(x2, rectOCR.right)
                            invalidate()
                            return true
                        }
                        MovingCorner.TOP_RIGHT -> {
                            rectOCR.top = min(y2, rectOCR.bottom)
                            rectOCR.right = max(x2, rectOCR.left)
                            invalidate()
                            return true
                        }
                        MovingCorner.BOTTOM_LEFT -> {
                            rectOCR.bottom = max(y2, rectOCR.top)
                            rectOCR.left = min(x2, rectOCR.right)
                            invalidate()
                            return true
                        }
                        MovingCorner.BOTTOM_RIGHT -> {
                            rectOCR.bottom = max(y2, rectOCR.top)
                            rectOCR.right = max(x2, rectOCR.left)
                            invalidate()
                            return true
                        }
                    }
                    rectOCR.offset(x2 - x1, y2 - y1)
                    x1 = x2
                    y1 = y2
                    invalidate()
                    true
                }
                ACTION_UP -> {
                    moving = null
                    if (!hasMoved && System.currentTimeMillis() - duration >= ViewConfiguration.getLongPressTimeout()) {
                        val region = RectF(rectOCR)
                        rectOCR.set(0f, 0f, 0f, 0f)
                        isRectangleDone = false
                        longTapCallback(region)
                    }
                    duration = 0
                    return false
                }
                else -> false
            }
        }
    }
}
