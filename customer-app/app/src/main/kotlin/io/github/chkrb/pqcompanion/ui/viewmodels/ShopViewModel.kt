package io.github.chkrb.pqcompanion.ui.viewmodels

import android.util.Log
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.chkrb.pqcompanion.R
import io.github.chkrb.pqcompanion.data.Catalog
import io.github.chkrb.pqcompanion.data.OrderRequest
import io.github.chkrb.pqcompanion.data.RetailerStatus
import kotlinx.coroutines.launch
import java.io.InputStream

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
class ShopViewModel(globalCatalogStream: InputStream) : ViewModel() {
    var catalog: Catalog
    var retailerStatus: RetailerStatus? = null
    var retailerStatusError = false
    var orderRequestData = ubyteArrayOf()

    init {
        catalog = Catalog.loadFromJsonStream(globalCatalogStream)
    }

    fun reset() {
        retailerStatus = null
        retailerStatusError = false
        orderRequestData = ubyteArrayOf()
    }

    fun processRetailerStatusData(data: UByteArray) {
        viewModelScope.launch {
            try {
                retailerStatus = RetailerStatus.loadFromPosData(data, catalog)
            } catch (e: Exception) {
                e.printStackTrace()
                retailerStatus = null
                retailerStatusError = true
            }
        }
    }

    fun processOrderRequest(orderRequest: OrderRequest) {
        orderRequestData = orderRequest.toUByteArray()
    }
}

class ShopViewModelFactory(private val globalCatalogStream: InputStream) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(ShopViewModel::class.java) -> {
                ShopViewModel(globalCatalogStream) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class $modelClass")
        }
}
