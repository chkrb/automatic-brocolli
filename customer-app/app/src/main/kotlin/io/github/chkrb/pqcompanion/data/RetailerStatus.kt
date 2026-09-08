package io.github.chkrb.pqcompanion.data

import android.util.Log

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
data class RetailerStatusProduct(
    /** The part of the UUID enough to uniquely identify the product from a catalog. (since v1) */
    val uuidFragment: UByteArray,
    /** The stock available in the retailer's inventory, or allocated to the POS. (since v1) */
    val availableStock: UInt,
    /** The selling price of the product set by the retailer. (since v1) */
    val sellingPrice: UInt,

    /** The product reference from the catalog. Derived from UUID fragment. */
    val catalogProduct: CatalogProduct,
)

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
data class RetailerStatus(
    /** Version of global catalog available in the merchant's server. (since v1) */
    val catalogVersion: UInt,
    /** Version of POS data specification this blob data follows. (since v1) */
    val posDataVersion: UInt,
    /** Array of products and their details. (since v1) */
    val products: List<RetailerStatusProduct>,
) {
    companion object {
        fun loadFromPosData(data: UByteArray, catalog: Catalog): RetailerStatus {
            // Catalog Version: 2 bytes, little-endian, unsigned non-zero integer.
            val catalogVersion = data[0].toUInt() or (data[1].toUInt() shl 8)
            assert(catalogVersion != 0u)

            // POS Data Version: 2 bytes, little-endian, unsigned non-zero integer.
            val posDataVersion = data[2].toUInt() or (data[3].toUInt() shl 8)
            assert(posDataVersion != 0u)

            // Products: Variable, array with sentinel.
            val products = when (posDataVersion) {
                1u -> loadProductListFromPosDataV1(data, catalog, catalogVersion, 4)
                else -> throw IllegalArgumentException("unknown or invalid POS data version")
            }

            return RetailerStatus(
                catalogVersion = catalogVersion,
                posDataVersion = posDataVersion,
                products = products,
            )
        }

        private fun loadProductListFromPosDataV1(
            data: UByteArray,
            catalog: Catalog,
            catalogVersion: UInt,
            offset: Int,
        ): List<RetailerStatusProduct> {
            var offset = offset
            val products = mutableListOf<RetailerStatusProduct>()

            while (true) {
                // Product: Header: 1 byte. 
                val header = data[offset++].toUInt()
                // Product: Header: UUID Fragment Field Length: 1 to 16. 
                val headerUuidFragmentBytes = (header and 0b00001111u) + 1u
                // Product: Header: Available Stock Field Length: 1 to 4. 
                val headerAvailableStockBytes = ((header and 0b00110000u) shr 4) + 1u
                // Product: Header: Selling Price Field Length: 1 to 4. 
                val headerSellingPriceBytes = ((header and 0b11000000u) shr 6) + 1u

                // Product: UUID Fragment: 1 to 16 bytes, big-endian.
                var uuidFragment = data.filterIndexed {
                    index, byte -> index in offset..<(offset + headerUuidFragmentBytes.toInt())
                }.toUByteArray()
                offset += headerUuidFragmentBytes.toInt()

                // Product: Available Stock: 1 to 4 bytes, little-endian, unsigned non-zero integer.
                var availableStock = 0u
                for (i in offset..<(offset + headerAvailableStockBytes.toInt())) {
                    availableStock = availableStock or (data[i].toUInt() shl (8 * (i - offset)))
                }
                offset += headerAvailableStockBytes.toInt()

                // Sneaky check to make sure this is not a sentinel guard.
                // A product can never be reported as having 0 stock, if that's the
                // case this is used as sentinel.
                if (availableStock == 0u) break

                // Product: Selling Price: 1 to 4 bytes, little-endian, unsigned integer.
                var sellingPrice = 0u
                for (i in offset..<(offset + headerSellingPriceBytes.toInt())) {
                    sellingPrice = sellingPrice or (data[i].toUInt() shl (8 * (i - offset)))
                }
                offset += headerSellingPriceBytes.toInt()

                val catalogProduct = catalog.searchProduct(uuidFragment, catalogVersion)

                if (catalogProduct != null) {
                    Log.d(this::class.java.name, "Product UUID: ${catalogProduct.uuid}")
                    Log.d(this::class.java.name, "Product Available Stock: $availableStock")
                    Log.d(this::class.java.name, "Product Selling Price: $sellingPrice")

                    products.add(
                        RetailerStatusProduct(
                            uuidFragment = uuidFragment,
                            availableStock = availableStock,
                            sellingPrice = sellingPrice,
                            catalogProduct = catalogProduct,
                        )
                    )
                }
            }

            return products
        }
    }
}
