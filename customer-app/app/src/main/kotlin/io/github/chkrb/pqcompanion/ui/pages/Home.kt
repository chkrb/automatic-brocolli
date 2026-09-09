package io.github.chkrb.pqcompanion.ui.pages

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.chkrb.pqcompanion.ui.NavDestination
import kotlin.math.cos
import kotlin.math.sin

private val OffWhite = Color(0xFFFAF7F2)
private val TextPrimary = Color(0xFF2C2A26)
private val TextSecondary = Color(0xFF8C8579)
private val Accent = Color(0xFFC1694F)
private val OnAccent = Color(0xFFFFFBF5)

// Pattern background palette: a background tone plus a few close-in-value
// beige/tan tones so the tiled illustrations stay subtle, like a watermark.
private val PatternBg = OffWhite
private val PatternTones = listOf(
    Color(0xFFEAE2CE),
    Color(0xFFE3D9C0),
    Color(0xFFDED2B0),
    Color(0xFFE6DCC5)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed, diagonally drifting pattern of grocery/product icons.
        GroceryPatternBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "POSqueue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ready to scan",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Scan the QR displayed at the shop to start shopping",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.78f)
                )

                Spacer(modifier = Modifier.height(52.dp))

                ScanButton()

                Spacer(modifier = Modifier.height(40.dp))

                ScanQrCodeButton(
                    onClick = { navController.navigate(NavDestination.SHOP.route()) }
                )
            }
        }
    }
}

/**
 * A soft, full-screen wallpaper of small grocery/product glyphs (bottles,
 * fruit, spirals, packages, ...) tiled in a grid and continuously drifting
 * diagonally, similar to a subtly animated watermark pattern.
 *
 * The grid is drawn one cell larger than the viewport on every side and the
 * animation offset is restarted every time it reaches exactly one cell size,
 * so the loop is seamless — there's no visible jump or edge.
 */
@Composable
private fun GroceryPatternBackground(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cellPx = with(density) { 96.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "patternDrift")
    val offsetPx by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = cellPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "patternOffset"
    )

    Canvas(modifier = modifier) {
        drawRect(color = PatternBg, size = size)

        val cols = (size.width / cellPx).toInt() + 3
        val rows = (size.height / cellPx).toInt() + 3

        for (r in -1 until rows) {
            for (c in -1 until cols) {
                // Move every cell the same amount, down and to the right,
                // which reads as the whole pattern sliding diagonally.
                val cx = c * cellPx + cellPx / 2f + offsetPx
                val cy = r * cellPx + cellPx / 2f + offsetPx

                val seed = (r + 97) * 7 + (c + 131) * 13
                val type = Math.floorMod(seed, 9)
                val tone = PatternTones[Math.floorMod(seed / 3, PatternTones.size)]
                val rotation = Math.floorMod(seed * 11, 360).toFloat()

                drawPatternIcon(
                    type = type,
                    center = Offset(cx, cy),
                    s = cellPx * 0.52f,
                    color = tone,
                    rotationDeg = rotation
                )
            }
        }
    }
}

/** Dispatches to one of a handful of simple grocery/product glyphs. */
private fun DrawScope.drawPatternIcon(
    type: Int,
    center: Offset,
    s: Float,
    color: Color,
    rotationDeg: Float
) {
    rotate(degrees = rotationDeg, pivot = center) {
        when (type) {
            0 -> drawCircle(color = color, radius = s * 0.42f, center = center)
            1 -> drawPatternBottle(center, s, color)
            2 -> drawPatternSpiral(center, s, color)
            3 -> drawPatternCrescent(center, s, color)
            4 -> drawPatternPaddle(center, s, color)
            5 -> drawPatternSlice(center, s, color)
            6 -> drawPatternHex(center, s, color)
            7 -> drawPatternTeardrop(center, s, color)
            else -> drawPatternDots(center, s, color)
        }
    }
}

private fun DrawScope.drawPatternBottle(center: Offset, s: Float, color: Color) {
    val bodyW = s * 0.5f
    val bodyH = s * 0.85f
    val bodyTop = Offset(center.x - bodyW / 2f, center.y - bodyH / 2f + s * 0.1f)
    drawRoundRect(
        color = color,
        topLeft = bodyTop,
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(bodyW * 0.25f)
    )

    val capW = bodyW * 0.5f
    val capH = s * 0.22f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - capW / 2f, bodyTop.y - capH + s * 0.02f),
        size = Size(capW, capH),
        cornerRadius = CornerRadius(capW * 0.3f)
    )

    // Label stripe cut out of the body.
    drawRect(
        color = PatternBg,
        topLeft = Offset(bodyTop.x, center.y - s * 0.03f),
        size = Size(bodyW, s * 0.09f)
    )
}

