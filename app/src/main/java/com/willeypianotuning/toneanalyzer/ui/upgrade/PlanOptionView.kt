package com.willeypianotuning.toneanalyzer.ui.upgrade

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import com.willeypianotuning.toneanalyzer.R

class PlanOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var iconCheck: ImageView
    private var optionTitleTextView: TextView
    private var optionDescriptionTextView: TextView

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.upgrade_plan_option, this, true)
        iconCheck = findViewById(R.id.iconCheck)
        optionTitleTextView = findViewById(R.id.planOptionTitleTextView)
        optionDescriptionTextView = findViewById(R.id.planOptionDescriptionTextView)

        if (attrs != null) {
            processAttributes(attrs)
        }

        setOnClickListener {
            switchDescription()
        }
    }

    private fun processAttributes(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PlanOptionView)

        optionTitleTextView.text = ta.getText(R.styleable.PlanOptionView_po_title)
        optionDescriptionTextView.text = ta.getText(R.styleable.PlanOptionView_po_description)
        val expanded = ta.getBoolean(R.styleable.PlanOptionView_po_expanded, false)
        optionDescriptionTextView.visibility = if (expanded) View.VISIBLE else View.GONE
        val arrowDrawable = AppCompatResources.getDrawable(
            context,
            if (expanded) R.drawable.ic_arrow_drop_up_white_24dp else R.drawable.ic_arrow_drop_down_white_24dp
        )
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
            optionTitleTextView,
            null,
            null,
            arrowDrawable,
            null
        )

        setOptionEnabled(ta.getBoolean(R.styleable.PlanOptionView_po_enabled, false))

        ta.recycle()
    }

    fun setOptionEnabled(enabled: Boolean) {
        val drawableId = if (enabled) {
            R.drawable.ic_check_green_24dp
        } else {
            R.drawable.ic_check_white_24dp
        }
        iconCheck.setImageDrawable(AppCompatResources.getDrawable(context, drawableId))
    }

    private fun switchDescription() {
        if (optionDescriptionTextView.visibility == View.GONE) {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                optionTitleTextView,
                null,
                null,
                AppCompatResources.getDrawable(context, R.drawable.ic_arrow_drop_up_white_24dp),
                null
            )
            expand(optionDescriptionTextView)
        } else {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                optionTitleTextView,
                null,
                null,
                AppCompatResources.getDrawable(context, R.drawable.ic_arrow_drop_down_white_24dp),
                null
            )
            collapse(optionDescriptionTextView)
        }
    }

    companion object {
        private fun expand(v: View) {
            val matchParentMeasureSpec =
                MeasureSpec.makeMeasureSpec((v.parent as View).width, MeasureSpec.EXACTLY)
            val wrapContentMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
            val targetHeight = v.measuredHeight
            // Older versions of android (pre API 21) cancel animations for views with a height of 0.
            v.clearAnimation()
            v.layoutParams.height = 1
            v.visibility = View.VISIBLE
            val a: Animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    v.layoutParams.height = if (interpolatedTime == 1f)
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    else
                        (targetHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }
            // Expansion speed of 1dp/ms
            a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
            v.startAnimation(a)
        }

        private fun collapse(v: View) {
            v.clearAnimation()
            val initialHeight = v.measuredHeight
            val a: Animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    if (interpolatedTime == 1f) {
                        v.visibility = View.GONE
                    } else {
                        v.layoutParams.height =
                            initialHeight - (initialHeight * interpolatedTime).toInt()
                        v.requestLayout()
                    }
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }
            // Collapse speed of 1dp/ms
            a.duration = (initialHeight / v.context.resources.displayMetrics.density).toLong()
            v.startAnimation(a)
        }
    }

}