package io.github.chkrb.pqcompanion.ui.pages.shop

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.chkrb.pqcompanion.presentation.PagedData
import io.github.chkrb.pqcompanion.data.RetailerStatus
import io.github.chkrb.pqcompanion.ui.NavDestination
import io.github.chkrb.pqcompanion.ui.viewmodels.ShopViewModel
import java.util.concurrent.Executors

@Composable
@OptIn(kotlin.ExperimentalUnsignedTypes::class)
fun ShopScanPage(
    navController: NavController,
    vm: ShopViewModel,
    storeName: String = "POSqueue"
) {
    LaunchedEffect(Unit) {
        vm.reset()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview { data ->
            vm.processRetailerStatusData(data)
            navController.navigate(NavDestination.SHOP_EXPLORE.route()) {
                popUpTo(NavDestination.HOME.route())
            }
        }

        // Scanner viewfinder, floating over the live camera feed
        ScannerFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
        )

        // Top scrim + store branding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .padding(top = 48.dp, bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = storeName,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
                Text(
                    text = "Scan the store code to check in",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Bottom scrim + instruction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
                .padding(top = 64.dp, bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hold steady and line up the QR code",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A rounded viewfinder outline with accented corner brackets, drawn on
 * [Canvas] so it needs no extra icon or graphics dependencies. Sits over
 * the live camera feed to show the user where to line up the QR code.
 */
@Composable
private fun ScannerFrame(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val cornerRadius = 24.dp.toPx()
        val bracketLength = size.minDimension * 0.18f
        val bracketStroke = 5.dp.toPx()
        val w = size.width
        val h = size.height

        // Soft translucent panel behind the frame
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
        // Thin outline for the whole frame
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Accented corner brackets, each defined by three points
        val corners = listOf(
            Triple(Offset(0f, bracketLength), Offset(0f, 0f), Offset(bracketLength, 0f)),
            Triple(Offset(w - bracketLength, 0f), Offset(w, 0f), Offset(w, bracketLength)),
            Triple(Offset(w, h - bracketLength), Offset(w, h), Offset(w - bracketLength, h)),
            Triple(Offset(bracketLength, h), Offset(0f, h), Offset(0f, h - bracketLength))
        )
        for ((p1, p2, p3) in corners) {
            drawLine(accentColor, p1, p2, bracketStroke, cap = StrokeCap.Round)
            drawLine(accentColor, p2, p3, bracketStroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
@OptIn(kotlin.ExperimentalUnsignedTypes::class)
internal fun CameraPreview(onDataReady: (UByteArray) -> Unit) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
        }

    LaunchedEffect(Unit) {
        if (!hasPermission) { permissionLauncher.launch(Manifest.permission.CAMERA) }
    }

    if (hasPermission) {
        CameraPreviewView(context, onDataReady = onDataReady)
    } else {
        PermissionRequestScreen(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
    }
}

/**
 * Styled fallback shown when camera permission hasn't been granted yet,
 * matching the rest of the scan page's dark, branded look.
 */
@Composable
private fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Simple camera glyph, drawn so no extra icon dependency is needed
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val primary = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(32.dp)) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(0f, h * 0.2f),
                        size = androidx.compose.ui.geometry.Size(w, h * 0.65f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = h * 0.22f,
                        center = Offset(w / 2f, h * 0.55f)
                    )
                }
            }

            Text(
                text = "Camera access needed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "POSqueue uses your camera to scan the store's QR code and check you in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 28.dp)
                    .fillMaxWidth(0.8f)
            )

            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Allow camera access",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
@OptIn(kotlin.ExperimentalUnsignedTypes::class)
internal fun CameraPreviewView(
    context: Context = LocalContext.current,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    modifier: Modifier = Modifier,
    onDataReady: (UByteArray) -> Unit,
) {
    var pagedData by remember { mutableStateOf(PagedData()) }
    var pagedDataAssembled by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val barcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(barcodeScannerOptions)
    }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            PreviewView(viewContext).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener(
                {
                    // Compose Camera Preview
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    // Scanning QR Codes
                    // Reference:
                    //   https://developers.google.com/ml-kit/vision/barcode-scanning/android
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                if (!pagedDataAssembled) {
                                    val qrcode =
                                        barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                    val value = qrcode?.rawBytes?.toUByteArray()

                                    if (!value.isNullOrEmpty()) {
                                        val data = pagedData.addDataPageAndConstruct(value)

                                        if (data != null) {
                                            pagedDataAssembled = true
                                            onDataReady(data)
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { }
                            .addOnCompleteListener { imageProxy.close() }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    )
}