private fun DrawScope.drawPatternSpiral(center: Offset, s: Float, color: Color) {
    val strokeW = s * 0.11f
    for (i in 0..2) {
        val r = s * 0.4f - i * strokeW * 1.15f
        if (r <= 0f) continue
        drawArc(
            color = color,
            startAngle = i * 90f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawPatternCrescent(center: Offset, s: Float, color: Color) {
    val r = s * 0.42f
    val base = Path().apply {
        addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    }
    val bite = Path().apply {
        val off = r * 0.65f
        addOval(
            Rect(
                center.x - r + off,
                center.y - r - off * 0.15f,
                center.x + r + off,
                center.y + r - off * 0.15f
            )
        )
    }
    val crescent = Path().apply { op(base, bite, PathOperation.Difference) }
    drawPath(crescent, color)
}

private fun DrawScope.drawPatternPaddle(center: Offset, s: Float, color: Color) {
    val handleLen = s * 0.5f
    val angleRad = Math.toRadians(35.0)
    val dx = (handleLen * cos(angleRad)).toFloat()
    val dy = (handleLen * sin(angleRad)).toFloat()
    val headCenter = Offset(center.x - dx * 0.35f, center.y - dy * 0.35f)
    val tailEnd = Offset(center.x + dx * 0.6f, center.y + dy * 0.6f)

    drawLine(
        color = color,
        start = headCenter,
        end = tailEnd,
        strokeWidth = s * 0.13f,
        cap = StrokeCap.Round
    )
    drawCircle(color = color, radius = s * 0.3f, center = headCenter)
}

private fun DrawScope.drawPatternSlice(center: Offset, s: Float, color: Color) {
    val w = s * 0.85f
    val h = s * 0.6f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(s * 0.16f)
    )
    // A couple of thin cut-outs, like a bacon rasher or cheese slice.
    drawLine(
        color = PatternBg,
        start = Offset(center.x - w / 2f, center.y - h * 0.12f),
        end = Offset(center.x + w / 2f, center.y - h * 0.28f),
        strokeWidth = s * 0.07f
    )
    drawLine(
        color = PatternBg,
        start = Offset(center.x - w / 2f, center.y + h * 0.3f),
        end = Offset(center.x + w / 2f, center.y + h * 0.14f),
        strokeWidth = s * 0.05f
    )
}

private fun DrawScope.drawPatternHex(center: Offset, s: Float, color: Color) {
    val r = s * 0.42f
    val sides = 7
    val path = Path()
    for (i in 0 until sides) {
        val angle = Math.toRadians(i * (360.0 / sides))
        val px = center.x + r * cos(angle).toFloat()
        val py = center.y + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)

    drawCircle(color = PatternBg, radius = s * 0.045f, center = Offset(center.x - s * 0.1f, center.y - s * 0.05f))
    drawCircle(color = PatternBg, radius = s * 0.035f, center = Offset(center.x + s * 0.09f, center.y + s * 0.08f))
}

private fun DrawScope.drawPatternTeardrop(center: Offset, s: Float, color: Color) {
    val r = s * 0.32f
    val path = Path().apply {
        moveTo(center.x, center.y - s * 0.5f)
        quadraticTo(center.x + r * 1.3f, center.y - s * 0.05f, center.x, center.y + s * 0.42f)
        quadraticTo(center.x - r * 1.3f, center.y - s * 0.05f, center.x, center.y - s * 0.5f)
        close()
    }
    drawPath(path, color)

    for (i in -1..1) {
        drawLine(
            color = PatternBg,
            start = Offset(center.x - s * 0.05f, center.y - s * 0.2f + i * s * 0.12f),
            end = Offset(center.x + s * 0.16f, center.y - s * 0.12f + i * s * 0.12f),
            strokeWidth = s * 0.025f
        )
    }
}

private fun DrawScope.drawPatternDots(center: Offset, s: Float, color: Color) {
    val r = s * 0.16f
    drawCircle(color = color, radius = r, center = Offset(center.x, center.y - r * 0.8f))
    drawCircle(color = color, radius = r, center = Offset(center.x - r * 1.6f, center.y + r * 0.6f))
    drawCircle(color = color, radius = r, center = Offset(center.x + r * 1.6f, center.y + r * 0.6f))
}

/**
 * The real scan action: a pill-shaped outlined button with a camera
 * glyph and "Scan QR Code" label. This is what actually triggers
 * navigation into the scan flow.
 */
@Composable
private fun ScanQrCodeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "scanQrButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth(0.82f)
            .shadow(
                elevation = if (pressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = TextPrimary.copy(alpha = 0.25f),
                spotColor = TextPrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(OffWhite)
            .border(width = 1.dp, color = TextSecondary.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CameraGlyph(
                color = Accent,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "Scan QR Code",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun CameraGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Camera body
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.7f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )
        // Viewfinder bump
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.3f, 0f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // Lens
        drawCircle(
            color = OffWhite,
            radius = h * 0.22f,
            center = Offset(w / 2f, h * 0.58f)
        )
        drawCircle(
            color = color,
            radius = h * 0.13f,
            center = Offset(w / 2f, h * 0.58f)
        )
    }
}

/**
 * A large circular decoration: a soft pulsing glow ring behind a
 * gradient-filled circle with a glossy highlight and a viewfinder icon
 * drawn on [Canvas] (no extended icon-library dependency needed). Purely
 * visual now — the actual scan action lives in [ScanQrCodeButton] below it.
 */
@Composable
private fun ScanButton(
    size: Int = 168
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanButtonPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val primaryLight = lerp(Accent, Color.White, 0.12f)
    val primaryDark = lerp(Accent, Color.Black, 0.28f)

    val density = LocalDensity.current
    val sizePx = with(density) { size.dp.toPx() }

    Box(contentAlignment = Alignment.Center) {
        // Pulsing glow ring
        Box(
            modifier = Modifier
                .size(size.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Accent.copy(alpha = pulseAlpha))
        )

        // Main circle: a soft top-lit gradient plus a light rim so it reads
        // as a raised, lit sphere rather than a flat tinted disc.
        Box(
            modifier = Modifier
                .size(size.dp)
                .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = Accent, spotColor = Accent)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryLight, Accent, primaryDark),
                        center = Offset(sizePx * 0.32f, sizePx * 0.28f),
                        radius = sizePx * 1.05f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glossy highlight, offset toward the top-left
            Box(
                modifier = Modifier
                    .size((size * 0.55f).dp)
                    .align(Alignment.TopStart)
                    .padding(top = (size * 0.06f).dp, start = (size * 0.08f).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.32f), Color.Transparent)
                        )
                    )
            )

            QrCodeGlyph(
                color = OnAccent,
                modifier = Modifier.size((size * 0.5f).dp)
            )
        }
    }
}

