package io.github.chkrb.pqcompanion.data

import android.util.Log
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
data class OrderRequestProduct(
    /** The part of the UUID enough to uniquely identify the product from a catalog. (since v1) */
    val uuidFragment: UByteArray,
    /** The stock of the product intended to be bought by the customer. (since v1) */
    var orderedStock: UInt,

    /** The product reference from the retailer inventory status. */
    val retailerStatusProduct: RetailerStatusProduct,
)

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
data class OrderRequest(
    /** Array of products and their details. (since v1) */
    val products: List<OrderRequestProduct>,

    /** Version of POS data specification the request is encoded in. */
    val posDataVersion: UInt,
) {
    companion object {
        fun loadFromRetailerStatus(retailerStatus: RetailerStatus): OrderRequest {
            return OrderRequest(
                products = retailerStatus.products.map { product ->
                    OrderRequestProduct(
                        uuidFragment = product.uuidFragment,
                        orderedStock = 0u,
                        retailerStatusProduct = product,
                    )
                },
                posDataVersion = retailerStatus.posDataVersion,
            )
        }
    }

    @OptIn(kotlin.ExperimentalUnsignedTypes::class)
    fun toUByteArray(): UByteArray {
        return when (posDataVersion) {
            1u -> toUByteArrayV1()
            else -> throw IllegalArgumentException("unknown or invalid POS data version")
        }
    }

    @OptIn(kotlin.ExperimentalUnsignedTypes::class)
    fun toUByteArrayV1(): UByteArray {
        var accum = ubyteArrayOf()

        for (product in products) {
            if (product.orderedStock == 0u) continue

            // Product: Header: UUID Fragment Field Length: 1 to 16. 
            val headerUuidFragmentBytes = product.uuidFragment.size.toUInt()
            // Product: Header: Ordered Stock Field Length: 1 to 4.
            val headerOrderedStockBytes =
                ceil(log2(product.orderedStock.toDouble() + 1) / 8).toUInt()
            // Product: Header: 1 byte.
            val header =
                ((headerUuidFragmentBytes - 1u) or ((headerOrderedStockBytes - 1u) shl 4))
                    .toUByte()

            // Product: UUID Fragment: 1 to 16 bytes, big-endian.
            val uuidFragment = product.uuidFragment

            // Product: Ordered Stock: 1 to 4 bytes, little-endian, unsigned non-zero integer.
            var orderedStock = ubyteArrayOf()
            for (i in 0..<headerOrderedStockBytes.toInt()) {
                orderedStock += (0xffu and product.orderedStock shl (8 * i)).toUByte()
            }

            accum += header
            accum += uuidFragment
            accum += orderedStock
        }

        return accum
    }
}
