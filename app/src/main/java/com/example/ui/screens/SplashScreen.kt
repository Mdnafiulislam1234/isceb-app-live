package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onDismiss: () -> Unit,
    isInitializing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }
    var minimumWaitElapsed by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.94f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(650)
        minimumWaitElapsed = true
    }

    // Hard fail-safe: guarantee SplashScreen NEVER hangs even on slow devices
    LaunchedEffect(Unit) {
        delay(1500)
        onDismiss()
    }

    LaunchedEffect(minimumWaitElapsed, isInitializing) {
        if (minimumWaitElapsed && !isInitializing) {
            onDismiss()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_splash")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // User can tap to immediately proceed
                onDismiss()
            },
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Official ISCEB logo (1200 x 896, aspect ratio ~1.34:1)
                // Cleanly fitted on white background without cropping
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1200f / 896f)
                        .scale(scaleAnim)
                        .alpha(alphaAnim),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB - Connect. learn. Achieve.",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("img_splash_isceb_logo"),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Subtle accreditation text
                Column(
                    modifier = Modifier.alpha(alphaAnim),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "POST GRADUATE DIPLOMA (PGD)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Masterclass & Executive Certification Portal",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Minimal elegant loading indicator
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(140.dp)
                            .height(3.dp),
                        color = GoldMetallic,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }

            // Bottom copyright & portal registry
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .alpha(alphaAnim),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Official Academic Registry: ISCEB • www.isceb.org",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
