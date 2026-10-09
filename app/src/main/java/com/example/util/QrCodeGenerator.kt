package com.example.util

/**
 * Pure Kotlin QR Code Matrix generator (Version 1-10 Byte Mode, ECC Low/Medium).
 * Encodes text (e.g. crypto addresses) into a boolean matrix of modules.
 */
object QrCodeGenerator {

    /**
     * Generates a 2D Boolean array representing the QR code matrix (true = dark, false = light).
     */
    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        val len = bytes.size

        // Select minimal version (V1 to V7) capable of holding len bytes with Medium ECC
        // Capacity in bytes for Medium ECC:
        // V1: 14, V2: 26, V3: 42, V4: 62, V5: 84, V6: 106, V7: 122
        val (version, dataCodewordsCount, ecCodewordsCount) = when {
            len <= 14 -> Triple(1, 16, 10)
            len <= 26 -> Triple(2, 28, 16)
            len <= 42 -> Triple(3, 44, 26)
            len <= 62 -> Triple(4, 64, 36)
            len <= 84 -> Triple(5, 86, 48)
            len <= 106 -> Triple(6, 108, 64)
            else -> Triple(7, 124, 72)
        }

        val size = version * 4 + 17
        val matrix = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        // 1. Finder patterns (7x7) + separators
        fun placeFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val mr = top + r
                    val mc = left + c
                    if (mr in 0 until size && mc in 0 until size) {
                        isFunction[mr][mc] = true
                        val isDark = (r in 0..6 && (c == 0 || c == 6)) ||
                                (c in 0..6 && (r == 0 || r == 6)) ||
                                (r in 2..4 && c in 2..4)
                        matrix[mr][mc] = isDark
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        // 2. Alignment patterns (for Version >= 2)
        if (version >= 2) {
            val alignPos = getAlignmentPatternPositions(version)
            for (r in alignPos) {
                for (c in alignPos) {
                    // Skip if overlapping finder patterns
                    if ((r <= 8 && c <= 8) || (r <= 8 && c >= size - 8) || (r >= size - 8 && c <= 8)) continue
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val mr = r + dr
                            val mc = c + dc
                            isFunction[mr][mc] = true
                            matrix[mr][mc] = (dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0))
                        }
                    }
                }
            }
        }

        // 3. Timing patterns
        for (i in 8 until size - 8) {
            isFunction[6][i] = true
            matrix[6][i] = (i % 2 == 0)
            isFunction[i][6] = true
            matrix[i][6] = (i % 2 == 0)
        }

        // 4. Dark module
        isFunction[size - 8][8] = true
        matrix[size - 8][8] = true

        // 5. Reserve format information areas
        for (i in 0..8) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
        for (i in 0..7) {
            isFunction[8][size - 1 - i] = true
            isFunction[size - 1 - i][8] = true
        }

        // 6. Encode Data (Byte mode: indicator 0100, 8-bit length for V1-V9)
        val bitBuffer = mutableListOf<Int>()
        fun putBits(value: Int, numBits: Int) {
            for (i in numBits - 1 downTo 0) {
                bitBuffer.add((value ushr i) and 1)
            }
        }

        // Mode: Byte (0100)
        putBits(4, 4)
        // Character count indicator (8 bits for V1-V9)
        putBits(len, 8)
        // Data bytes
        for (b in bytes) {
            putBits(b.toInt() and 0xFF, 8)
        }

        // Terminator (up to 4 zeroes)
        val maxDataBits = dataCodewordsCount * 8
        val terminatorBits = minOf(4, maxDataBits - bitBuffer.size)
        repeat(terminatorBits) { bitBuffer.add(0) }

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < maxDataBits) {
            bitBuffer.add(0)
        }

        // Pad bytes (0xEC, 0x11 alternating)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < maxDataBits) {
            putBits(padBytes[padIdx % 2], 8)
            padIdx++
        }

        // Convert bit buffer to data codewords
        val dataCodewords = IntArray(dataCodewordsCount)
        for (i in 0 until dataCodewordsCount) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bitBuffer[i * 8 + b]
            }
            dataCodewords[i] = byteVal
        }

        // Error correction calculation
        val ecCodewords = calculateErrorCorrection(dataCodewords, ecCodewordsCount)
        val allCodewords = dataCodewords + ecCodewords

        // Convert all codewords to bits
        val allBits = mutableListOf<Int>()
        for (cw in allCodewords) {
            for (i in 7 downTo 0) {
                allBits.add((cw ushr i) and 1)
            }
        }

        // 7. Place data bits into matrix
        var bitIndex = 0
        var right = size - 1
        var upwards = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing pattern column

            val rows = if (upwards) (size - 1 downTo 0).toList() else (0 until size).toList()
            for (row in rows) {
                for (col in intArrayOf(right, right - 1)) {
                    if (!isFunction[row][col]) {
                        val bit = if (bitIndex < allBits.size) allBits[bitIndex++] else 0
                        // Apply Mask 0: (row + col) % 2 == 0
                        val mask = (row + col) % 2 == 0
                        matrix[row][col] = if (mask) bit == 0 else bit == 1
                    }
                }
            }
            upwards = !upwards
            right -= 2
        }

        // 8. Format Information: ECC Level M (00) + Mask 0 (000) -> 00000
        // BCH code for 00000 with mask 101010000010010 is 101010000010010
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        // Around top-left finder
        val formatCoords1 = listOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
            Pair(8, 7), Pair(8, 8), Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8),
            Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        for (i in 0 until 15) {
            val (r, c) = formatCoords1[i]
            matrix[r][c] = formatBits[i] == 1
        }
        // Around other finders
        for (i in 0..6) {
            matrix[size - 1 - i][8] = formatBits[i] == 1
        }
        for (i in 0..7) {
            matrix[8][size - 8 + i] = formatBits[7 + i] == 1
        }

        return matrix
    }

    private fun getAlignmentPatternPositions(version: Int): IntArray {
        return when (version) {
            2 -> intArrayOf(6, 18)
            3 -> intArrayOf(6, 22)
            4 -> intArrayOf(6, 26)
            5 -> intArrayOf(6, 30)
            6 -> intArrayOf(6, 34)
            7 -> intArrayOf(6, 22, 38)
            else -> intArrayOf()
        }
    }

    // Reed-Solomon Code calculation in GF(2^8) with mod 0x11D
    private fun calculateErrorCorrection(data: IntArray, ecCount: Int): IntArray {
        // GF(2^8) tables
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            exp[i + 255] = x
            log[x] = i
            x = (x shl 1) xor (if (x >= 128) 0x11D else 0)
        }

        fun gfMul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return exp[log[a] + log[b]]
        }

        // Generator polynomial
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val factor = intArrayOf(exp[i], 1)
            val nextGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMul(gen[j], factor[0])
                nextGen[j + 1] = nextGen[j + 1] xor gfMul(gen[j], factor[1])
            }
            gen = nextGen
        }

        // Remainder division
        val remainder = IntArray(ecCount)
        for (dataByte in data) {
            val factor = dataByte xor remainder[0]
            for (j in 0 until ecCount - 1) {
                remainder[j] = remainder[j + 1] xor gfMul(gen[ecCount - 1 - j], factor)
            }
            remainder[ecCount - 1] = gfMul(gen[0], factor)
        }

        return remainder.reversedArray()
    }
}
