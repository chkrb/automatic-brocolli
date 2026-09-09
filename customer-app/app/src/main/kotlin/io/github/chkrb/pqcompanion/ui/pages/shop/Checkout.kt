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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
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
    // Nabbed from https://dev.to/devniiaddy/qr-code-with-jetpack-compose-47e
    val encodeHints = mutableMapOf(
        EncodeHintType.QR_VERSION to version,
        EncodeHintType.CHARACTER_SET to "ISO-8859-1",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
        EncodeHintType.MARGIN to 0,
    )

    val bitmapMatrix =
        MultiFormatWriter().encode(
            String(content.toByteArray(), StandardCharsets.ISO_8859_1),
            BarcodeFormat.QR_CODE,
            199,
            199,
            encodeHints,
        )

    val bitmap = Bitmap.createBitmap(
        bitmapMatrix.width,
        bitmapMatrix.height,
        Bitmap.Config.ARGB_8888,
    )

    for (x in 0 until bitmapMatrix.width) {
        for (y in 0 until bitmapMatrix.height) {
            val high = bitmapMatrix?.get(x, y) ?: false
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
        // For ZXing, max bytes is one less than standard. WHY???
        var zxingQrVersion = 1
        var zxingQrBytes = qrVersionBytes[0]

        var lo = 0
        var hi = qrVersionBytes.size - 1

        while (lo < hi) {
            val mid = lo + (hi - lo) / 2
            zxingQrVersion = mid + 1
            zxingQrBytes = qrVersionBytes[mid] - 1

            if (zxingQrBytes == vm.orderRequestData.size) break
            else if (zxingQrBytes < vm.orderRequestData.size) lo = mid + 1
            else hi = mid - 1
        }

        mutableStateOf(
            PagedData().getDataPagesFromData(vm.orderRequestData, zxingQrBytes)
                .map {
                    dataToQrBitmap(
                        it,
                        qrLowColorArgb,
                        qrHighColorArgb,
                        zxingQrVersion,
                    )
                }
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