@Composable
private fun QrCodeGlyph(color: Color, modifier: Modifier = Modifier) {
    // A hand-authored 9x9 module grid: rings in three corners (like the
    // finder patterns on a real QR code), a timing dot at the center, and a
    // denser scatter of data modules, so it reads convincingly as "QR code"
    // at a glance without needing an image asset.
    val pattern = remember {
        listOf(
            listOf(1, 1, 1, 0, 1, 0, 1, 1, 1),
            listOf(1, 0, 1, 0, 0, 0, 1, 0, 1),
            listOf(1, 1, 1, 0, 1, 0, 1, 1, 1),
            listOf(0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(1, 0, 1, 1, 0, 1, 0, 1, 1),
            listOf(0, 0, 0, 0, 1, 0, 1, 0, 0),
            listOf(1, 1, 1, 0, 0, 1, 0, 0, 1),
            listOf(1, 0, 1, 0, 1, 0, 1, 1, 0),
            listOf(1, 1, 1, 0, 1, 0, 0, 1, 1)
        )
    }

    Canvas(modifier = modifier) {
        val cols = pattern.first().size
        val moduleSize = size.width / cols
        val moduleCorner = moduleSize * 0.14f

        for (row in pattern.indices) {
            for (col in pattern[row].indices) {
                if (pattern[row][col] == 1) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(col * moduleSize, row * moduleSize),
                        size = Size(moduleSize * 0.82f, moduleSize * 0.82f),
                        cornerRadius = CornerRadius(moduleCorner, moduleCorner)
                    )
                }
            }
        }
    }
}