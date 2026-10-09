package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderHighlight
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun EmptyWalletCard(
    onCreateFirstCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("empty_wallet_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rounded wallet icon container with subtle glow & border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161F36))
                    .border(1.5.dp, CardBorderHighlight, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Billetera",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Comienza con tu Billetera Cripto",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No hay saldos falsos. Vincula tu dirección cripto real (Bitcoin, Ethereum, Solana, USDT, etc.) y asígnale un nombre a tu tarjeta para ver tus fondos y generar tu código QR.",
                color = TextMuted,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onCreateFirstCardClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = PurpleAccent.copy(alpha = 0.5f),
                        spotColor = PurplePrimary.copy(alpha = 0.7f)
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(PurpleDark, PurplePrimary, PurpleAccent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("create_first_card_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Crear Primera Tarjeta Cripto",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
