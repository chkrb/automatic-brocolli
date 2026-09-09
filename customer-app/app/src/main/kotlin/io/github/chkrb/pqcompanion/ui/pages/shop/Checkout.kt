package io.github.chkrb.pqcompanion.ui.pages.shop

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nayuki.qrcodegen.QrCode
import io.nayuki.qrcodegen.QrSegment
import io.github.chkrb.pqcompanion.presentation.PagedData
import io.github.chkrb.pqcompanion.ui.viewmodels.ShopViewModel
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
fun dataToQrBitmap(
    content: UByteArray,
    lowColorArgb: Int,
    highColorArgb: Int,
    version: Int,
): ImageBitmap {
    val segment = QrSegment.makeBytes(content.toByteArray())
    val qr = QrCode.encodeSegments(listOf(segment), QrCode.Ecc.LOW, version, version, -1, true)
    val bitmap = Bitmap.createBitmap(qr.size, qr.size, Bitmap.Config.ARGB_8888)

    for (x in 0 until bitmap.width) {
        for (y in 0 until bitmap.height) {
            val high = qr.getModule(x, y)
            bitmap.setPixel(x, y, if (high) highColorArgb else lowColorArgb)
        }
    }

    return bitmap.asImageBitmap()
}

@Composable
@OptIn(kotlin.ExperimentalUnsignedTypes::class)
fun ShopCheckoutPage(navController: NavController, vm: ShopViewModel) {
    assert(vm.orderRequestData.size > 0)

    val qrLowColorArgb = MaterialTheme.colorScheme.background.toArgb()
    val qrHighColorArgb = MaterialTheme.colorScheme.onBackground.toArgb()

    var dataPageIndex by remember { mutableStateOf(0) }
    val dataPageBitmaps by remember {
        // Reference: https://www.qrcode.com/en/about/version.html
        val qrVersionBytes =
            arrayOf(17, 32, 53, 78, 106, 134, 154, 192, 230, 271, 321, 367, 425, 458, 520, 586, 644, 718, 792, 858)
        var qrVersion = 1
        var qrBytes = qrVersionBytes[0]

        var lo = 0
        var hi = qrVersionBytes.size - 1

        while (lo < hi) {
            val mid = lo + (hi - lo) / 2
            qrVersion = mid + 1
            qrBytes = qrVersionBytes[mid]

            if (qrBytes == vm.orderRequestData.size) break
            else if (qrBytes < vm.orderRequestData.size) lo = mid + 1
            else hi = mid - 1
        }

        mutableStateOf(
            PagedData()
                .getDataPagesFromData(vm.orderRequestData, qrBytes)
                .map { dataToQrBitmap(it, qrLowColorArgb, qrHighColorArgb, qrVersion) }
        )
    }

    LaunchedEffect(dataPageBitmaps) {
        while (true) {
            delay(500)

            if (dataPageBitmaps.size > 0) {
                dataPageIndex = (dataPageIndex + 1) % dataPageBitmaps.size
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                bitmap = dataPageBitmaps[dataPageIndex],
                contentDescription = "qr",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        }
    }
}
