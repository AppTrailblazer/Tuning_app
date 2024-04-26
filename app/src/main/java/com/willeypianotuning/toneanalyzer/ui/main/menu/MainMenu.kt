package com.willeypianotuning.toneanalyzer.ui.main.menu

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import com.willeypianotuning.toneanalyzer.BuildConfig
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import com.willeypianotuning.toneanalyzer.databinding.ActivityMainMenuLayoutBinding
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme

class MainMenu(
    private val binding: ActivityMainMenuLayoutBinding,
    private val purchaseStore: PurchaseStore
) {
    private val context: Context get() = binding.menuLayout.context

    @Volatile
    var menuShown = false
        private set

    private val menuAdapter: MainMenuAdapter = MainMenuAdapter()

    val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            toggleMenu()
        }
    }

    private val adapterItemClickListener =
        AdapterView.OnItemClickListener { _, _, _, id ->
            toggleMenu()
            onItemClickListener?.onMainMenuItemClicked(id.toInt())
        }

    var onItemClickListener: OnMainMenuItemClickListener? = null

    init {
        binding.menuLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        binding.menuListView.adapter = menuAdapter
        binding.menuListView.onItemClickListener = adapterItemClickListener
        binding.menuLayout.setDebounceOnClickListener { toggleMenu() }
        binding.closeMenuButton.setOnClickListener { hideMenu() }
    }

    private fun generateItems(): List<MainMenuItem> {
        val isPro = purchaseStore.isPro
        val items = mutableListOf(
            MainMenuItem(
                MainMenuItem.ID_NEW_TUNING,
                context.getString(R.string.menu_item_new_tuning_file),
                R.drawable.ic_menu_new_file
            ),
            MainMenuItem(
                MainMenuItem.ID_OPEN_TUNING,
                context.getString(R.string.menu_item_open_tuning_file),
                R.drawable.ic_menu_open_file,
                !isPro
            ),
            MainMenuItem(
                MainMenuItem.ID_TUNING_SETTINGS,
                context.getString(R.string.menu_item_tuning_file_settings),
                R.drawable.ic_menu_file_settings
            ),
            MainMenuItem(
                MainMenuItem.ID_PITCH_RAISE,
                context.getString(R.string.menu_item_pitch_raise),
                R.drawable.ic_menu_pitch,
                !isPro
            ),
            MainMenuItem(
                MainMenuItem.ID_GLOBAL_SETTINGS,
                context.getString(R.string.menu_item_general_settings),
                R.drawable.ic_menu_global_settings
            ),
            MainMenuItem(
                MainMenuItem.ID_HELP,
                context.getString(R.string.menu_item_help),
                R.drawable.ic_menu_help
            )
        )
        val upgradeItem = when {
            purchaseStore.isProSubscription -> MainMenuItem(
                MainMenuItem.ID_UPGRADE,
                context.getString(R.string.menu_item_manage_subscription),
                R.drawable.ic_menu_unlock
            )
            BuildConfig.DEBUG -> MainMenuItem(
                MainMenuItem.ID_UPGRADE,
                context.getString(R.string.menu_item_upgrade) + " (DEBUG)",
                R.drawable.ic_menu_unlock
            )
            purchaseStore.isPro -> null
            else -> MainMenuItem(
                MainMenuItem.ID_UPGRADE,
                context.getString(R.string.menu_item_upgrade),
                R.drawable.ic_menu_unlock
            )
        }
        upgradeItem?.let { items.add(it) }
        return items
    }

    fun updateColors(colorScheme: ColorScheme) {
        binding.actionBarLayout.setBackgroundColor(colorScheme.toolbarColor)
        binding.menuLayout.setBackgroundColor(colorScheme.menuShadow)
        binding.menuListView.setBackgroundColor(colorScheme.menuPrimary)

        menuAdapter.setTextColors(colorScheme.menuTextPrimary, colorScheme.menuTextSecondary)
    }

    fun updateMenu() {
        menuAdapter.items = generateItems()
    }

    private fun showMenu() {
        if (menuShown) {
            return
        }

        binding.actionBarLayout.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.action_bar_fade_in
            )
        )
        binding.menuLayout.bringToFront()
        binding.menuLayout.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.menu_fade_in
            )
        )
        menuShown = true
        backPressedCallback.isEnabled = true
    }

    private fun hideMenu() {
        if (!menuShown) {
            return
        }

        binding.actionBarLayout.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.action_bar_fade_out
            )
        )
        binding.menuLayout.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.menu_fade_out
            )
        )
        menuShown = false
        backPressedCallback.isEnabled = false
    }

    fun toggleMenu() {
        when (menuShown) {
            true -> hideMenu()
            else -> showMenu()
        }
    }

}

fun interface OnMainMenuItemClickListener {
    fun onMainMenuItemClicked(id: Int)
}