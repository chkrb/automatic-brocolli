package io.github.chkrb.pqcompanion.ui.pages.shop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.chkrb.pqcompanion.data.RetailerStatusProduct
import io.github.chkrb.pqcompanion.data.OrderRequest
import io.github.chkrb.pqcompanion.ui.NavDestination
import io.github.chkrb.pqcompanion.ui.viewmodels.ShopViewModel
import io.github.chkrb.pqcompanion.ui.icons.iconAddShoppingCart
import io.github.chkrb.pqcompanion.ui.icons.iconArrowBack
import io.github.chkrb.pqcompanion.ui.icons.iconRemoveShoppingCart
import io.github.chkrb.pqcompanion.ui.icons.iconShoppingCartCheckout

@Composable
fun ShopExplorePage(navController: NavController, vm: ShopViewModel) {
    if (vm.retailerStatusError) {
        ErrorPage()
        return
    }

    val orderRequest by remember {
        mutableStateOf(OrderRequest.loadFromRetailerStatus(vm.retailerStatus!!))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(navController) },
        bottomBar = { BottomBar(navController, vm, orderRequest) },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(orderRequest.products) { product ->
                    var stockInCart by remember { mutableStateOf(product.orderedStock) }

                    ProductCard(
                        product.retailerStatusProduct,
                        stockInCart,
                        {
                            product.orderedStock++
                            stockInCart++
                        },
                        {
                            product.orderedStock--
                            stockInCart--
                        },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun TopBar(navController: NavController) {
    LargeTopAppBar(
        title = { Text(text = "Store") },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = iconArrowBack, contentDescription = "Back")
            }
        },
    )
}

@Composable
internal fun BottomBar(
    navController: NavController,
    vm: ShopViewModel,
    orderRequest: OrderRequest,
) {
    BottomAppBar {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            FloatingActionButton(
                onClick = {
                    if (orderRequest.products.any { it.orderedStock > 0u }) {
                        vm.processOrderRequest(orderRequest)
                        navController.navigate(NavDestination.SHOP_CHECKOUT.route()) {
                            popUpTo(NavDestination.HOME.route())
                        }
                    }
                },
            ) {
                Icon(iconShoppingCartCheckout, "Checkout")
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
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).height(100.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = product.catalogProduct.name, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onStockDecrement() },
                    enabled = stockInCart > 0u,
                ) {
                    Icon(imageVector = iconRemoveShoppingCart, contentDescription = "Remove")
                }

                // TODO: jumpy when 9 -> 10, 99 -> 100, etc.
                // TODO: customize for non-quanitzed items
                Text("${stockInCart}")

                IconButton(
                    onClick = { onStockIncrement() },
                    enabled = stockInCart < product.availableStock,
                ) {
                    Icon(imageVector = iconAddShoppingCart, contentDescription = "Add")
                }
            }
        }
    }
}

@Composable
internal fun ErrorPage() {
}
