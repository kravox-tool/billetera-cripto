package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto_cards")
data class CryptoCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val cryptoCode: String,      // BTC, ETH, SOL, USDT_TRC20, TRX, etc.
    val cryptoName: String,      // Bitcoin, Ethereum, Solana, etc.
    val networkName: String,     // Red Bitcoin (Mainnet), Red Ethereum / EVM, etc.
    val balance: Double = 0.0,   // Optional user-specified or tracked balance
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val colorHex: Long = 0xFF6D28D9 // Accent color
)
