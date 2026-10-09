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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale

@Composable
fun WalletHeaderCard(
    totalBalanceUsd: Double,
    cardCount: Int,
    onCreateCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedBalance = String.format(Locale.US, "%,.2f", totalBalanceUsd)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 22.dp)
            .testTag("wallet_header_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SALDO TOTAL DE TU BILLETERA",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$$formattedBalance USD",
                color = TextWhite,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("total_balance_text")
            )

            Spacer(modifier = Modifier.height(6.dp))

            val subtitle = if (cardCount == 0) {
                "No hay tarjetas creadas aún. Saldo en $0.00"
            } else {
                "$cardCount ${if (cardCount == 1) "tarjeta guardada" else "tarjetas guardadas"} • Cotización en tiempo real"
            }

            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onCreateCardClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = PurpleAccent.copy(alpha = 0.5f),
                        spotColor = PurplePrimary.copy(alpha = 0.6f)
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(PurpleDark, PurplePrimary, PurpleAccent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("create_new_card_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Crear Nueva Tarjeta",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
