package com.talent.monthlybudgetkeeper.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory

object CategoryVisuals {
    fun icon(category: TransactionCategory): ImageVector {
        return when (category) {
            TransactionCategory.FOOD -> Icons.Outlined.LocalDining
            TransactionCategory.TRANSPORT -> Icons.Outlined.DirectionsCar
            TransactionCategory.BILLS -> Icons.AutoMirrored.Outlined.ReceiptLong
            TransactionCategory.SHOPPING -> Icons.Outlined.ShoppingBag
            TransactionCategory.HEALTH -> Icons.Outlined.Favorite
            TransactionCategory.EDUCATION -> Icons.AutoMirrored.Outlined.MenuBook
            TransactionCategory.ENTERTAINMENT -> Icons.Outlined.LocalMovies
            TransactionCategory.FAMILY -> Icons.Outlined.Groups
            TransactionCategory.RENT -> Icons.Outlined.HomeWork
            TransactionCategory.OTHER_EXPENSE -> Icons.Outlined.MoreHoriz
            TransactionCategory.SALARY -> Icons.Outlined.AccountBalance
            TransactionCategory.FREELANCE -> Icons.Outlined.LaptopMac
            TransactionCategory.BUSINESS -> Icons.Outlined.BusinessCenter
            TransactionCategory.GIFT -> Icons.Outlined.CardGiftcard
            TransactionCategory.OTHER_INCOME -> Icons.Outlined.AttachMoney
        }
    }

    fun color(category: TransactionCategory): Color {
        return when (category) {
            TransactionCategory.FOOD -> Color(0xFFD06E3B)
            TransactionCategory.TRANSPORT -> Color(0xFF406BBE)
            TransactionCategory.BILLS -> Color(0xFF7057A3)
            TransactionCategory.SHOPPING -> Color(0xFFB68B2D)
            TransactionCategory.HEALTH -> Color(0xFFBC4B58)
            TransactionCategory.EDUCATION -> Color(0xFF2A7B8C)
            TransactionCategory.ENTERTAINMENT -> Color(0xFF7B5EA7)
            TransactionCategory.FAMILY -> Color(0xFF3A8969)
            TransactionCategory.RENT -> Color(0xFF55638F)
            TransactionCategory.OTHER_EXPENSE -> Color(0xFF788491)
            TransactionCategory.SALARY -> Color(0xFF1C8A62)
            TransactionCategory.FREELANCE -> Color(0xFF218278)
            TransactionCategory.BUSINESS -> Color(0xFF285E8A)
            TransactionCategory.GIFT -> Color(0xFFB65D66)
            TransactionCategory.OTHER_INCOME -> Color(0xFF40906F)
        }
    }
}
