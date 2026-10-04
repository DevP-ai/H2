package com.neoqubix.devajit.h2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen
import com.neoqubix.devajit.h2.utils.formatDate
import com.neoqubix.devajit.h2.utils.formatRupees
import com.neoqubix.devajit.h2.utils.formatTime

// Revenue shows as green "+", expenses as red "-". onClick is only given to admins (to edit the record).
@Composable
fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    cartName: String? = null,
    onClick: (() -> Unit)? = null
) {
    val color = if (transaction.isRevenue) RevenueGreen else ExpenseRed
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(modifier.fillMaxWidth().then(clickable).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (transaction.isRevenue) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                contentDescription = if (transaction.isRevenue) "Revenue" else "Expense",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                transaction.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val details = buildList {
                add(if (transaction.isRevenue) "Revenue" else "Expense")
                if (transaction.isEdited) add("Edited")
                if (!transaction.isRevenue && transaction.description.isNotBlank()) add(transaction.description)
                if (cartName != null) add(cartName)
            }.joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val time = transaction.createdAt ?: transaction.date
            Text(
                "${formatDate(transaction.date)} · ${formatTime(time)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (transaction.isRevenue) "+ " else "- ") + formatRupees(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (transaction.pendingSync) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(2.dp))
                    Text("Waiting to sync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
