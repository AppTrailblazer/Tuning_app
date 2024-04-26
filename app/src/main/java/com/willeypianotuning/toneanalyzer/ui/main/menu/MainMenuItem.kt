package com.willeypianotuning.toneanalyzer.ui.main.menu

data class MainMenuItem(
    val id: Int,
    val title: String,
    val image: Int,
    val showLock: Boolean = false
) {
    companion object {
        const val ID_NEW_TUNING = 1
        const val ID_OPEN_TUNING = 2
        const val ID_TUNING_SETTINGS = 3
        const val ID_GLOBAL_SETTINGS = 4
        const val ID_PITCH_RAISE = 5
        const val ID_HELP = 6
        const val ID_UPGRADE = 7
    }
}