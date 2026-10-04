package com.neoqubix.devajit.h2.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen

@Composable
fun StatusChip(active: Boolean, activeLabel: String = "Active", inactiveLabel: String = "Inactive") {
    val color = if (active) RevenueGreen else ExpenseRed
    AssistChip(
        onClick = {},
        label = { Text(if (active) activeLabel else inactiveLabel) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color, containerColor = color.copy(alpha = 0.08f)),
        border = null
    )
}

@Composable
private fun IconLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CartCard(
    cart: Cart,
    managerName: String?,
    summary: FinancialSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cart.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (cart.location.isNotBlank()) IconLine(Icons.Outlined.Place, cart.location)
                    IconLine(Icons.Outlined.Person, managerName ?: "No manager assigned", if (managerName == null) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(cart.isActive)
            }
            if (summary != null) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SummaryLine(summary)
            }
        }
    }
}

@Composable
fun ManagerCard(
    manager: UserProfile,
    cartName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(manager.name.ifBlank { "Unnamed" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(manager.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cart: ${cartName ?: "Not assigned"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (cartName == null) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
            }
            StatusChip(cartName != null, activeLabel = "Assigned", inactiveLabel = "Unassigned")
        }
    }
}
