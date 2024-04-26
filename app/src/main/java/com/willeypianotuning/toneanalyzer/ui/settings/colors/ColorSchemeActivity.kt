package com.willeypianotuning.toneanalyzer.ui.settings.colors

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorSchemeActivity : BaseActivity() {

    private val viewModel: ColorSchemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColorSchemeScreen(
                viewModel = viewModel,
                onBackPressed = {
                    viewModel.saveCurrentScheme()
                    finish()
                }
            )
        }
    }
}