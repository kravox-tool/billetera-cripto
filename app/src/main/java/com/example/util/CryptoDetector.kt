package com.example.util

enum class CryptoType(
    val code: String,
    val displayName: String,
    val symbol: String,
    val networkName: String,
    val coingeckoId: String,
    val iconEmoji: String,
    val defaultDecimals: Int,
    val colorHex: Long
) {
    BITCOIN(
        code = "BTC",
        displayName = "Bitcoin",
        symbol = "BTC",
        networkName = "Red Bitcoin (Mainnet)",
        coingeckoId = "bitcoin",
        iconEmoji = "₿",
        defaultDecimals = 8,
        colorHex = 0xFFF7931A
    ),
    ETHEREUM(
        code = "ETH",
        displayName = "Ethereum",
        symbol = "ETH",
        networkName = "Red Ethereum / EVM",
        coingeckoId = "ethereum",
        iconEmoji = "Ξ",
        defaultDecimals = 18,
        colorHex = 0xFF627EEA
    ),
    USDT_TRC20(
        code = "USDT_TRC20",
        displayName = "USDT (TRC-20)",
        symbol = "USDT",
        networkName = "Red TRON (TRC-20)",
        coingeckoId = "tether",
        iconEmoji = "₮",
        defaultDecimals = 6,
        colorHex = 0xFF26A17B
    ),
    SOLANA(
        code = "SOL",
        displayName = "Solana",
        symbol = "SOL",
        networkName = "Red Solana (Mainnet)",
        coingeckoId = "solana",
        iconEmoji = "◎",
        defaultDecimals = 9,
        colorHex = 0xFF9945FF
    ),
    TRON(
        code = "TRX",
        displayName = "TRON",
        symbol = "TRX",
        networkName = "Red TRON",
        coingeckoId = "tron",
        iconEmoji = "⚡",
        defaultDecimals = 6,
        colorHex = 0xFFEF0027
    ),
    USDT_ERC20(
        code = "USDT_ERC20",
        displayName = "USDT (ERC-20)",
        symbol = "USDT",
        networkName = "Red Ethereum (ERC-20)",
        coingeckoId = "tether",
        iconEmoji = "₮",
        defaultDecimals = 6,
        colorHex = 0xFF26A17B
    ),
    LITECOIN(
        code = "LTC",
        displayName = "Litecoin",
        symbol = "LTC",
        networkName = "Red Litecoin",
        coingeckoId = "litecoin",
        iconEmoji = "Ł",
        defaultDecimals = 8,
        colorHex = 0xFF345D9D
    ),
    DOGECOIN(
        code = "DOGE",
        displayName = "Dogecoin",
        symbol = "DOGE",
        networkName = "Red Dogecoin",
        coingeckoId = "dogecoin",
        iconEmoji = "Ð",
        defaultDecimals = 8,
        colorHex = 0xFFC2A633
    ),
    CARDANO(
        code = "ADA",
        displayName = "Cardano",
        symbol = "ADA",
        networkName = "Red Cardano (Shelley)",
        coingeckoId = "cardano",
        iconEmoji = "₳",
        defaultDecimals = 6,
        colorHex = 0xFF0033AD
    ),
    TON(
        code = "TON",
        displayName = "The Open Network",
        symbol = "TON",
        networkName = "Red TON",
        coingeckoId = "the-open-network",
        iconEmoji = "💎",
        defaultDecimals = 9,
        colorHex = 0xFF0098EA
    ),
    RIPPLE(
        code = "XRP",
        displayName = "Ripple",
        symbol = "XRP",
        networkName = "Red XRP Ledger",
        coingeckoId = "ripple",
        iconEmoji = "✕",
        defaultDecimals = 6,
        colorHex = 0xFF23292F
    ),
    OTHER(
        code = "OTHER",
        displayName = "Personalizada",
        symbol = "CRYPTO",
        networkName = "Red Desconocida / Otra",
        coingeckoId = "",
        iconEmoji = "🪙",
        defaultDecimals = 8,
        colorHex = 0xFF8B5CF6
    );

    companion object {
        fun fromCode(code: String): CryptoType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: OTHER
        }
    }
}

