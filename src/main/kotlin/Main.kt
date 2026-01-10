package org.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import androidx.compose.ui.res.loadImageBitmap
import java.io.InputStream
import org.example.ui.*

@Composable
fun App() {
    var screen by remember { mutableStateOf("home") }
    val today = java.time.LocalDate.now().toString()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(org.example.ui.theme.AppTheme.backgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Spacer(modifier = Modifier.height(32.dp))

            when (screen) {
                "home" -> Column(modifier = Modifier.fillMaxSize()) {

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Lakshmi Multiplex",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = org.example.ui.theme.AppTheme.primaryDarkColor,
                            letterSpacing = 3.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    HomeScreen(
                        onAdminClick = { screen = "adminLogin" },
                        onUserClick = { screen = "userLogin" }
                    )
                }

                "adminLogin" -> AdminLoginScreen(
                    onLoginSuccess = { screen = "productList" },
                    onBack = { screen = "home" }
                )

                "productList" -> ProductListScreen(
                    onLogout = { screen = "home" }
                )

                "userLogin" -> UserLoginScreen(
                    onLoginSuccess = { screen = "userBilling" },
                    onBack = { screen = "home" }
                )

                "userBilling" -> UserBillingScreen(
                    onBack = { screen = "home" }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    "Date: $today",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = org.example.ui.theme.AppTheme.primaryDarkColor
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onAdminClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Tile("User", onUserClick)
            Tile("Admin", onAdminClick)
        }
    }
}

@Composable
fun Tile(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF3B82F6), RoundedCornerShape(30.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun main() = application {
    val icon = run {
        val resource: InputStream? = object {}.javaClass.classLoader.getResourceAsStream("Logo.jpg")
        if (resource != null) {
            BitmapPainter(loadImageBitmap(resource))
        } else {
            null
        }
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "LT POS",
        icon = icon,
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        App()
    }
}
