package com.willeypianotuning.toneanalyzer.ui.settings.colors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willeypianotuning.toneanalyzer.R

@Composable
fun ColorSchemeList(
    modifier: Modifier = Modifier,
    viewModel: ColorSchemeViewModel,
) {
    val settings = viewModel.colorSettings.collectAsState().value
    LazyColumn(modifier = modifier) {
        items(settings.size) {
            ColorSettingWidget(
                setting = settings[it],
                onEdit = viewModel::edit,
            )
        }
    }
}

@Composable
private fun ColorSettingWidget(
    setting: ColorSetting,
    onEdit: (ColorSetting) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onEdit(setting) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement
            .spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(setting.icon),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(setting.title),
            color = Color.White,
            style = TextStyle(
                fontSize = 16.sp
            )
        )
        Box(
            Modifier
                .size(36.dp)
                .background(Color(setting.color), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
        ) {
            if (setting.isTransparent) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_not_interested_24),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = Color.White
                )
            }
        }
    }
}