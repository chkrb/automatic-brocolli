package io.github.chkrb.pqcompanion.presentation

import android.util.Log
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
class PagedData {
    // WARN: `dataPages` has a method `.size()` which reports how many elements
    // are in the map. However, the return type is Int. Therefore the
    // implementation cannot use this method to check how many elements are in
    // the map because there can be a maximum of 2^16 pages.
    //
    // The solution is to keep track of the number of elements externally.
    // However we require a data type which can store any value from 0 to 2^16.
    // We can instead sneakily fit this into a ULong instead, with some compromises.
    //
    // For starters, we keep track of one less than the number of elements
    // present, i.e. -1 to 2^16 - 1. The -1 value represents an empty map, thus
    // we can only reliably check that variable if `dataPages.isEmpty()` is `true`.
    private var dataPages = mutableMapOf<ULong, UByteArray>()
    private var numDataPagesMinusOne = 0uL
    private var lastDataPageNumber = ULong.MAX_VALUE

    fun addDataPageAndConstruct(page: UByteArray): UByteArray? {
        // Data is divided into one or more pages, and a header is added to each page.
        // - The first byte stores the meta info:
        //   - bit 7 indicates that the page is the final page in sequence.
        //   - bits 6:3 are reserved.
        //   - bits 2:0 indicates the number of bytes required to store the page
        //     number, minus 1.
        // - The next byte(s) store the variable-width page number.

        val headerMeta = page[0].toUInt()
        val headerMetaLastPage = headerMeta shr 7 != 0u
        val headerMetaPageBytes = (headerMeta and 0b00000111u) + 1u

        var headerPageNumber = 0uL
        for (i in 0..<headerMetaPageBytes.toInt()) {
            headerPageNumber =
                headerPageNumber or (page[i + 1].toULong() shl (8 * i))
        }

        if (headerPageNumber !in dataPages.keys) {
            // We assume that the total number of pages is the maximum possible
            // pages. However if the last page as indicated by the header is
            // received, the total number of pages is changed.
            //
            // NOTE: This logic is very fragile, one may mix pages two different
            // POSes, which assembles garbage data.
            if (headerMetaLastPage && lastDataPageNumber == ULong.MAX_VALUE) {
                Log.d(this.javaClass.name, "this is the last page, correct expecting pages")
                lastDataPageNumber = headerPageNumber
            }

            val dataPagesNotEmpty = !dataPages.isEmpty()
            dataPages[headerPageNumber] =
                page.filterIndexed { index, byte -> index >= headerMetaPageBytes.toInt() + 1 }
                    .toUByteArray()

            if (dataPagesNotEmpty) numDataPagesMinusOne++

            Log.d(this.javaClass.name, "received page $headerPageNumber")
        }

        if (numDataPagesMinusOne == lastDataPageNumber) {
            var accum = ubyteArrayOf()

            for (i in 0uL..lastDataPageNumber) {
                if (dataPages[i] == null) return null
                accum += dataPages[i]!!
            }

            return accum
        }

        return null
    }

    fun getDataPagesFromData(data: UByteArray, pageSize: Int): List<UByteArray> {
        // Data is divided into one or more pages, and a header is added to each page.
        // - The first byte stores the meta info:
        //   - bit 7 indicates that the page is the final page in sequence.
        //   - bits 6:3 are reserved.
        //   - bits 2:0 indicates the number of bytes required to store the page
        //     number, minus 1.
        // - The next byte(s) store the variable-width page number.
        
        var page = 0uL
        var dataOffset = 0

        assert(pageSize > 5) // Header (1 byte) + Page Number (max 4 bytes)

        while (dataOffset < data.size) {
            var headerMeta = 0u

            val headerMetaPageNumberBytes = max(ceil(log2(page.toDouble() + 1.0) / 8).toUInt(), 1u)
            assert(headerMetaPageNumberBytes <= 8u)
            headerMeta = headerMeta or (headerMetaPageNumberBytes - 1u)

            var headerPageNumber = ubyteArrayOf()
            for (i in 0..<headerMetaPageNumberBytes.toInt()) {
                headerPageNumber += ((page shr (8 * i)) and 0xffu).toUByte()
            }

            val headerLength = 1 + headerMetaPageNumberBytes.toInt()
            val dataLength = pageSize - headerLength

            val pageData =
                data.filterIndexed {
                    index, byte -> index in dataOffset..<(dataOffset + dataLength)
                }.toUByteArray()

            dataOffset += dataLength
            if (dataOffset >= data.size) {
                headerMeta = headerMeta or (1u shl 7)
            }

            dataPages[page++] = ubyteArrayOf(headerMeta.toUByte()) + headerPageNumber + pageData
        }

        return dataPages.values.toList()
    }
}
