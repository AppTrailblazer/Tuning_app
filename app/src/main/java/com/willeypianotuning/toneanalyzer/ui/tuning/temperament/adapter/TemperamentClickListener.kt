package com.willeypianotuning.toneanalyzer.ui.tuning.temperament.adapter

import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament

interface TemperamentClickListener {
    fun onTemperamentClicked(temperament: Temperament)
}