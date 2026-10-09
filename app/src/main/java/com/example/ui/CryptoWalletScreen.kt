package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CreateEditCardDialog
import com.example.ui.components.CryptoCardItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyWalletCard
import com.example.ui.components.QrCodeDialog
import com.example.ui.components.WalletHeaderCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite

@Composable
fun CryptoWalletScreen(
    viewModel: CryptoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val filteredCards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val totalBalanceUsd by viewModel.totalPortfolioUsd.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

    // Form states
    val formName by viewModel.formName.collectAsStateWithLifecycle()
    val formAddress by viewModel.formAddress.collectAsStateWithLifecycle()
    val formCryptoType by viewModel.formCryptoType.collectAsStateWithLifecycle()
    val formBalance by viewModel.formBalance.collectAsStateWithLifecycle()
    val formNotes by viewModel.formNotes.collectAsStateWithLifecycle()
    val detectionResult by viewModel.detectionResult.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("crypto_wallet_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // App Bar Title & Refresh
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF161F36))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Billetera Cripto",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Detección inteligente de direcciones",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.refreshPrices()
                            Toast.makeText(context, "Precios de mercado actualizados", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp).testTag("refresh_prices_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar precios",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Top Total Balance Card (Always present, matching screenshot)
            item {
                WalletHeaderCard(
                    totalBalanceUsd = totalBalanceUsd,
                    cardCount = cards.size,
                    onCreateCardClick = { viewModel.openCreateDialog() }
                )
            }

            // If empty, show the Hero Empty State Card matching screenshot
            if (cards.isEmpty()) {
                item {
                    EmptyWalletCard(
                        onCreateFirstCardClick = { viewModel.openCreateDialog() }
                    )
                }
            } else {
                // Search and Filters
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Buscar tarjeta por nombre o dirección...", color = TextSubtle, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_cards_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter chips
                        val filters = listOf(
                            "ALL" to "Todas",
                            "BTC" to "Bitcoin (BTC)",
                            "ETH" to "Ethereum / EVM",
                            "SOL" to "Solana (SOL)",
                            "TRON" to "TRON / USDT"
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            filters.forEach { (code, label) ->
                                val isSelected = selectedFilter == code
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) PurpleDark else SurfaceDark
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) PurpleAccent else CardBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.onFilterSelected(code) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("filter_chip_$code")
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) TextWhite else TextMuted,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // List of Crypto Cards
                if (filteredCards.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Text(
                                text = "No se encontraron tarjetas con ese criterio",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredCards,
                        key = { it.id }
                    ) { card ->
                        CryptoCardItem(
                            card = card,
                            onViewQrClick = { viewModel.openQrDialog(card) },
                            onEditClick = { viewModel.openEditDialog(card) },
                            onDeleteClick = { viewModel.openDeleteConfirm(card) }
                        )
                    }
                }
            }
        }

        // Active Dialogs
        when (val state = dialogState) {
            is DialogState.Create -> {
                CreateEditCardDialog(
                    isEditing = false,
                    name = formName,
                    onNameChange = { viewModel.onFormNameChanged(it) },
                    address = formAddress,
                    onAddressChange = { viewModel.onFormAddressChanged(it) },
                    selectedCrypto = formCryptoType,
                    onCryptoChange = { viewModel.onFormCryptoTypeChanged(it) },
                    balance = formBalance,
                    onBalanceChange = { viewModel.onFormBalanceChanged(it) },
                    notes = formNotes,
                    onNotesChange = { viewModel.onFormNotesChanged(it) },
                    detectionResult = detectionResult,
                    errorMessage = formError,
                    onSave = { viewModel.saveCard() },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is DialogState.Edit -> {
                CreateEditCardDialog(
                    isEditing = true,
                    name = formName,
                    onNameChange = { viewModel.onFormNameChanged(it) },
                    address = formAddress,
                    onAddressChange = { viewModel.onFormAddressChanged(it) },
                    selectedCrypto = formCryptoType,
                    onCryptoChange = { viewModel.onFormCryptoTypeChanged(it) },
                    balance = formBalance,
                    onBalanceChange = { viewModel.onFormBalanceChanged(it) },
                    notes = formNotes,
                    onNotesChange = { viewModel.onFormNotesChanged(it) },
                    detectionResult = detectionResult,
                    errorMessage = formError,
                    onSave = { viewModel.saveCard() },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is DialogState.ViewQr -> {
                QrCodeDialog(
                    card = state.card,
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is DialogState.ConfirmDelete -> {
                DeleteConfirmDialog(
                    card = state.card,
                    onConfirm = { viewModel.deleteCard(state.card) },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is DialogState.None -> Unit
        }
    }
}