data class DetectionResult(
    val cryptoType: CryptoType,
    val confidence: String,
    val formatDescription: String,
    val alternativeTypes: List<CryptoType> = emptyList()
)

object CryptoDetector {

    // Base58 characters
    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    // Bech32 characters
    private const val BECH32_ALPHABET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    private fun isBase58(str: String): Boolean {
        return str.all { it in BASE58_ALPHABET }
    }

    private fun isHex(str: String): Boolean {
        return str.all { it in "0123456789abcdefABCDEF" }
    }

    /**
     * Inspects a crypto address string and returns the detected cryptocurrency network.
     */
    fun detectAddress(rawInput: String): DetectionResult? {
        val address = rawInput.trim()
        if (address.length < 10) return null

        // 1. Ethereum / EVM (0x + 40 hex digits)
        if (address.startsWith("0x", ignoreCase = true) && address.length == 42) {
            val hexPart = address.substring(2)
            if (isHex(hexPart)) {
                return DetectionResult(
                    cryptoType = CryptoType.ETHEREUM,
                    confidence = "Alta (100%)",
                    formatDescription = "Dirección EVM estándar (Ethereum, BNB Chain, Polygon, Arbitrum, USDT ERC-20)",
                    alternativeTypes = listOf(CryptoType.ETHEREUM, CryptoType.USDT_ERC20)
                )
            }
        }

        // 2. Bitcoin Bech32 Native SegWit (bc1q...) or Taproot (bc1p...)
        if (address.startsWith("bc1", ignoreCase = true)) {
            val lower = address.lowercase()
            val bechPart = lower.substring(3)
            if (bechPart.all { it in BECH32_ALPHABET }) {
                val isTaproot = lower.startsWith("bc1p")
                return DetectionResult(
                    cryptoType = CryptoType.BITCOIN,
                    confidence = "Alta (100%)",
                    formatDescription = if (isTaproot) "Bitcoin Taproot (Bech32m)" else "Bitcoin SegWit Nativo (Bech32)",
                    alternativeTypes = listOf(CryptoType.BITCOIN)
                )
            }
        }

        // 3. Bitcoin Testnet (tb1...)
        if (address.startsWith("tb1", ignoreCase = true)) {
            return DetectionResult(
                cryptoType = CryptoType.BITCOIN,
                confidence = "Alta (Testnet)",
                formatDescription = "Bitcoin Testnet Bech32",
                alternativeTypes = listOf(CryptoType.BITCOIN)
            )
        }

        // 4. TRON (Starts with 'T' and length 34 base58) -> Commonly used for TRX and USDT TRC-20
        if (address.startsWith("T") && address.length == 34 && isBase58(address)) {
            return DetectionResult(
                cryptoType = CryptoType.USDT_TRC20,
                confidence = "Alta (100%)",
                formatDescription = "Red TRON (TRX / USDT TRC-20 formato Base58)",
                alternativeTypes = listOf(CryptoType.USDT_TRC20, CryptoType.TRON)
            )
        }

        // 5. Litecoin Bech32 (ltc1...)
        if (address.startsWith("ltc1", ignoreCase = true)) {
            return DetectionResult(
                cryptoType = CryptoType.LITECOIN,
                confidence = "Alta (100%)",
                formatDescription = "Litecoin SegWit Nativo (Bech32)",
                alternativeTypes = listOf(CryptoType.LITECOIN)
            )
        }

        // 6. Cardano Shelley (addr1...)
        if (address.startsWith("addr1", ignoreCase = true) && address.length in 58..110) {
            return DetectionResult(
                cryptoType = CryptoType.CARDANO,
                confidence = "Alta (100%)",
                formatDescription = "Cardano Shelley Address (Bech32)",
                alternativeTypes = listOf(CryptoType.CARDANO)
            )
        }

        // 7. TON (EQ... or UQ... base64url ~48 chars)
        if ((address.startsWith("EQ") || address.startsWith("UQ") || address.startsWith("kQ")) && address.length in 46..50) {
            return DetectionResult(
                cryptoType = CryptoType.TON,
                confidence = "Alta (100%)",
                formatDescription = "The Open Network (TON) User-Friendly Address",
                alternativeTypes = listOf(CryptoType.TON)
            )
        }

        // 8. Ripple XRP (Starts with 'r', length 25-35, base58)
        if (address.startsWith("r") && address.length in 25..35 && isBase58(address)) {
            return DetectionResult(
                cryptoType = CryptoType.RIPPLE,
                confidence = "Alta (95%)",
                formatDescription = "XRP Ledger Classic Address",
                alternativeTypes = listOf(CryptoType.RIPPLE)
            )
        }

        // 9. Dogecoin (Starts with 'D', length 34, base58)
        if (address.startsWith("D") && address.length == 34 && isBase58(address)) {
            return DetectionResult(
                cryptoType = CryptoType.DOGECOIN,
                confidence = "Alta (95%)",
                formatDescription = "Dogecoin Legacy Address",
                alternativeTypes = listOf(CryptoType.DOGECOIN)
            )
        }

        // 10. Litecoin Legacy (Starts with 'L' or 'M', length 26-35, base58)
        if ((address.startsWith("L") || address.startsWith("M")) && address.length in 26..35 && isBase58(address)) {
            return DetectionResult(
                cryptoType = CryptoType.LITECOIN,
                confidence = "Alta (95%)",
                formatDescription = "Litecoin Legacy / P2SH Address",
                alternativeTypes = listOf(CryptoType.LITECOIN)
            )
        }

        // 11. Bitcoin Legacy (starts with '1') or SegWit P2SH (starts with '3')
        if ((address.startsWith("1") || address.startsWith("3")) && address.length in 26..35 && isBase58(address)) {
            return DetectionResult(
                cryptoType = CryptoType.BITCOIN,
                confidence = "Alta (98%)",
                formatDescription = if (address.startsWith("1")) "Bitcoin Legacy (P2PKH)" else "Bitcoin SegWit / Multi-sig (P2SH)",
                alternativeTypes = listOf(CryptoType.BITCOIN)
            )
        }

        // 12. Solana (Base58, length 32 to 44 characters, usually 43-44)
        if (address.length in 32..44 && isBase58(address) && !address.startsWith("0x")) {
            return DetectionResult(
                cryptoType = CryptoType.SOLANA,
                confidence = "Alta (90%)",
                formatDescription = "Solana Public Key (Base58)",
                alternativeTypes = listOf(CryptoType.SOLANA)
            )
        }

        return null
    }

    /**
     * Returns a public block explorer URL for a given crypto type and address
     */
    fun getExplorerUrl(cryptoType: CryptoType, address: String): String {
        return when (cryptoType) {
            CryptoType.BITCOIN -> "https://www.blockchain.com/explorer/addresses/btc/$address"
            CryptoType.ETHEREUM, CryptoType.USDT_ERC20 -> "https://etherscan.io/address/$address"
            CryptoType.SOLANA -> "https://solscan.io/account/$address"
            CryptoType.TRON, CryptoType.USDT_TRC20 -> "https://tronscan.org/#/address/$address"
            CryptoType.LITECOIN -> "https://blockchair.com/litecoin/address/$address"
            CryptoType.DOGECOIN -> "https://dogechain.info/address/$address"
            CryptoType.CARDANO -> "https://cardanoscan.io/address/$address"
            CryptoType.TON -> "https://tonscan.org/address/$address"
            CryptoType.RIPPLE -> "https://xrpscan.com/account/$address"
            CryptoType.OTHER -> "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(address, "UTF-8")}"
        }
    }
}
