package io.github.chkrb.pqcompanion.ui.pages.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.NumberFormat
import java.util.Locale
import io.github.chkrb.pqcompanion.data.RetailerStatusProduct
import io.github.chkrb.pqcompanion.data.OrderRequest
import io.github.chkrb.pqcompanion.ui.NavDestination
import io.github.chkrb.pqcompanion.ui.viewmodels.ShopViewModel
import io.github.chkrb.pqcompanion.ui.icons.iconAddShoppingCart
import io.github.chkrb.pqcompanion.ui.icons.iconArrowBack
import io.github.chkrb.pqcompanion.ui.icons.iconRemoveShoppingCart
import io.github.chkrb.pqcompanion.ui.icons.iconShoppingCartCheckout

// ---- Palette pulled from the home screen (off-white base + terracotta accent) ----
private val BackgroundOffWhite = Color(0xFFFAF6EC)
private val CardBackground = Color(0xFFFFFFFF)
private val AccentTerracotta = Color(0xFFC1694F)
private val AccentTerracottaDark = Color(0xFFA6543D)
private val TextPrimary = Color(0xFF2B2420)
private val TextSecondary = Color(0xFF8C8377)
private val StatusGreenBg = Color(0xFFE3F0E5)
private val StatusGreenText = Color(0xFF3F7D4F)
private val StatusRedBg = Color(0xFFF5E4E1)
private val StatusRedText = Color(0xFFB6442F)
private val StatusAmberBg = Color(0xFFFBEEDD)
private val StatusAmberText = Color(0xFFB07A2C)

@Composable
fun ShopExplorePage(navController: NavController, vm: ShopViewModel) {
    if (vm.retailerStatusError) {
        ErrorPage()
        return
    }

    val orderRequest by remember {
        mutableStateOf(OrderRequest.loadFromRetailerStatus(vm.retailerStatus!!))
    }

    // Tracks the live total so the bottom bar recomposes as items are added/removed.
    var totalPayable by remember { mutableStateOf(computeTotal(orderRequest)) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(searchQuery, orderRequest.products) {
        if (searchQuery.isBlank()) {
            orderRequest.products
        } else {
            orderRequest.products.filter {
                it.retailerStatusProduct.catalogProduct.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(BackgroundOffWhite),
        containerColor = BackgroundOffWhite,
        topBar = { TopBar(navController) },
        bottomBar = {
            BottomBar(
                navController = navController,
                vm = vm,
                orderRequest = orderRequest,
                totalPayable = totalPayable,
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.fillMaxSize().background(BackgroundOffWhite).padding(scaffoldPadding),
        ) {
            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredProducts) { product ->
                    var stockInCart by remember { mutableStateOf(product.orderedStock) }

                    ProductCard(
                        product.retailerStatusProduct,
                        stockInCart,
                        {
                            product.orderedStock++
                            stockInCart++
                            totalPayable = computeTotal(orderRequest)
                        },
                        {
                            product.orderedStock--
                            stockInCart--
                            totalPayable = computeTotal(orderRequest)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = CardBackground,
        shape = RoundedCornerShape(50),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(
                modifier = Modifier.padding(end = 10.dp).size(18.dp),
            ) {
                val stroke = 1.6.dp.toPx()
                val glassRadius = size.minDimension * 0.32f
                val glassCenter = Offset(size.width * 0.42f, size.height * 0.42f)

                drawCircle(
                    color = AccentTerracottaDark,
                    radius = glassRadius,
                    center = glassCenter,
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = AccentTerracottaDark,
                    start = Offset(
                        glassCenter.x + glassRadius * 0.75f,
                        glassCenter.y + glassRadius * 0.75f,
                    ),
                    end = Offset(size.width * 0.95f, size.height * 0.95f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search products",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun computeTotal(orderRequest: OrderRequest): Int =
    orderRequest.products.sumOf {
        it.orderedStock.toInt() * it.retailerStatusProduct.sellingPrice.toInt()
    }

// en-IN grouping gives the lakh/crore comma placement (₹1,23,456.00) instead of the
// western ₹123,456.00 that plain string interpolation was producing.
private val inrFormatter: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}

private fun formatInr(rupees: Int): String = inrFormatter.format(rupees)

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun TopBar(navController: NavController) {
    TopAppBar(
        title = {
            Text(
                text = "Store",
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
}

@Composable
internal fun BottomBar(
    navController: NavController,
    vm: ShopViewModel,
    orderRequest: OrderRequest,
    totalPayable: Int,
) {
    val hasItems = orderRequest.products.any { it.orderedStock > 0u }

    Surface(
        color = CardBackground,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Total payable",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = formatInr(totalPayable),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Button(
                onClick = {
                    if (hasItems) {
                        vm.processOrderRequest(orderRequest)
                        navController.navigate(NavDestination.SHOP_CHECKOUT.route())
                    }
                },
                enabled = hasItems,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTerracotta,
                    contentColor = Color.White,
                    disabledContainerColor = AccentTerracotta.copy(alpha = 0.35f),
                    disabledContentColor = Color.White,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp,
                    vertical = 12.dp,
                ),
            ) {
                Icon(iconShoppingCartCheckout, contentDescription = "Checkout")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Checkout", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun ProductCard(
    product: RetailerStatusProduct,
    stockInCart: UInt,
    onStockIncrement: () -> Unit,
    onStockDecrement: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // TODO: restore once the correct brand field name on CatalogProduct is confirmed.
                // Text(
                //     text = product.catalogProduct.brandName,
                //     color = TextSecondary,
                //     style = MaterialTheme.typography.labelSmall,
                // )

                Text(
                    text = product.catalogProduct.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatInr(product.sellingPrice.toInt()),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    StockBadge(availableStock = product.availableStock)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onStockDecrement() },
                    enabled = stockInCart > 0u,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = AccentTerracottaDark,
                        disabledContentColor = TextSecondary.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(imageVector = iconRemoveShoppingCart, contentDescription = "Remove")
                }

                // TODO: jumpy when 9 -> 10, 99 -> 100, etc.
                // TODO: customize for non-quanitzed items
                Text(
                    text = "$stockInCart",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )

                IconButton(
                    onClick = { onStockIncrement() },
                    enabled = stockInCart < product.availableStock,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = AccentTerracotta,
                        disabledContentColor = TextSecondary.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(imageVector = iconAddShoppingCart, contentDescription = "Add")
                }
            }
        }
    }
}

/**
 * Shows "Out of Stock" when nothing is left, "x left" while stock is low (1-5 units),
 * and a plain "In Stock" badge once availability is comfortably above that.
 */
@Composable
internal fun StockBadge(availableStock: UInt) {
    val (label, bg, fg) = when {
        availableStock == 0u -> Triple("Out of Stock", StatusRedBg, StatusRedText)
        availableStock in 1u..5u -> Triple("$availableStock left", StatusAmberBg, StatusAmberText)
        else -> Triple("In Stock", StatusGreenBg, StatusGreenText)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun ErrorPage() {
    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundOffWhite),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Something went wrong loading the store.",
            color = TextSecondary,
        )
    }
}
