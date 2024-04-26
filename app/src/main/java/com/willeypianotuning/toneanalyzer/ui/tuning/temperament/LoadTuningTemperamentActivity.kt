package com.willeypianotuning.toneanalyzer.ui.tuning.temperament

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.store.TemperamentDataStore
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.tuning.temperament.adapter.TemperamentClickListener
import com.willeypianotuning.toneanalyzer.ui.tuning.temperament.adapter.TemperamentListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoadTuningTemperamentActivity : BaseActivity(), TemperamentClickListener {
    @Inject
    lateinit var temperamentDataStore: TemperamentDataStore

    private val temperamentsRecyclerView by lazy { findViewById<RecyclerView>(R.id.temperamentsRecyclerView) }
    private val temperamentListAdapter = TemperamentListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_tuning_temperament)
        setupToolbar()
        setTitle(R.string.activity_load_tuning_temperament_title)

        temperamentsRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        temperamentsRecyclerView.adapter = temperamentListAdapter
        temperamentListAdapter.setTemperamentClickListener(this)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(
            AppCompatResources.getDrawable(
                applicationContext,
                R.drawable.line_divider
            )!!
        )
        temperamentsRecyclerView.addItemDecoration(decoration)

        loadTemperaments()
    }

    private fun loadTemperaments() {
        lifecycleScope.launch {
            kotlin.runCatching {
                val temperaments = withContext(Dispatchers.IO) {
                    temperamentDataStore.allTemperaments()
                }
                withContext(Dispatchers.Main) {
                    onTemperamentsLoaded(temperaments)
                }
            }.onFailure {
                Timber.e(it, "Cannot load temperaments")
            }
        }
    }

    private fun onTemperamentsLoaded(temperaments: List<Temperament>) {
        temperamentListAdapter.setItems(temperaments)
    }

    override fun onTemperamentClicked(temperament: Temperament) {
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(EXTRA_TEMPERAMENT, temperament)
            }
        )
        finish()
    }

    companion object {
        private const val EXTRA_TEMPERAMENT = "Temperament"
    }

    class Contract : ActivityResultContract<Void?, Temperament?>() {
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(context, LoadTuningTemperamentActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Temperament? {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return null
            }
            return intent.getParcelableExtra(EXTRA_TEMPERAMENT)
        }
    }
}
