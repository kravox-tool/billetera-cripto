package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CardBorder
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.util.CryptoType
import com.example.util.DetectionResult
import com.example.util.PriceService
import java.util.Locale

@Composable
fun CreateEditCardDialog(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    selectedCrypto: CryptoType,
    onCryptoChange: (CryptoType) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    detectionResult: DetectionResult?,
    errorMessage: String?,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val text = item?.text?.toString()?.trim() ?: ""
        if (text.isNotBlank()) {
            onAddressChange(text)
        }
    }

    val livePrice = PriceService.getUsdPriceFor(selectedCrypto)
    val parsedBal = balance.toDoubleOrNull() ?: 0.0
    val estimatedUsd = parsedBal * livePrice

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
                .testTag("create_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEditing) "Editar Tarjeta Cripto" else "Nueva Tarjeta Cripto",
                        color = TextWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Nombre de la tarjeta
                Text(
                    text = "Nombre de la Tarjeta *",
                    color = TextWhite,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text("Ej. Mi Billetera Fría, Binance Spot, MetaMask", color = TextSubtle, fontSize = 13.5.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Color(0xFF0E1424),
                        unfocusedContainerColor = Color(0xFF0E1424)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_card_name")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Dirección Cripto with Paste Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dirección Cripto *",
                        color = TextWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161F36))
                            .clickable { pasteFromClipboard() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("paste_address_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Pegar",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pegar",
                            color = Color(0xFF818CF8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    placeholder = { Text("Pega aquí tu dirección Bitcoin, ETH, SOL, Tron...", color = TextSubtle, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Color(0xFF0E1424),
                        unfocusedContainerColor = Color(0xFF0E1424)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_card_address")
                )

                // Real-Time Detection Pill Banner
                AnimatedVisibility(
                    visible = detectionResult != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (detectionResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0C241F))
                                .border(1.dp, GreenSuccess.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                                .testTag("detection_result_box")
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Detectado",
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Detectado automáticamente: ${detectionResult.cryptoType.displayName}",
                                        color = GreenSuccess,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detectionResult.formatDescription,
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )

                                // If alternative types exist (e.g. EVM can be ETH or USDT ERC20), offer quick switcher
                                if (detectionResult.alternativeTypes.size > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Asignar como:",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        detectionResult.alternativeTypes.forEach { altType ->
                                            val isSelected = selectedCrypto == altType
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) PurpleAccent else Color(0xFF162529)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) PurpleAccent else Color(0xFF284547),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onCryptoChange(altType) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${altType.iconEmoji} ${altType.symbol}",
                                                    color = TextWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Selector de Criptomoneda / Red
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tipo de Criptomoneda",
                        color = TextWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Autodetección",
                        tint = PurpleAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val cryptoList = listOf(
                    CryptoType.BITCOIN,
                    CryptoType.ETHEREUM,
                    CryptoType.USDT_TRC20,
                    CryptoType.SOLANA,
                    CryptoType.TRON,
                    CryptoType.USDT_ERC20,
                    CryptoType.LITECOIN,
                    CryptoType.DOGECOIN,
                    CryptoType.CARDANO,
                    CryptoType.TON,
                    CryptoType.RIPPLE,
                    CryptoType.OTHER
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("crypto_type_selector")
                ) {
                    cryptoList.forEach { type ->
                        val isSelected = selectedCrypto == type
                        val pillColor = Color(type.colorHex)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) pillColor.copy(alpha = 0.25f) else Color(0xFF0E1424)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) pillColor else CardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onCryptoChange(type) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("crypto_pill_${type.code}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = type.iconEmoji,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = type.symbol,
                                    color = if (isSelected) TextWhite else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Saldo Opcional
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saldo en Billetera (Opcional)",
                        color = TextWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (parsedBal > 0) {
                        Text(
                            text = "≈ $${String.format(Locale.US, "%,.2f", estimatedUsd)} USD",
                            color = GreenSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = balance,
                    onValueChange = onBalanceChange,
                    placeholder = { Text("Ej. 0.05", color = TextSubtle, fontSize = 13.5.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        Text(
                            text = selectedCrypto.symbol,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Color(0xFF0E1424),
                        unfocusedContainerColor = Color(0xFF0E1424)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_card_balance")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Notas opcionales
                Text(
                    text = "Nota / Descripción (Opcional)",
                    color = TextWhite,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Ej. Dirección para recibir pagos, Ledger frío...", color = TextSubtle, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Color(0xFF0E1424),
                        unfocusedContainerColor = Color(0xFF0E1424)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Guardar Button
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 12.dp,
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
                        .testTag("save_card_button")
                ) {
                    Text(
                        text = if (isEditing) "Guardar Cambios" else "Guardar Tarjeta Cripto",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
