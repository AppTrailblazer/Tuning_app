package com.willeypianotuning.toneanalyzer.ui.help

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.databinding.ActivityHelpBinding
import com.willeypianotuning.toneanalyzer.utils.LocaleUtils
import net.cachapa.expandablelayout.ExpandableLayout

class HelpActivity : AppCompatActivity() {
    private val titleToContentMap: Map<TextView, ExpandableLayout>
        get() = mapOf(
            binding.titleGettingStarted to binding.gettingStartedContent,
            binding.titleTuningDisplay to binding.tuningDisplayContent,
            binding.titleTuningOtherFrequencies to binding.tuningOtherFrequenciesContent,
            binding.titleCalibration to binding.calibrationContent,
            binding.titleTuningFiles to binding.tuningFilesContent,
            binding.titleLockingTuning to binding.lockingTuningContent,
            binding.titleNoteSwitching to binding.noteSwitchingContent,
            binding.titleGraphingArea to binding.graphingAreaContent,
            binding.titlePitchRaise to binding.pitchRaiseContent,
            binding.titleTuningStyles to binding.tuningStylesContent
        )

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateArrows()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateArrows() {
        for ((title, content) in titleToContentMap.entries) {
            val drawableRes = if (content.isExpanded) {
                R.drawable.ic_arrow_drop_up_white_24dp
            } else {
                R.drawable.ic_arrow_drop_down_white_24dp
            }
            setRightDrawable(title, drawableRes)
        }
    }

    private fun setRightDrawable(textView: TextView, drawableRes: Int) {
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
            textView,
            null,
            null,
            AppCompatResources.getDrawable(this, drawableRes),
            null
        )
    }

    fun onTitlePressed(arg: View) {
        val target = titleToContentMap[arg] ?: return

        val textView: TextView = arg as TextView
        if (target.isExpanded) {
            target.collapse(true)
            setRightDrawable(textView, R.drawable.ic_arrow_drop_down_white_24dp)
        } else {
            target.expand(true)
            setRightDrawable(textView, R.drawable.ic_arrow_drop_up_white_24dp)
        }
    }
}