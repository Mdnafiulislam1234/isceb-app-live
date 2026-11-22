package com.example.util

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Self-contained pure-Kotlin ISO/IEC 18004 Byte-mode QR Code matrix generator
 * (Versions 1..10, Error Correction Level M).
 * Zero external network or third-party dependencies so certificates always carry
 * verifiable offline + PDF QR codes.
 */
object QrCodeGenerator {

    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfLog[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11d
        }
        for (i in 255 until 512) {
            gfExp[i] = gfExp[i - 255]
        }
    }

    private data class QrSpec(
        val version: Int,
        val totalDataCodewords: Int,
        val ecPerBlock: Int,
        val g1Count: Int,
        val g1Data: Int,
        val g2Count: Int,
        val g2Data: Int,
        val alignCenters: IntArray
    )

    private val qrSpecsM = listOf(
        QrSpec(1, 16, 10, 1, 16, 0, 0, intArrayOf()),
        QrSpec(2, 28, 16, 1, 28, 0, 0, intArrayOf(6, 18)),
        QrSpec(3, 44, 26, 1, 44, 0, 0, intArrayOf(6, 22)),
        QrSpec(4, 64, 18, 2, 32, 0, 0, intArrayOf(6, 26)),
        QrSpec(5, 86, 24, 2, 43, 0, 0, intArrayOf(6, 30)),
        QrSpec(6, 108, 16, 4, 27, 0, 0, intArrayOf(6, 34)),
        QrSpec(7, 124, 18, 4, 31, 0, 0, intArrayOf(6, 22, 38)),
        QrSpec(8, 154, 22, 2, 38, 2, 39, intArrayOf(6, 24, 42)),
        QrSpec(9, 182, 22, 3, 36, 2, 37, intArrayOf(6, 26, 46)),
        QrSpec(10, 216, 26, 4, 43, 1, 44, intArrayOf(6, 28, 50))
    )

    private val versionInfoBch = mapOf(
        7 to 0x07c94,
        8 to 0x085bc,
        9 to 0x09a99,
        10 to 0x0a4d3
    )

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val factor = gfExp[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
            }
            poly = next
        }
        return poly
    }

    private fun rsRemainder(data: IntArray, ecCount: Int): IntArray {
        val gen = rsGeneratorPoly(ecCount)
        val res = IntArray(ecCount)
        for (d in data) {
            val factor = d xor res[0]
            System.arraycopy(res, 1, res, 0, ecCount - 1)
            res[ecCount - 1] = 0
            if (factor != 0) {
                for (j in 0 until ecCount) {
                    res[j] = res[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return res
    }

    private fun chooseSpec(byteLen: Int): QrSpec {
        for (spec in qrSpecsM) {
            val headerBits = 4 + if (spec.version >= 10) 16 else 8
            val neededCodewords = ceil((headerBits + byteLen * 8 + 4) / 8.0).toInt()
            if (neededCodewords <= spec.totalDataCodewords) return spec
        }
        // Fallback truncate-safe to V10 spec if extreme payload
        return qrSpecsM.last()
    }

    private fun encodeDataCodewords(bytes: ByteArray, spec: QrSpec): IntArray {
        val maxPayloadBytes = (spec.totalDataCodewords * 8 - 24) / 8
        val safeBytes = if (bytes.size > maxPayloadBytes) bytes.copyOf(maxPayloadBytes) else bytes
        val bits = ArrayList<Int>(spec.totalDataCodewords * 8)
        fun pushBits(value: Int, len: Int) {
            for (i in len - 1 downTo 0) bits.add((value ushr i) and 1)
        }
        pushBits(0b0100, 4) // Byte mode
        pushBits(safeBytes.size, if (spec.version >= 10) 16 else 8)
        for (b in safeBytes) pushBits(b.toInt() and 0xFF, 8)
        val maxBits = spec.totalDataCodewords * 8
        pushBits(0, minOf(4, maxBits - bits.size))
        while (bits.size % 8 != 0) bits.add(0)
        val pads = intArrayOf(0xEC, 0x11)
        var pi = 0
        while (bits.size < maxBits) {
            pushBits(pads[pi % 2], 8)
            pi++
        }
        return IntArray(spec.totalDataCodewords) { i ->
            var v = 0
            for (b in 0 until 8) v = (v shl 1) or bits[i * 8 + b]
            v
        }
    }

    private fun buildFinalBytes(dataCodewords: IntArray, spec: QrSpec): IntArray {
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0
        repeat(spec.g1Count) {
            val blk = dataCodewords.copyOfRange(offset, offset + spec.g1Data)
            offset += spec.g1Data
            dataBlocks.add(blk)
            ecBlocks.add(rsRemainder(blk, spec.ecPerBlock))
        }
        repeat(spec.g2Count) {
            val blk = dataCodewords.copyOfRange(offset, offset + spec.g2Data)
            offset += spec.g2Data
            dataBlocks.add(blk)
            ecBlocks.add(rsRemainder(blk, spec.ecPerBlock))
        }
        val result = ArrayList<Int>()
        val maxD = max(spec.g1Data, spec.g2Data)
        for (i in 0 until maxD) {
            for (blk in dataBlocks) if (i < blk.size) result.add(blk[i])
        }
        for (i in 0 until spec.ecPerBlock) {
            for (blk in ecBlocks) result.add(blk[i])
        }
        return result.toIntArray()
    }

    private fun formatBitsForMask(maskId: Int): Int {
        val data = (0b00 shl 3) or maskId // ECC Level M = 00
        var rem = data shl 10
        val gen = 0x537
        for (i in 14 downTo 10) {
            if (((rem ushr i) and 1) != 0) rem = rem xor (gen shl (i - 10))
        }
        return ((data shl 10) or rem) xor 0x5412
    }

    fun generateQrMatrix(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val spec = chooseSpec(bytes.size)
        val size = spec.version * 4 + 17
        val modules = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun setFixed(r: Int, c: Int, dark: Boolean) {
            modules[r][c] = dark
            reserved[r][c] = true
        }

        fun drawFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = top + r
                    val cc = left + c
                    if (rr !in 0 until size || cc !in 0 until size) continue
                    val inOuter = r in 0..6 && c in 0..6
                    val onBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val inCenter = r in 2..4 && c in 2..4
                    setFixed(rr, cc, inOuter && (onBorder || inCenter))
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        for (i in 8 until size - 8) {
            setFixed(6, i, i % 2 == 0)
            setFixed(i, 6, i % 2 == 0)
        }
        setFixed(size - 8, 8, true)

        for (rCenter in spec.alignCenters) {
            for (cCenter in spec.alignCenters) {
                if (reserved[rCenter][cCenter]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val dark = max(abs(dr), abs(dc)) != 1
                        setFixed(rCenter + dr, cCenter + dc, dark)
                    }
                }
            }
        }

        for (i in 0 until 9) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0 until 8) {
            reserved[8][size - 1 - i] = true
            if (i < 7) reserved[size - 1 - i][8] = true
        }

        versionInfoBch[spec.version]?.let { vBits ->
            for (i in 0 until 18) {
                val bit = ((vBits ushr i) and 1) == 1
                val r = i / 3
                val c = size - 11 + (i % 3)
                setFixed(r, c, bit)
                setFixed(c, r, bit)
            }
        }

        val finalBytes = buildFinalBytes(encodeDataCodewords(bytes, spec), spec)
        val bitStream = ArrayList<Int>(finalBytes.size * 8)
        for (byte in finalBytes) {
            for (b in 7 downTo 0) bitStream.add((byte ushr b) and 1)
        }

        val maskId = 0
        var bitIdx = 0
        var dirUp = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (i in 0 until size) {
                val r = if (dirUp) size - 1 - i else i
                for (colOffset in 0..1) {
                    val c = right - colOffset
                    if (reserved[r][c]) continue
                    val dataBit = if (bitIdx < bitStream.size) bitStream[bitIdx++] else 0
                    val maskBit = if ((r + c) % 2 == 0) 1 else 0
                    modules[r][c] = (dataBit xor maskBit) == 1
                }
            }
            dirUp = !dirUp
            right -= 2
        }

        val fmt = formatBitsForMask(maskId)
        for (i in 0 until 15) {
            val dark = ((fmt ushr i) and 1) == 1
            when {
                i < 6 -> modules[i][8] = dark
                i < 8 -> modules[i + 1][8] = dark
                else -> modules[size - 15 + i][8] = dark
            }
            when {
                i < 8 -> modules[8][size - 1 - i] = dark
                i < 9 -> modules[8][15 - i] = dark
                else -> modules[8][14 - i] = dark
            }
        }
        return modules
    }

    fun drawQrOnAndroidCanvas(
        canvas: Canvas,
        data: String,
        left: Float,
        top: Float,
        sizePx: Float,
        darkColor: Int,
        lightColor: Int
    ) {
        val matrix = generateQrMatrix(data)
        val n = matrix.size
        val quiet = 2
        val totalModules = n + quiet * 2
        val cell = sizePx / totalModules.toFloat()

        val bgPaint = Paint().apply {
            color = lightColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(left, top, left + sizePx, top + sizePx, bgPaint)

        val fgPaint = Paint().apply {
            color = darkColor
            style = Paint.Style.FILL
        }
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (matrix[r][c]) {
                    val x = left + (c + quiet) * cell
                    val y = top + (r + quiet) * cell
                    canvas.drawRect(x, y, x + cell + 0.35f, y + cell + 0.35f, fgPaint)
                }
            }
        }
    }
}
