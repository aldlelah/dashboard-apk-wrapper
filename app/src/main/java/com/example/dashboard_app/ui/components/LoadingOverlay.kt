package com.example.dashboard_app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.dashboard_app.ui.theme.PrimaryBlue
import com.example.dashboard_app.ui.theme.Slate900

@Composable
fun TopProgressBar(
    progress: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = progress in 1..99,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = PrimaryBlue,
            trackColor = Slate900.copy(alpha = 0.5f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate900),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = PrimaryBlue,
                strokeWidth = 4.dp
            )
        }
    }
}
