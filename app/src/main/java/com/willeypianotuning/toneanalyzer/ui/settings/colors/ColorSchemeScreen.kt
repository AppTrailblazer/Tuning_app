package com.willeypianotuning.toneanalyzer.ui.settings.colors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.willeypianotuning.toneanalyzer.R
import java.util.*

@Composable
fun ColorSchemeScreen(
    viewModel: ColorSchemeViewModel, onBackPressed: () -> Unit
) {
    BackHandler {
        onBackPressed()
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.global_settings_color_scheme)) },
            navigationIcon = {
                IconButton(
                    onClick = { onBackPressed() },
                    content = {
                        Icon(painterResource(R.drawable.ic_back_arrow), null)
                    },
                )
            },
            backgroundColor = colorResource(R.color.toolbar_color),
            contentColor = Color.White,
            elevation = 12.dp
        )
    }, content = { offsets ->
        Column(modifier = Modifier.padding(offsets)) {
            ColorSchemeList(
                modifier = Modifier
                    .background(colorResource(R.color.menu_background))
                    .weight(1f),
                viewModel = viewModel
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.toolbar_color))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = {
                    viewModel.restoreDefaultScheme()
                    onBackPressed()
                }) {
                    Text(
                        stringResource(id = R.string.action_restore_defaults),
                        color = Color.White
                    )
                }
                TextButton(onClick = {
                    viewModel.saveCurrentScheme()
                    onBackPressed()
                }) {
                    Text(stringResource(id = R.string.action_save), color = Color.White)
                }
            }
        }
    })
    val editSetting = viewModel.colorSchemeEditor.collectAsState().value
    if (editSetting != null) {
        ColorSchemeEditor(viewModel, editSetting)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColorSchemeEditor(
    viewModel: ColorSchemeViewModel,
    editSetting: ColorSetting,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedColor by remember {
        mutableStateOf(Color(editSetting.color))
    }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = selectedColor.hex(editSetting.alphaAllowed)
            )
        )
    }
    Dialog(
        onDismissRequest = viewModel::cancelEdit,
    ) {
        Card(
            backgroundColor = colorResource(R.color.menu_background),
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier.padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(editSetting.title), fontSize = 20.sp)
                ClassicColorPicker(modifier = Modifier.height(240.dp),
                    color = HsvColor.from(selectedColor),
                    showAlphaBar = editSetting.alphaAllowed,
                    onColorChanged = { color: HsvColor ->
                        selectedColor = color.toColor()
                        textFieldValue = TextFieldValue(
                            text = selectedColor.hex(editSetting.alphaAllowed)
                        )
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    })
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(selectedColor, CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextField(
                        value = textFieldValue,
                        onValueChange = { value ->
                            var newValue = value.copy(text = value.text.uppercase(Locale.US))
                            if (!Regex("^#?[\\dA-F]*$").matches(newValue.text)) {
                                return@TextField
                            }
                            if (newValue.text.isNotEmpty() && !newValue.text.startsWith("#")) {
                                newValue = newValue.copy(
                                    text = "#${newValue.text}",
                                    selection = TextRange(
                                        newValue.selection.start + 1,
                                        newValue.selection.end + 1
                                    )
                                )
                            }
                            val maxLength = if (editSetting.alphaAllowed) 9 else 7
                            if (newValue.text.length > maxLength) {
                                newValue = newValue.copy(
                                    text = newValue.text.substring(0, maxLength),
                                    selection = TextRange(
                                        minOf(newValue.selection.start, maxLength + 1),
                                        minOf(newValue.selection.end, maxLength + 1)
                                    ),
                                )
                            }
                            textFieldValue = newValue
                            if (newValue.text.length == maxLength) {
                                selectedColor = colorFromHex(newValue.text)
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        },
                        placeholder = {
                            Text(selectedColor.hex(editSetting.alphaAllowed))
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = Color.White,
                            placeholderColor = Color.LightGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.LightGray,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            kotlin.runCatching {
                                selectedColor = colorFromHex(textFieldValue.text)
                            }.onFailure {
                                textFieldValue =
                                    TextFieldValue(text = selectedColor.hex(editSetting.alphaAllowed))
                            }
                            focusManager.clearFocus(force = true)
                        }),
                        singleLine = true,
                        maxLines = 1,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = {
                        selectedColor = Color(editSetting.defaultColor)
                        textFieldValue = TextFieldValue(
                            text = selectedColor.hex(editSetting.alphaAllowed)
                        )
                    }) {
                        Text(
                            stringResource(id = R.string.action_default),
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        viewModel.cancelEdit()
                    }) {
                        Text(
                            stringResource(id = R.string.action_cancel),
                            color = Color.White,
                        )
                    }
                    TextButton(onClick = {
                        viewModel.update(
                            editSetting.update(
                                viewModel.colorScheme, selectedColor.toArgb()
                            )
                        )
                    }) {
                        Text(
                            stringResource(id = R.string.action_ok),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

private fun Color.hex(argb: Boolean): String {
    val mask = when {
        argb -> 0xffffffff.toInt()
        else -> 0xffffff
    }
    val hex = Integer.toHexString(toArgb() and mask).uppercase(Locale.US)
    return "#${hex.padStart(if (argb) 8 else 6, '0')}"
}

private fun colorFromHex(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}