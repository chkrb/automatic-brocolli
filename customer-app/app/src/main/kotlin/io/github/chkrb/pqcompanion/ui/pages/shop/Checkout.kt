package io.github.chkrb.pqcompanion.ui.pages.shop

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.core.view.WindowCompat
import io.github.chkrb.pqcompanion.presentation.PagedData
import io.github.chkrb.pqcompanion.ui.NavDestination
import io.github.chkrb.pqcompanion.ui.icons.iconArrowBack
import io.github.chkrb.pqcompanion.ui.viewmodels.ShopViewModel
import io.nayuki.qrcodegen.QrCode
import io.nayuki.qrcodegen.QrSegment
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---- Palette mirrored from ShopExplorePage (off-white base + terracotta accent) ----
private val BackgroundOffWhite = Color(0xFFFAF6EC)
private val CardBackground = Color(0xFFFFFFFF)
private val AccentTerracotta = Color(0xFFC1694F)
private val TextPrimary = Color(0xFF2B2420)
private val TextSecondary = Color(0xFF8C8377)

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
@OptIn(kotlin.ExperimentalUnsignedTypes::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
fun ShopCheckoutPage(navController: NavController, vm: ShopViewModel) {
    // After the UI was slopped away from material 3 (and thus adopted a light
    // theme) this has to be added.
    val window = LocalActivity.current!!.window
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.isAppearanceLightStatusBars = true
    windowInsetsController.isAppearanceLightNavigationBars = true

    assert(vm.orderRequestData.size > 0)

    // QR modules are drawn straight onto the card background, so the code reads
    // cleanly against the theme instead of a default black-on-white square.
    val qrLowColorArgb = CardBackground.toArgb()
    val qrHighColorArgb = TextPrimary.toArgb()

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundOffWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Checkout",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = iconArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite,
                    scrolledContainerColor = BackgroundOffWhite,
                    titleContentColor = TextPrimary,
                ),
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOffWhite)
                .padding(scaffoldPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Show this code at the counter",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "The store will scan it to complete your order",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f),
                colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Guard against the brief window before the first page is ready,
                    // instead of indexing into an empty list.
                    val bitmap = dataPageBitmaps.getOrNull(dataPageIndex)
                    if (bitmap != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = bitmap,
                            contentDescription = "Order QR code",
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multi-page orders cycle through several QR codes; dots + a counter make
            // that flipping legible instead of looking like a glitch.
            if (dataPageBitmaps.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dataPageBitmaps.indices.forEach { index ->
                        Surface(
                            modifier = Modifier.size(if (index == dataPageIndex) 8.dp else 6.dp),
                            color = if (index == dataPageIndex) {
                                AccentTerracotta
                            } else {
                                TextSecondary.copy(alpha = 0.35f)
                            },
                            shape = RoundedCornerShape(50),
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Page ${dataPageIndex + 1} of ${dataPageBitmaps.size}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Order is considered handed off once the user leaves this screen via this
            // button, so the whole shop flow (Explore + Checkout) is cleared from the
            // back stack rather than just popped once.
            Button(
                onClick = {
                    navController.navigate(NavDestination.HOME.route()) {
                        popUpTo(NavDestination.HOME.route()) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTerracotta,
                    contentColor = Color.White,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 32.dp,
                    vertical = 12.dp,
                ),
            ) {
                Text(text = "Back to Home", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
