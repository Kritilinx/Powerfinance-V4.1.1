package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.NormalBalance
import com.example.data.repository.AccountBalanceItem
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletEquity
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<Account>,
    accountBalances: List<AccountBalanceItem>,
    searchQuery: String,
    selectedCategory: AccountCategory?,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (AccountCategory?) -> Unit,
    onAddAccount: (String, String, AccountCategory, String, NormalBalance, String) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onDeleteAccount: (Account) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    val balanceMap = accountBalances.associateBy { it.account.id }

    val filteredAccounts = accounts.filter { acc ->
        val matchesCategory = selectedCategory == null || acc.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                acc.code.contains(searchQuery.trim(), ignoreCase = true) ||
                acc.name.contains(searchQuery.trim(), ignoreCase = true) ||
                acc.subcategory.contains(searchQuery.trim(), ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_account")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Akun Baru")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_search_input"),
                placeholder = { Text("Cari kode atau nama akun...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus Pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text("Semua (${accounts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                AccountCategory.values().forEach { cat ->
                    val count = accounts.count { it.category == cat }
                    item {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text("${cat.displayName} ($count)") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Account List
            if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada akun yang sesuai pencarian.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAccounts) { acc ->
                        val bal = balanceMap[acc.id]?.netBalance ?: 0.0
                        AccountListItemCard(
                            account = acc,
                            balance = bal,
                            onEdit = { accountToEdit = acc },
                            onToggleActive = { onToggleActive(acc.id, !acc.isActive) },
                            onDelete = { accountToDelete = acc }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name, category, subcategory, normalBalance, desc ->
                onAddAccount(code, name, category, subcategory, normalBalance, desc)
                showAddDialog = false
            }
        )
    }

    // Edit Account Dialog
    accountToEdit?.let { acc ->
        EditAccountDialog(
            account = acc,
            onDismiss = { accountToEdit = null },
            onConfirm = { updated ->
                onUpdateAccount(updated)
                accountToEdit = null
            }
        )
    }

    // Delete Account Confirmation Dialog
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = RoseLoss)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Akun")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus akun '${acc.code} - ${acc.name}'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Perhatian: Sistem akan memeriksa apakah akun ini memiliki riwayat jurnal. Akun yang memiliki histori akuntansi TIDAK DAPAT dihapus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount(acc)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseLoss),
                    modifier = Modifier.testTag("btn_confirm_delete_account")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun AccountListItemCard(
    account: Account,
    balance: Double,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.code}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = account.code,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (account.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (account.isSystem) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Akun Sistem",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Balance Display
                Text(
                    text = FormatUtils.formatRupiah(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (account.code in listOf("3002", "3003")) VioletEquity else MaterialTheme.colorScheme.primary
                        )
                        if (account.subcategory.isNotBlank()) {
                            Text(
                                text = " • ${account.subcategory}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Saldo Normal: ${account.normalBalance.displayName} • Status: ${if (account.isActive) "Aktif" else "Nonaktif"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Toggle Active Icon
                    IconButton(
                        onClick = onToggleActive,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (account.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Ubah Status Aktif",
                            tint = if (account.isActive) EmeraldGain else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("btn_edit_${account.code}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Akun",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button (disabled for system accounts)
                    if (!account.isSystem) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).testTag("btn_delete_${account.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Akun",
                                tint = RoseLoss,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, AccountCategory, String, NormalBalance, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AccountCategory.ASSET) }
    var subcategory by remember { mutableStateOf("") }
    var normalBalance by remember { mutableStateOf(NormalBalance.DEBIT) }
    var description by remember { mutableStateOf("") }

    var catDropdownExpanded by remember { mutableStateOf(false) }
    var normDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Akun Baru (COA)", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kode Akun (Contoh: 1003)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_add_account_code"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Akun") },
                        placeholder = { Text("Contoh: Bank Mandiri") },
                        modifier = Modifier.fillMaxWidth().testTag("input_add_account_name"),
                        singleLine = true
                    )
                }

                // Category Selector
                item {
                    ExposedDropdownMenuBox(
                        expanded = catDropdownExpanded,
                        onExpandedChange = { catDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Akun") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = catDropdownExpanded,
                            onDismissRequest = { catDropdownExpanded = false }
                        ) {
                            AccountCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        category = cat
                                        normalBalance = when (cat) {
                                            AccountCategory.ASSET, AccountCategory.COGS, AccountCategory.EXPENSE -> NormalBalance.DEBIT
                                            AccountCategory.LIABILITY, AccountCategory.EQUITY, AccountCategory.REVENUE -> NormalBalance.CREDIT
                                        }
                                        catDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = subcategory,
                        onValueChange = { subcategory = it },
                        label = { Text("Subkategori (Opsional)") },
                        placeholder = { Text("Contoh: Bank / Kas / Beban Rutin") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Normal Balance Selector
                item {
                    ExposedDropdownMenuBox(
                        expanded = normDropdownExpanded,
                        onExpandedChange = { normDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = normalBalance.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Saldo Normal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = normDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = normDropdownExpanded,
                            onDismissRequest = { normDropdownExpanded = false }
                        ) {
                            NormalBalance.values().forEach { norm ->
                                DropdownMenuItem(
                                    text = { Text(norm.displayName) },
                                    onClick = {
                                        normalBalance = norm
                                        normDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onConfirm(code.trim(), name.trim(), category, subcategory.trim(), normalBalance, description.trim())
                    }
                },
                modifier = Modifier.testTag("btn_save_new_account")
            ) {
                Text("Simpan Akun")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var subcategory by remember { mutableStateOf(account.subcategory) }
    var description by remember { mutableStateOf(account.description) }
    var isActive by remember { mutableStateOf(account.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Akun ${account.code}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = account.code,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kode Akun (Terkunci)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Akun") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_account_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subkategori") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Akun Aktif", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGain)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan Tambahan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            account.copy(
                                name = name.trim(),
                                subcategory = subcategory.trim(),
                                description = description.trim(),
                                isActive = isActive
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("btn_save_edit_account")
            ) {
                Text("Perbarui Akun")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
