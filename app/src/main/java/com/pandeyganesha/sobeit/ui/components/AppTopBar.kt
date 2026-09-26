package com.pandeyganesha.kaamsutra.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier
){
    Text(
        text = title,
        modifier=modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.headlineMedium
    )
}