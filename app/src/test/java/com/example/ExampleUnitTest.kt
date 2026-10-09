package com.example

import com.example.util.CryptoDetector
import com.example.util.CryptoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testEthereumAddressDetection() {
        val ethAddress = "0x71C83605a2e618F2Eab3950A72B8ebcD48589cAb"
        val result = CryptoDetector.detectAddress(ethAddress)
        assertNotNull(result)
        assertEquals(CryptoType.ETHEREUM, result!!.cryptoType)
    }

    @Test
    fun testBitcoinSegWitAddressDetection() {
        val btcSegWit = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
        val result = CryptoDetector.detectAddress(btcSegWit)
        assertNotNull(result)
        assertEquals(CryptoType.BITCOIN, result!!.cryptoType)
    }

    @Test
    fun testBitcoinLegacyAddressDetection() {
        val btcLegacy = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val result = CryptoDetector.detectAddress(btcLegacy)
        assertNotNull(result)
        assertEquals(CryptoType.BITCOIN, result!!.cryptoType)
    }

    @Test
    fun testTronAddressDetection() {
        val tronAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val result = CryptoDetector.detectAddress(tronAddress)
        assertNotNull(result)
        assertEquals(CryptoType.USDT_TRC20, result!!.cryptoType)
    }

    @Test
    fun testSolanaAddressDetection() {
        val solAddress = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        val result = CryptoDetector.detectAddress(solAddress)
        assertNotNull(result)
        assertEquals(CryptoType.SOLANA, result!!.cryptoType)
    }

    @Test
    fun testCardanoAddressDetection() {
        val adaAddress = "addr1q9f2s4yqrv7g55v0x9d4h5cswffq6kkwvg3e96z55f6vhg4q05d045d4"
        val result = CryptoDetector.detectAddress(adaAddress)
        assertNotNull(result)
        assertEquals(CryptoType.CARDANO, result!!.cryptoType)
    }
}
