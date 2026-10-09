package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PriceService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Default reference prices in USD if offline or waiting
    private val cachedPrices = mutableMapOf(
        "bitcoin" to 67500.0,
        "ethereum" to 3520.0,
        "solana" to 155.0,
        "tether" to 1.0,
        "tron" to 0.16,
        "litecoin" to 72.0,
        "dogecoin" to 0.14,
        "cardano" to 0.38,
        "the-open-network" to 5.80,
        "ripple" to 0.58
    )

    private var lastFetchTime = 0L

    suspend fun getPrices(): Map<String, Double> {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime < 60_000 && lastFetchTime > 0) {
            return cachedPrices.toMap()
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,tron,tether,litecoin,dogecoin,cardano,ripple,the-open-network&vs_currencies=usd"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CryptoWalletApp/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val json = JSONObject(body)
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val obj = json.getJSONObject(key)
                                if (obj.has("usd")) {
                                    cachedPrices[key] = obj.getDouble("usd")
                                }
                            }
                            lastFetchTime = now
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep cached/default fallback values without crashing
            }
            cachedPrices.toMap()
        }
    }

    fun getUsdPriceFor(cryptoType: CryptoType): Double {
        if (cryptoType.coingeckoId.isEmpty()) return 1.0
        return cachedPrices[cryptoType.coingeckoId] ?: when (cryptoType) {
            CryptoType.BITCOIN -> 67500.0
            CryptoType.ETHEREUM -> 3520.0
            CryptoType.SOLANA -> 155.0
            CryptoType.USDT_TRC20, CryptoType.USDT_ERC20 -> 1.0
            CryptoType.TRON -> 0.16
            CryptoType.LITECOIN -> 72.0
            CryptoType.DOGECOIN -> 0.14
            CryptoType.CARDANO -> 0.38
            CryptoType.TON -> 5.80
            CryptoType.RIPPLE -> 0.58
            CryptoType.OTHER -> 1.0
        }
    }
}
