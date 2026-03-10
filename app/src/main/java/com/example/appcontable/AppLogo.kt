package com.example.appcontable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AppLogo(
    modifier: Modifier = Modifier, 
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = Icons.Default.AccountBalanceWallet,
        contentDescription = "Logo de la app",
        modifier = modifier, // CORREGIDO: Ya no se fuerza el tamaño
        tint = tint
    )
}
