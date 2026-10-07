package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.theme.ExplorerBlue
import kotlinx.coroutines.launch

/**
 * A bouncing "scroll down" pill shown at the bottom of a scrollable panel while there is more content
 * below. It fades the content edge so it's clear the panel continues, and tapping it scrolls down.
 * Place it inside the same [Box] as the scrolling column.
 */
@Composable
fun BoxScope.ScrollMoreHint(scroll: ScrollState, language: HelperLanguage, fadeColor: Color) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        // Hidden while typing: the keyboard already covers the bottom of the panel.
        visible = scroll.canScrollForward && WindowInsets.ime.getBottom(LocalDensity.current) == 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
    ) {
        val bounce by rememberInfiniteTransition(label = "scrollHint")
            .animateFloat(0f, 6f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "bounce")
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, fadeColor)))
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .offset(y = bounce.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ExplorerBlue)
                    .clickable { scope.launch { scroll.animateScrollTo(scroll.value + scroll.viewportSize * 2 / 3) } }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    language.pick("⬇ مرّري للأسفل لرؤية المزيد", "⬇ Scroll down to see more"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
