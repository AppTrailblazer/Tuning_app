package com.willeypianotuning.toneanalyzer.ui.settings.weights.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.extensions.dpToPx

class IntervalsLabelRenderer(
    context: Context,
    private val textColor: Int = ContextCompat.getColor(
        context,
        R.color.tuning_style_label_disabled
    ),
    private val textSizeDp: Float,
) : IShapeRenderer {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizeDp.dpToPx(context.resources)
        color = textColor
    }
    private val textBounds = Rect()

    override fun renderShape(
        c: Canvas,
        dataSet: IScatterDataSet,
        viewPortHandler: ViewPortHandler,
        posX: Float,
        posY: Float,
        renderPaint: Paint
    ) {
        renderPaint.style = Paint.Style.FILL

        val label = dataSet.getEntryForIndex(0).data as String
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        c.drawText(
            label,
            posX,
            posY + textBounds.height() / 2,
            textPaint
        )
    }

}