package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

/**
 * First launch: two taps — pick a helper language, pick how long to study each day — then Home,
 * where a one-time tour card explains what the app contains.
 */
@Composable
fun OnboardingScreen(viewModel: BlasterViewModel) {
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy, Color(0xFF15367A))))
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
        ) {
            Image(
                painter = painterResource(R.drawable.lia_happy),
                contentDescription = "Lía",
                modifier = Modifier.size(170.dp)
            )
            if (step == 0) {
                Text(tl("¡Hola! Soy Lía"), color = SolarGold, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    tl("مرحبًا! أنا ليا. سنتعلم الإسبانية معًا.\nHi! I'm Lía. Let's learn Spanish together."),
                    color = StarWhite, fontSize = 16.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text("اختر لغتك · Choose your language", color = StarWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                BigChoice("العربية", "") { viewModel.setHelperLanguage(HelperLanguage.ARABIC); step = 1 }
                BigChoice("English", "") { viewModel.setHelperLanguage(HelperLanguage.ENGLISH); step = 1 }
            } else {
                Text(
                    language.pick("كم دقيقة في اليوم؟", "How many minutes a day?"),
                    color = SolarGold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )
                Text(
                    language.pick("يمكنك تغيير ذلك لاحقًا.", "You can change this later."),
                    color = StarWhite.copy(alpha = 0.8f), fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                BigChoice(language.pick("5 دقائق", "5 minutes"), language.pick("خفيف", "Easy")) { viewModel.finishOnboarding(20) }
                BigChoice(language.pick("10 دقائق", "10 minutes"), language.pick("منتظم", "Regular")) { viewModel.finishOnboarding(50) }
                BigChoice(language.pick("15 دقيقة", "15 minutes"), language.pick("جاد", "Serious")) { viewModel.finishOnboarding(100) }
                Spacer(Modifier.height(6.dp))
                Text(
                    language.pick("بعدها ستظهر لك الصفحة الرئيسية.", "Then you'll see your home screen."),
                    color = StarWhite.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BigChoice(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = StarWhite,
        border = BorderStroke(2.dp, SolarGold),
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = SpaceNavy, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            if (subtitle.isNotEmpty()) Text(subtitle, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
