package com.willeypianotuning.toneanalyzer.ui.files

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.databinding.ActivityFilesBinding
import com.willeypianotuning.toneanalyzer.extensions.forceShowIcons
import com.willeypianotuning.toneanalyzer.extensions.setCompoundDrawableTop
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import com.willeypianotuning.toneanalyzer.sync.RestoreStrategy
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.files.FileSortOrderDialog.OnSortOrderChangeListener
import com.willeypianotuning.toneanalyzer.ui.files.export_tunings.AndroidFileExportLocation
import com.willeypianotuning.toneanalyzer.ui.files.export_tunings.AndroidUriExportLocation
import com.willeypianotuning.toneanalyzer.ui.files.export_tunings.ExportLocation
import com.willeypianotuning.toneanalyzer.ui.files.export_tunings.ExportTuningsViewModel
import com.willeypianotuning.toneanalyzer.ui.files.import_tunings.AndroidUriImportLocation
import com.willeypianotuning.toneanalyzer.ui.files.import_tunings.ImportLocation
import com.willeypianotuning.toneanalyzer.ui.files.import_tunings.ImportTuningsViewModel
import com.willeypianotuning.toneanalyzer.ui.files.state.ImportTuningState
import com.willeypianotuning.toneanalyzer.ui.files.state.ShareTuningState
import com.willeypianotuning.toneanalyzer.ui.main.MainActivity
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnDoneEditorActionListener
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnEnterPressKeyListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FilesActivity : BaseActivity() {
    private val viewModel: FilesViewModel by viewModels()
    private val importViewModel: ImportTuningsViewModel by viewModels()
    private val exportViewModel: ExportTuningsViewModel by viewModels()

    private val adapter: TuningFilesAdapter by lazy { TuningFilesAdapter(this, ArrayList()) }

    private lateinit var binding: ActivityFilesBinding

    private var selectAll = false
    private var isKeyboardShown = false

    private val actionCreateDocumentForTuningsExport = registerForActivityResult(
        CreateDocumentContract(),
        ::onCreateDocumentForTuningsExportResultReceived
    )

    private val selectionModeBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            exitSelectMode()
        }
    }

    private val searchBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
            binding.searchBarLayout.visibility = View.INVISIBLE
        }
    }

    private val navigateHomeBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val upIntent = NavUtils.getParentActivityIntent(this@FilesActivity)
            if (NavUtils.shouldUpRecreateTask(this@FilesActivity, upIntent!!)) {
                TaskStackBuilder.create(this@FilesActivity)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities()
            } else {
                NavUtils.navigateUpTo(this@FilesActivity, upIntent)
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.searchButton.setOnClickListener { toggleSearchBar() }
        binding.sortButton.setOnClickListener { showSortPopup() }
        binding.selectButton.setOnClickListener { onSelectButtonClicked() }
        binding.selectAllOrNoneButton.setOnClickListener { selectAllOrNone() }
        binding.deleteButton.setOnClickListener { deleteSelectedFiles() }
        binding.exportButton.setOnClickListener { v -> onExportArchiveButtonClicked(v) }
        binding.searchLabelButton.setOnClickListener { toggleSearchBar() }
        binding.searchView.setOnClickListener { onSearchViewClicked() }

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            performSearch()
            return@setOnEditorActionListener false
        }

        binding.searchEditText.doAfterTextChanged {
            performSearch()
        }

        binding.tuningsListView.setOnItemClickListener { _, view, position, _ ->
            onItemClicked(view, position)
        }
        binding.tuningsListView.setOnItemLongClickListener { _, view, position, _ ->
            onItemLongClick(view, position)
            return@setOnItemLongClickListener true
        }
        binding.tuningsListView.adapter = adapter

        viewModel.tuningsList.observe(this) { tunings: List<PianoTuningInfo> ->
            adapter.setFilesList(tunings)
            binding.bottomMenuLayout.visibility = if (tunings.isEmpty()) View.GONE else View.VISIBLE
            binding.emptyView.visibility = if (tunings.isEmpty()) View.VISIBLE else View.GONE
        }
        exportViewModel.shareFilesState.observe(this) { state: ShareTuningState ->
            onExportFileStateChanged(state)
        }
        importViewModel.importFilesState.observe(this) { state: ImportTuningState ->
            onImportFilesStateChanged(state)
        }
        onBackPressedDispatcher.addCallback(this, selectionModeBackCallback)
        onBackPressedDispatcher.addCallback(this, searchBackCallback)
        onBackPressedDispatcher.addCallback(this, navigateHomeBackCallback)
        selectionModeBackCallback.isEnabled = adapter.isSelectMode()
        searchBackCallback.isEnabled = binding.searchBarLayout.isVisible
        navigateHomeBackCallback.isEnabled = callingActivity == null
    }

    private fun onItemClicked(view: View, position: Int) {
        if (adapter.isSelectMode()) {
            val checkBox = view.findViewById<CheckBox>(R.id.files_list_item_checkbox)
            checkBox.performClick()
        } else {
            loadTuning(adapter.getItem(position))
        }
    }

    private fun onExportFileStateChanged(state: ShareTuningState) {
        if (state is ShareTuningState.Error) {
            Toast.makeText(
                this,
                "Failed to export tuning: " + state.error.message,
                Toast.LENGTH_LONG
            ).show()
            exportViewModel.onShareTuningsResultProcessed()
            return
        }
        if (state is ShareTuningState.Success) {
            if (state.share) {
                shareFile(state.exportLocation)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.activity_files_message_file_exported),
                    Toast.LENGTH_LONG
                ).show()
            }
            exportViewModel.onShareTuningsResultProcessed()
        }
    }

    private fun onImportFilesStateChanged(state: ImportTuningState) {
        if (state is ImportTuningState.Error) {
            Toast.makeText(
                this,
                "Failed to import tunings: " + state.error.message,
                Toast.LENGTH_LONG
            ).show()
            importViewModel.onImportTuningsResultProcessed()
            return
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        checkIfThereAreFilesToImport()
    }

    private fun checkIfThereAreFilesToImport() {
        // import .etf file if launched with intent
        val uri = intent.data
        if (uri != null) {
            // clear the uri, so we do not show importing menu again
            intent.data = null
            Timber.d("Found a file to restore tunings from. %s", uri.toString())
            if (isPro) {
                showImportConflictResolution(AndroidUriImportLocation(applicationContext, uri))
            } else {
                Toast.makeText(
                    this@FilesActivity,
                    "Import of tuning files is not supported in the " + (if (isPlus) "Plus" else "Free") + " version",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun showImportConflictResolution(location: ImportLocation) {
        val importDialog = AlertDialog.Builder(this)
        importDialog.setTitle("Import tunings")
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.activity_files_overwrite_option_keep_existing),
                    getString(R.string.activity_files_overwrite_option_replace_existing)
                ),
                0
            ) { dialogInterface: DialogInterface, i: Int ->
                importViewModel.importTunings(
                    location,
                    if (i == 1) RestoreStrategy.OVERWRITE_EXISTING else RestoreStrategy.RESTORE_MISSING
                )
                dialogInterface.dismiss()
            }
        importDialog.show()
    }

    private fun onSelectButtonClicked() {
        binding.selectButton.visibility = View.GONE
        binding.selectAllOrNoneButton.visibility = View.VISIBLE
        binding.deleteButton.visibility = View.VISIBLE
        binding.exportButton.visibility = View.VISIBLE
        binding.searchView.visibility = View.GONE
        binding.searchEditText.isFocusable = false
        binding.searchEditText.isEnabled = false
        adapter.deselectAll()
        adapter.setSelectMode(true)
        selectionModeBackCallback.isEnabled = true
        binding.selectAllOrNoneButton.setCompoundDrawableTop(R.drawable.ic_files_select_all)
        binding.selectAllOrNoneButton.setText(R.string.activity_files_action_select_all)
        selectAll = true
        adapter.notifyDataSetChanged()
    }

    private fun onExportArchiveButtonClicked(v: View) {
        val popup = PopupMenu(this, v)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            val selectedTunings = adapter.selectedFiles()
            if (selectedTunings.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.activity_files_error_delete_no_files_selected),
                    Toast.LENGTH_LONG
                ).show()
                exitSelectMode()
                popup.dismiss()
                return@setOnMenuItemClickListener true
            }

            when (item.itemId) {
                R.id.export_file -> {
                    exportViewModel.onTuningsForExportSelected(selectedTunings)?.let {
                        askUserForNewFileLocation(it)
                    }
                }
                R.id.export_share -> {
                    exportViewModel.onTuningsForShareSelected(selectedTunings)?.let {
                        exportViewModel.onExportSelectedTunings(
                            AndroidFileExportLocation(
                                applicationContext,
                                it
                            ), true
                        )
                    }
                }
            }
            exitSelectMode()
            popup.dismiss()
            true
        }
        popup.menuInflater.inflate(R.menu.menu_export, popup.menu)
        popup.show()
    }

    private fun askUserForNewFileLocation(fileName: String) {
        actionCreateDocumentForTuningsExport.launch(fileName)
    }

    private fun onSearchViewClicked() {
        binding.searchEditText.setText("")
        binding.searchBarLayout.isVisible = true
        searchBackCallback.isEnabled = true
        binding.searchEditText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun selectAllOrNone() {
        selectAll = if (selectAll) {
            adapter.selectAll()
            binding.selectAllOrNoneButton.setCompoundDrawableTop(R.drawable.ic_files_select_none)
            binding.selectAllOrNoneButton.setText(R.string.activity_files_action_select_none)
            false
        } else {
            adapter.deselectAll()
            binding.selectAllOrNoneButton.setCompoundDrawableTop(R.drawable.ic_files_select_all)
            binding.selectAllOrNoneButton.setText(R.string.activity_files_action_select_all)
            true
        }
        adapter.notifyDataSetChanged()
    }

    private fun copySelectedFile(tuning: PianoTuningInfo) {
        viewModel.copyTuning(tuning)
    }

    private fun onCreateDocumentForTuningsExportResultReceived(uri: Uri?) {
        if (uri == null) {
            exportViewModel.onShareTuningsResultProcessed()
            return
        }

        exportViewModel.onExportSelectedTunings(
            AndroidUriExportLocation(applicationContext, uri),
            false
        )
    }

    private fun renameSelectedFile(file: PianoTuningInfo) {
        val input = EditText(this)
        input.setText(file.name)
        input.setSelectAllOnFocus(true)
        input.isSingleLine = true
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.activity_files_dialog_rename_file_title))
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(R.string.action_ok, null)
            .setView(input)
            .create()
        input.setOnKeyListener(SubmitDialogOnEnterPressKeyListener(dialog))
        input.setOnEditorActionListener(SubmitDialogOnDoneEditorActionListener(dialog))
        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val name = input.text.toString()
                if (!name.trim { it <= ' ' }.equals("", ignoreCase = true)) {
                    file.name = name
                    viewModel.renameTuning(file)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCloseSearch(v: View?) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        binding.searchBarLayout.visibility = View.INVISIBLE
        searchBackCallback.isEnabled = false
        adapter.filter(null)
    }

    private fun exitSelectMode() {
        binding.selectButton.visibility = View.VISIBLE
        binding.selectAllOrNoneButton.visibility = View.GONE
        binding.deleteButton.visibility = View.GONE
        binding.exportButton.visibility = View.GONE
        binding.searchView.visibility = View.VISIBLE
        binding.searchEditText.isFocusable = true
        binding.searchEditText.isFocusableInTouchMode = true
        binding.searchEditText.isEnabled = true
        if (binding.searchBarLayout.isVisible) {
            binding.searchEditText.requestFocus()
        }
        adapter.setSelectMode(false)
        selectionModeBackCallback.isEnabled = false
    }

    private fun loadTuning(tuning: PianoTuningInfo) {
        appSettings.currentTuningId = tuning.id
        if (callingActivity != null) {
            val data = Intent()
            data.putExtra(TUNING_ID, tuning.id)
            setResult(RESULT_OK, data)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun deleteSelectedFiles() {
        val selected = adapter.selectedFiles()
        if (selected.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.activity_files_error_delete_no_files_selected),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val deleteDialog = AlertDialog.Builder(this)
        deleteDialog.setTitle(
            resources.getQuantityString(
                R.plurals.message_confirm_delete_files,
                selected.size,
                selected.size
            )
        )
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                viewModel.deleteTunings(selected)
                exitSelectMode()
                dialogInterface.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .show()
    }

    private fun onItemLongClick(view: View, position: Int) {
        adapter.deselectAll()
        adapter.select(adapter.getItem(position))
        val popup = PopupMenu(this, view)
        popup.forceShowIcons()
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_open -> loadTuning(adapter.getItem(position))
                R.id.action_copy -> copySelectedFile(adapter.getItem(position))
                R.id.action_rename -> renameSelectedFile(adapter.getItem(position))
                R.id.action_delete -> viewModel.deleteTuning(adapter.getItem(position))
                R.id.action_share -> shareSelectedFile(adapter.getItem(position))
                R.id.action_export -> exportSelectedFile(adapter.getItem(position))
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popup.setOnDismissListener {
            adapter.deselectAll()
            binding.tuningsListView.requestFocus()
        }
        popup.menuInflater.inflate(R.menu.menu_files, popup.menu)
        popup.show()
    }

    private fun shareSelectedFile(file: PianoTuningInfo) {
        exportViewModel.onTuningsForShareSelected(listOf(file))?.let {
            exportViewModel.onExportSelectedTunings(
                AndroidFileExportLocation(
                    applicationContext,
                    it
                ), true
            )
        }
    }

    private fun exportSelectedFile(file: PianoTuningInfo) {
        exportViewModel.onTuningsForExportSelected(listOf(file))?.let {
            askUserForNewFileLocation(it)
        }
    }

    private fun shareFile(exportLocation: ExportLocation) {
        val intentShareFile = Intent(Intent.ACTION_SEND)
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intentShareFile.type = "*/*"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse(exportLocation.uriString()))
        startActivity(
            Intent.createChooser(
                intentShareFile,
                getString(R.string.activity_files_share_dialog_title)
            )
        )
    }

    private fun performSearch() {
        adapter.filter(binding.searchEditText.text.toString())
    }

    private fun toggleSearchBar() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        isKeyboardShown = !isKeyboardShown
        if (!isKeyboardShown) {
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        } else {
            binding.searchEditText.requestFocus()
            imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showSortPopup() {
        val dialog = FileSortOrderDialog(this, viewModel.sortOrder.value!!)
        dialog.onSortOrderChangeListener =
            OnSortOrderChangeListener { sortMode: Int, makeDefault: Boolean ->
                viewModel.changeSortOrder(
                    sortMode,
                    makeDefault
                )
            }
        dialog.show()
    }

    companion object {
        const val TUNING_ID = "tuningId"
    }

    class Contract : ActivityResultContract<Void?, String?>() {
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(context, FilesActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return null
            }
            return intent.getStringExtra(TUNING_ID)
        }
    }
}