package org.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.data.ProductDao
import org.example.data.Product
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.example.data.PurchaseDao
import org.example.data.Purchase
import org.example.util.PdfBillGenerator
import org.example.ui.theme.AppTheme

@Composable
fun UserBillingScreen(onBack: () -> Unit) {
    val productDao = remember { ProductDao() }
    var products by remember { mutableStateOf(productDao.getAll()) }
    val cart = remember { mutableStateMapOf<Product, Int>() }
    var paymentMode by remember { mutableStateOf("Cash") }
    var showBillDialog by remember { mutableStateOf(false) }
    val generateBillFocusRequester = remember { FocusRequester() }

    // Sort products alphabetically by name (case-insensitive)
    val sortedProducts = remember(products) {
        products.sortedBy { it.name.lowercase() }
    }

    // Function to handle bill generation
    fun generateBill() {
        if (cart.isNotEmpty()) {
            showBillDialog = true
        }
    }

    // Auto-focus the Generate Bill button when screen loads and whenever cart changes
    LaunchedEffect(Unit) {
        generateBillFocusRequester.requestFocus()
    }

    // Re-focus when cart changes to ensure button is always focused
    LaunchedEffect(cart.size) {
        if (!showBillDialog) {
            generateBillFocusRequester.requestFocus()
        }
    }

    // Re-focus when cart contents change (products added/removed)
    LaunchedEffect(cart.keys.size) {
        if (!showBillDialog) {
            generateBillFocusRequester.requestFocus()
        }
    }

    // Re-focus when any cart item quantity changes
    LaunchedEffect(cart.values.sum()) {
        if (!showBillDialog) {
            generateBillFocusRequester.requestFocus()
        }
    }

    // Re-focus Generate Bill button when dialog closes
    LaunchedEffect(showBillDialog) {
        if (!showBillDialog) {
            generateBillFocusRequester.requestFocus()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Product list
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppTheme.backgroundColor)
                .padding(24.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown && !showBillDialog) {
                        generateBillFocusRequester.requestFocus()
                        true
                    } else false
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val tileShape = RoundedCornerShape(AppTheme.cornerRadius.dp)
            val tileElevation = AppTheme.cardElevation.dp
            val tileBg = Color.White // White background for available products

            val tileBgOutOfStock = Color.White // White background for out of stock products
            val tileTextColor = AppTheme.textPrimary
            val tileTextOutOfStock = AppTheme.errorColor
            val tilePadding = 12.dp
            val tileSpacing = 16.dp

            Text("Select Products", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = tileTextColor)
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedProducts) { product ->
                    val isOutOfStock = product.stock <= 0
                    val cartQty = cart[product] ?: 0
                    val canAdd = product.stock > cartQty
                    Card(
                        shape = tileShape,
                        elevation = tileElevation,
                        backgroundColor = when {
                            isOutOfStock -> tileBgOutOfStock
                            else -> tileBg
                        },
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isOutOfStock) Color.Red else Color(0xFF006400)
                        ),
                        modifier = Modifier
                            .height(120.dp)
                            .let {
                                if (!canAdd) it else it.clickable {
                                    if (canAdd) {
                                        cart[product] = cartQty + 1
                                    }
                                }
                            },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(tilePadding),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(product.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOutOfStock) tileTextOutOfStock else tileTextColor
                            )
                            Text("Price: ₹${product.price}",
                                fontSize = 14.sp,
                                color = AppTheme.textSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Stock: ${product.stock}",
                                    fontSize = 14.sp,
                                    color = if (isOutOfStock) tileTextOutOfStock else tileTextColor
                                )
                                if (canAdd && !isOutOfStock) {
                                    Text(
                                        "+",
                                        color = AppTheme.accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Cart summary with fixed bottom actions
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppTheme.surfaceColor)
                .padding(24.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown && !showBillDialog) {
                        generateBillFocusRequester.requestFocus()
                        true
                    } else false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Cart", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppTheme.primaryColor)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.weight(1f)
                        .background(AppTheme.backgroundColor, RoundedCornerShape(AppTheme.cornerRadius.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(cart.entries.toList()) { entry ->
                            val product = entry.key
                            val qty = entry.value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(AppTheme.surfaceColor, shape = RoundedCornerShape(AppTheme.cornerRadius.dp))
                                    .padding(horizontal = 12.dp)
                                    .clickable {
                                        if (qty > 1) {
                                            cart[product] = qty - 1
                                        } else {
                                            cart.remove(product)
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppTheme.textPrimary)
                                Text("x$qty", fontSize = 16.sp, color = AppTheme.primaryColor)
                                Text("₹${product.price * qty}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.accentColor)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Divider()
                val total = cart.entries.sumOf { it.key.price * it.value }
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(AppTheme.surfaceColor, RoundedCornerShape(AppTheme.cornerRadius.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text("Total: ₹$total",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.primaryDarkColor
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Payment Mode:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = paymentMode == "Cash",
                            onClick = { paymentMode = "Cash" },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.accentColor)
                        )
                        Text("Cash", color = AppTheme.textSecondary)
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = paymentMode == "GPay",
                            onClick = { paymentMode = "GPay" },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.accentColor)
                        )
                        Text("GPay", color = AppTheme.textSecondary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { generateBill() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable(true)
                            .focusRequester(generateBillFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                    generateBill()
                                    true
                                } else false
                            },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppTheme.accentColor,
                            contentColor = AppTheme.textOnPrimary
                        ),
                        shape = RoundedCornerShape(AppTheme.cornerRadius.dp)
                    ) {
                        Text("Generate Bill", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppTheme.primaryColor
                        ),
                        shape = RoundedCornerShape(AppTheme.cornerRadius.dp)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
    if (showBillDialog) {
        BillDialog(
            cart = cart,
            paymentMode = paymentMode,
            onConfirm = {
                val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val currentBillTotal = cart.entries.sumOf { it.key.price * it.value }

                cart.forEach { (product, qty) ->
                    val newStock = product.stock - qty
                    productDao.update(product.copy(stock = newStock))
                    PurchaseDao.add(Purchase(
                        productId = product.id,
                        quantity = qty,
                        paymentMode = paymentMode,
                        dateTime = now
                    ))
                }
                PdfBillGenerator.generateBill(cart, paymentMode, null)

                // Print grand total summary
                PdfBillGenerator.printGrandTotalSummary(currentBillTotal, paymentMode)

                cart.clear()
                products = productDao.getAll() // Refresh product list to update stock
                showBillDialog = false
            },
            onDismiss = { showBillDialog = false },
            generateBillFocusRequester = generateBillFocusRequester
        )
    }
}

@Composable
fun BillDialog(
    cart: Map<Product, Int>,
    paymentMode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    generateBillFocusRequester: FocusRequester
) {
    val confirmButtonFocusRequester = remember { FocusRequester() }

    // Auto-focus the Confirm & Print button when dialog opens
    LaunchedEffect(Unit) {
        confirmButtonFocusRequester.requestFocus()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = AppTheme.surfaceColor,
        shape = RoundedCornerShape(AppTheme.cornerRadius.dp),
        title = {
            Text(
                "Confirm Bill",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppTheme.primaryColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cart.forEach { (product, qty) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${product.name} x$qty", color = AppTheme.textPrimary)
                        Text("₹${product.price * qty}", fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTheme.textPrimary)
                    Text(
                        "₹${cart.entries.sumOf { it.key.price * it.value }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppTheme.accentColor
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Payment Mode: $paymentMode", color = AppTheme.textSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .focusable(true)
                    .focusRequester(confirmButtonFocusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                            onConfirm()
                            true
                        } else false
                    },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.accentColor,
                    contentColor = AppTheme.textOnPrimary
                ),
                shape = RoundedCornerShape(AppTheme.cornerRadius.dp)
            ) {
                Text("Confirm & Print", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppTheme.primaryColor
                ),
                shape = RoundedCornerShape(AppTheme.cornerRadius.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}