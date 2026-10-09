package com.example.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.CryptoCardEntity
import com.example.data.CryptoRepository
import com.example.util.CryptoDetector
import com.example.util.CryptoType
import com.example.util.PriceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class WebBridge(
    private val context: Context,
    private val webView: WebView,
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val repository: CryptoRepository by lazy {
        val db = AppDatabase.getInstance(context)
        CryptoRepository(db.cryptoCardDao())
    }

    private val clipboard: ClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    @JavascriptInterface
    fun getInitialData() {
        scope.launch(Dispatchers.IO) {
            pushCardsAndPrices()
        }
    }

    @JavascriptInterface
    fun detectAddress(rawAddress: String): String {
        val result = CryptoDetector.detectAddress(rawAddress) ?: return "{}"
        val json = JSONObject()
        json.put("code", result.cryptoType.code)
        json.put("name", result.cryptoType.displayName)
        json.put("network", result.cryptoType.networkName)
        json.put("desc", result.formatDescription)
        json.put("confidence", result.confidence)

        val alts = JSONArray()
        result.alternativeTypes.forEach { alts.put(it.code) }
        json.put("alternatives", alts)

        return json.toString()
    }

    @JavascriptInterface
    fun saveCard(cardJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val obj = JSONObject(cardJson)
                val id = obj.optLong("id", 0L)
                val name = obj.getString("name")
                val address = obj.getString("address")
                val cryptoCode = obj.getString("cryptoCode")
                val cryptoName = obj.optString("cryptoName", "Bitcoin")
                val networkName = obj.optString("networkName", "Red Bitcoin")
                val balance = obj.optDouble("balance", 0.0)
                val notes = obj.optString("notes", "")
                val colorHex = obj.optLong("colorHex", 0xFF7C3AED)

                val entity = CryptoCardEntity(
                    id = id,
                    name = name,
                    address = address,
                    cryptoCode = cryptoCode,
                    cryptoName = cryptoName,
                    networkName = networkName,
                    balance = balance,
                    notes = notes,
                    colorHex = colorHex
                )

                if (id == 0L) {
                    repository.insertCard(entity)
                } else {
                    repository.updateCard(entity)
                }

                pushCardsAndPrices()
                showToastOnMain("Tarjeta guardada con éxito")
            } catch (e: Exception) {
                showToastOnMain("Error al guardar tarjeta: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun deleteCard(cardId: Long) {
        scope.launch(Dispatchers.IO) {
            repository.deleteCardById(cardId)
            pushCardsAndPrices()
            showToastOnMain("Tarjeta eliminada")
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        mainHandler.post {
            val clip = ClipData.newPlainText("Dirección Cripto", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Dirección copiada al portapapeles", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getClipboardText(): String {
        return try {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString() ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun shareAddress(name: String, network: String, address: String) {
        mainHandler.post {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$name ($network):\n$address")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir dirección").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo compartir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun openExplorer(cryptoCode: String, address: String) {
        mainHandler.post {
            try {
                val crypto = CryptoType.fromCode(cryptoCode)
                val url = CryptoDetector.getExplorerUrl(crypto, address)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun refreshPrices() {
        scope.launch(Dispatchers.IO) {
            val prices = PriceService.getPrices()
            pushCardsAndPrices(newPrices = prices)
            showToastOnMain("Cotizaciones actualizadas")
        }
    }

    private suspend fun pushCardsAndPrices(newPrices: Map<String, Double>? = null) {
        val cardsList = repository.allCards.first()
        val cardsArray = JSONArray()

        cardsList.forEach { card ->
            val cObj = JSONObject()
            cObj.put("id", card.id)
            cObj.put("name", card.name)
            cObj.put("address", card.address)
            cObj.put("cryptoCode", card.cryptoCode)
            cObj.put("cryptoName", card.cryptoName)
            cObj.put("networkName", card.networkName)
            cObj.put("balance", card.balance)
            cObj.put("notes", card.notes)
            cObj.put("colorHex", card.colorHex)
            cardsArray.put(cObj)
        }

        val pricesMap = newPrices ?: PriceService.getPrices()
        val pricesObj = JSONObject()
        pricesMap.forEach { (k, v) -> pricesObj.put(k, v) }

        val jsScript = "if (window.onDataUpdated) { window.onDataUpdated(${JSONObject.quote(cardsArray.toString())}, ${JSONObject.quote(pricesObj.toString())}); }"

        mainHandler.post {
            webView.evaluateJavascript(jsScript, null)
        }
    }

    private fun showToastOnMain(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
