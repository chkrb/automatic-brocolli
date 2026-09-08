package io.github.chkrb.pqcompanion.ui.pages.shop

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.chkrb.pqcompanion.data.PagedQrData
import io.github.chkrb.pqcompanion.data.RetailerStatus
import io.github.chkrb.pqcompanion.ui.NavDestination
import io.github.chkrb.pqcompanion.ui.viewmodels.ShopViewModel
import java.util.concurrent.Executors

@Composable
fun ShopScanPage(navController: NavController, vm: ShopViewModel) {
    LaunchedEffect(Unit) {
        vm.reset()
    }

    Scaffold { scaffoldPadding ->
        Column(modifier = Modifier.padding(scaffoldPadding)) {
            CameraPreview { data ->
                vm.processRetailerStatusData(data)
                navController.navigate(NavDestination.SHOP_EXPLORE.route()) {
                    popUpTo(NavDestination.HOME.route())
                }
            }
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            ) {
                Text("Allow camera access")
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
    var pagedQrData by remember { mutableStateOf(PagedQrData()) }
    var pagedQrDataAssembled by remember { mutableStateOf(false) }

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
                                if (!pagedQrDataAssembled) {
                                    val qrcode =
                                        barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                    val value = qrcode?.rawBytes?.toUByteArray()

                                    if (!value.isNullOrEmpty()) {
                                        val data = pagedQrData.addDataPageAndConstruct(value)

                                        if (data != null) {
                                            pagedQrDataAssembled = true
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
