package com.willeypianotuning.toneanalyzer.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.AudioInputType
import com.willeypianotuning.toneanalyzer.audio.PreferredAudioInput
import com.willeypianotuning.toneanalyzer.extensions.conditional

class AudioInputDialog(
    private val activity: AppCompatActivity,
    private val initialState: PreferredAudioInput,
    private val onApply: (PreferredAudioInput) -> Unit,
) : Dialog(activity) {
    private fun initViewTreeOwners() {
        window?.decorView?.let {
            it.setViewTreeViewModelStoreOwner(activity)
            it.setViewTreeLifecycleOwner(activity)
            it.setViewTreeSavedStateRegistryOwner(activity)
            it.setViewTreeOnBackPressedDispatcherOwner(activity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewTreeOwners()
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val view = ComposeView(context).apply {
            setContent {
                AudioInputDialogContent(
                    initialState,
                    onCancel = { dismiss() },
                    onApply = {
                        onApply(it)
                        dismiss()
                    },
                )
            }
        }
        setContentView(view)
    }
}

@Composable
fun AudioInputDialogContent(
    audioInputType: PreferredAudioInput,
    onCancel: () -> Unit,
    onApply: (PreferredAudioInput) -> Unit,
) {
    val audioInputOptions = remember {
        listOf(
            R.string.dialog_audio_input_option_built_in_mic to AudioInputType.BUILT_IN_MIC,
            R.string.dialog_audio_input_option_external_wired_mic to AudioInputType.EXTERNAL_MIC,
        )
    }
    var selectedIndex by remember {
        mutableIntStateOf(audioInputOptions.indexOfFirst { it.second == audioInputType.type })
    }
    val bluetoothCheckEnabled = selectedIndex == 1
    val (checkedState, onStateChange) = remember { mutableStateOf(audioInputType.allowBluetooth) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.menu_background))
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .padding(bottom = 8.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.dialog_audio_input_title),
            style = MaterialTheme.typography.h6,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            audioInputOptions.forEachIndexed { index, value ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIndex = index
                        }
                        .heightIn(min = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.LightGray,
                            disabledColor = Color.LightGray,
                        ),
                    )
                    Text(
                        text = stringResource(value.first),
                        style = MaterialTheme.typography.body1,
                        color = Color.White,
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .conditional(
                    bluetoothCheckEnabled,
                    ifTrue = {
                        this.toggleable(
                            value = checkedState,
                            onValueChange = { onStateChange(!checkedState) },
                            role = Role.Checkbox
                        )
                    }
                )
                .padding(start = 48.dp)
                .heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checkedState,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color.LightGray,
                    disabledColor = Color.Gray,
                    checkmarkColor = colorResource(R.color.menu_background),
                ),
                enabled = bluetoothCheckEnabled,
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.dialog_audio_input_option_allow_bluetooth),
                style = MaterialTheme.typography.body1,
                color = if (bluetoothCheckEnabled) Color.White else Color.Gray,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AudioInputDialogButtons(
            onCancel = onCancel,
            onApply = {
                onApply(
                    PreferredAudioInput(
                        type = audioInputOptions[selectedIndex].second,
                        allowBluetooth = checkedState,
                    )
                )
            }
        )
    }
}

@Composable
private fun AudioInputDialogButtons(
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = { onCancel() },
        ) {
            Text(
                text = stringResource(R.string.action_cancel),
                style = MaterialTheme.typography.button,
                color = Color.White,
            )
        }
        TextButton(
            onClick = { onApply() },
        ) {
            Text(
                text = stringResource(R.string.action_ok),
                style = MaterialTheme.typography.button,
                color = Color.White,
            )
        }
    }
}

private class AudioInputDialogContentPreviewProvider :
    PreviewParameterProvider<PreferredAudioInput> {
    override val values: Sequence<PreferredAudioInput>
        get() = sequenceOf(
            PreferredAudioInput(
                type = AudioInputType.BUILT_IN_MIC,
                allowBluetooth = false,
            ),
            PreferredAudioInput(
                type = AudioInputType.EXTERNAL_MIC,
                allowBluetooth = true,
            ),
        )

}

@Composable
@Preview
fun AudioInputDialogContentPreview(
    @PreviewParameter(AudioInputDialogContentPreviewProvider::class) input: PreferredAudioInput
) {
    AudioInputDialogContent(
        audioInputType = input,
        onCancel = {},
        onApply = {},
    )
}
