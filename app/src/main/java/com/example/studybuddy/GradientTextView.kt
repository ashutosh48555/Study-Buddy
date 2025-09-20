package com.example.studybuddy

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.studybuddy.R

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var gradient: LinearGradient? = null
    private var gradientColors: IntArray = intArrayOf()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        gradientColors = if (isDark) {
            intArrayOf(
                ContextCompat.getColor(context, R.color.primaryLightColor),
                ContextCompat.getColor(context, R.color.secondaryColor),
                ContextCompat.getColor(context, R.color.primaryColor)
            )
        } else {
            intArrayOf(
                ContextCompat.getColor(context, R.color.primaryDarkColor),
                ContextCompat.getColor(context, R.color.secondaryDarkColor),
                ContextCompat.getColor(context, R.color.primaryColor)
            )
        }
        gradient = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            gradientColors, null, Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (gradient == null) {
            updateGradient()
        }
        super.onDraw(canvas)
    }
} 