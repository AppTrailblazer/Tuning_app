package com.willeypianotuning.toneanalyzer.ui.settings.weights.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.settings.weights.adapter.TuningStyleListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoadTuningStyleActivity : BaseActivity() {
    private val viewModel: LoadTuningStyleViewModel by viewModels()

    private lateinit var temperamentsRecyclerView: RecyclerView
    private val tuningStyleListAdapter = TuningStyleListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_tuning_style)
        setupToolbar()
        setTitle(R.string.activity_load_tuning_style_title)

        temperamentsRecyclerView = findViewById(R.id.temperamentsRecyclerView)
        temperamentsRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        temperamentsRecyclerView.adapter = tuningStyleListAdapter
        tuningStyleListAdapter.tuningStyleClickListener = this::onTuningStyleClicked
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(
            AppCompatResources.getDrawable(
                applicationContext,
                R.drawable.line_divider
            )!!
        )
        temperamentsRecyclerView.addItemDecoration(decoration)

        viewModel.screenState.observe(this, ::onScreenStateChanged)
        if (viewModel.screenState.value is LoadTuningStyleScreenState.Error) {
            viewModel.load()
        }
    }

    private fun onScreenStateChanged(state: LoadTuningStyleScreenState?) {
        if (state == null) {
            viewModel.load()
            return
        }

        if (state is LoadTuningStyleScreenState.Success) {
            onTuningStylesLoaded(state.styles)
        }
    }

    private fun onTuningStylesLoaded(styles: List<TuningStyle>) {
        tuningStyleListAdapter.setItems(styles)
    }

    private fun onTuningStyleClicked(style: TuningStyle) {
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(EXTRA_TUNING_STYLE, style)
            }
        )
        finish()
    }

    companion object {
        private const val EXTRA_TUNING_STYLE = "TuningStyle"
    }

    class Contract : ActivityResultContract<Void?, TuningStyle?>() {
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(context, LoadTuningStyleActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): TuningStyle? {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return null
            }
            return intent.getParcelableExtra(EXTRA_TUNING_STYLE)
        }
    }
}
