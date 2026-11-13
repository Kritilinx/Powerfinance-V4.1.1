package com.example.receipt.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receipt.data.model.ReceiptItemSnapshot
import com.example.receipt.data.model.ReceiptRecord
import com.example.receipt.data.model.ReceiptWithItems
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ThermalReceiptCard(
    receiptWithItems: ReceiptWithItems,
    modifier: Modifier = Modifier
) {
    val receipt = receiptWithItems.receipt
    val items = receiptWithItems.items
    val nf = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .defaultMinSize(minHeight = 780.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .testTag("thermal_receipt_card"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 780.dp)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Store
                Text(
                    text = receipt.storeName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = receipt.storeAddress,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Telp: ${receipt.storePhone}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )

                ReceiptDashedDivider(char = '=')

                // Meta Info
                ReceiptKeyValueMonospace("No. Struk", receipt.receiptNumber, boldValue = true)
                ReceiptKeyValueMonospace("Waktu", receipt.formattedDateTime)
                ReceiptKeyValueMonospace("Pembayaran", receipt.paymentMethod, boldValue = true)

                ReceiptDashedDivider(char = '=')

                // Table Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ITEM & HARGA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("SUBTOTAL", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Items List
                items.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            text = item.productName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "  ${item.quantity} x ${nf.format(item.unitPrice).replace("Rp", "Rp ")}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF374151)
                            )
                            Text(
                                text = nf.format(item.subtotal).replace("Rp", "Rp "),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                    }
                }

                ReceiptDashedDivider(char = '-')

                // Quantity & Totals
                ReceiptKeyValueMonospace("Jumlah Jenis", "${receipt.totalItemTypes} produk")
                ReceiptKeyValueMonospace("Total Item", "${receipt.totalQuantity} pcs")

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = nf.format(receipt.totalAmount).replace("Rp", "Rp "),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReceiptDashedDivider(char = '=')

                // Barcode Drawing
                ReceiptBarcode(receiptNumber = receipt.receiptNumber)

                Spacer(modifier = Modifier.height(8.dp))

                // Footer Notes
                Text(
                    text = receipt.footerMessage,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Barang yang sudah dicetak tidak dapat ditukar/dikembalikan.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Waktu cetak: ${receipt.printedAtFormatted}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ReceiptKeyValueMonospace(key: String, value: String, boldValue: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = key, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF4B5563))
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
    }
}

@Composable
fun ReceiptDashedDivider(char: Char = '=') {
    Text(
        text = char.toString().repeat(32),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = Color(0xFF6B7280),
        modifier = Modifier.padding(vertical = 4.dp),
        maxLines = 1
    )
}

/**
 * Draws realistic 1D Barcode bars based on receipt number hash/pattern
 */
@Composable
fun ReceiptBarcode(receiptNumber: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(vertical = 4.dp)) {
        Canvas(modifier = Modifier.size(width = 200.dp, height = 44.dp)) {
            val width = size.width
            val height = size.height
            val cleanStr = receiptNumber.ifBlank { "STRUK-0001" }

            // Deterministic bar widths based on chars
            var x = 8f
            val barCount = 45
            val step = (width - 16f) / barCount

            for (i in 0 until barCount) {
                val charVal = cleanStr.getOrElse(i % cleanStr.length) { 'A' }.code
                val isThick = (charVal + i) % 3 == 0
                val barWidth = if (isThick) step * 0.75f else step * 0.35f

                drawLine(
                    color = Color.Black,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = barWidth
                )
                x += step
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "* $receiptNumber *",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 2.sp
        )
    }
}

/**
 * Action buttons for Sharing Image to WhatsApp and Copying Text
 */
@Composable
fun ReceiptActionButtons(
    receiptWithItems: ReceiptWithItems,
    whatsAppText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWhatsAppChooserDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // WhatsApp Image Share Button
        Button(
            onClick = {
                showWhatsAppChooserDialog = true
            },
            modifier = Modifier.weight(1f).testTag("btn_share_whatsapp"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kirim WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Copy Text Button
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Struk ${receiptWithItems.receipt.receiptNumber}", whatsAppText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Teks struk berhasil disalin!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("btn_copy_receipt")
        ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Salin Teks", fontSize = 12.sp)
        }
    }

    // Dialog: Pilih ingin mengirim menggunakan WhatsApp atau WhatsApp Business
    if (showWhatsAppChooserDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppChooserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pilih Jenis WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kirim gambar struk #${receiptWithItems.receipt.receiptNumber} melalui:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Pilihan 1: WhatsApp Biasa
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showWhatsAppChooserDialog = false
                                shareReceiptImageToPackage(
                                    context = context,
                                    receiptWithItems = receiptWithItems,
                                    targetPackage = "com.whatsapp",
                                    appLabel = "WhatsApp"
                                )
                            }
                            .testTag("btn_select_whatsapp"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF25D366).copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF25D366),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("💬", fontSize = 20.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kirim via WhatsApp Standar / Personal",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Pilihan 2: WhatsApp Business
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showWhatsAppChooserDialog = false
                                shareReceiptImageToPackage(
                                    context = context,
                                    receiptWithItems = receiptWithItems,
                                    targetPackage = "com.whatsapp.w4b",
                                    appLabel = "WhatsApp Business"
                                )
                            }
                            .testTag("btn_select_whatsapp_business"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF075E54).copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF075E54),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("💼", fontSize = 20.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "WhatsApp Business",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kirim via Akun Bisnis Toko",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showWhatsAppChooserDialog = false },
                    modifier = Modifier.testTag("btn_close_wa_dialog")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun shareReceiptImageToPackage(
    context: Context,
    receiptWithItems: ReceiptWithItems,
    targetPackage: String,
    appLabel: String
) {
    try {
        val imageUri = ReceiptImageGenerator.generateAndSaveReceiptImage(context, receiptWithItems)
        val caption = "Struk Pembelian ${receiptWithItems.receipt.receiptNumber} - ${receiptWithItems.receipt.storeName}"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            setPackage(targetPackage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "$appLabel tidak ditemukan di perangkat. Membuka pemilih aplikasi...",
                Toast.LENGTH_SHORT
            ).show()
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Kirim Gambar Struk via $appLabel"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal memproses gambar struk: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Generates high-resolution PNG receipt image for sharing via WhatsApp.
 * Accurately matches the preview card appearance, margins, fonts, dividers,
 * barcode, and proportions (width and height).
 */
object ReceiptImageGenerator {

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split("\\s+".toRegex())
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    fun generateAndSaveReceiptImage(context: Context, receiptWithItems: ReceiptWithItems): Uri {
        val receipt = receiptWithItems.receipt
        val items = receiptWithItems.items
        val nf = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

        // Paper size matching sample reference image: 840px width x 1600px height
        val bitmapWidth = 840
        val cardPadding = 44f
        val leftX = cardPadding
        val rightX = bitmapWidth - cardPadding
        val contentWidth = rightX - leftX // 752f
        val centerX = bitmapWidth / 2f

        // Paints matching Compose styling
        val paintCardBg = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintCardBorder = Paint().apply {
            color = AndroidColor.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 32f // 16sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintSubHeader = Paint().apply {
            color = AndroidColor.parseColor("#4B5563") // DarkGray
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintDivider = Paint().apply {
            color = AndroidColor.parseColor("#9CA3AF") // LightGray / Outline
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintLeftNormal = Paint().apply {
            color = AndroidColor.parseColor("#4B5563")
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintRightNormal = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val paintLeftBold = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintRightBold = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val paintItemName = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 24f // 12sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintItemDetail = Paint().apply {
            color = AndroidColor.parseColor("#374151")
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintTotalTitle = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 28f // 14sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintTotalVal = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 32f // 16sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val paintBar = Paint().apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            isAntiAlias = false
        }

        val paintBarcodeText = Paint().apply {
            color = AndroidColor.parseColor("#4B5563")
            textSize = 20f // 10sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintFooterMsg = Paint().apply {
            color = AndroidColor.parseColor("#111827")
            textSize = 22f // 11sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintFooterNote = Paint().apply {
            color = AndroidColor.parseColor("#6B7280")
            textSize = 20f // 10sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintFooterTime = Paint().apply {
            color = AndroidColor.parseColor("#9CA3AF")
            textSize = 18f // 9sp * 2
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Dividers spanning exactly edge-to-edge
        val charWidth = paintDivider.measureText("=").coerceAtLeast(1f)
        val numChars = (contentWidth / charWidth).toInt().coerceAtLeast(32)
        val equalDivider = "=".repeat(numChars)
        val dashDivider = "-".repeat(numChars)

        // Precompute wrapped lines for accurate height
        val addressLines = wrapText(receipt.storeAddress, paintSubHeader, contentWidth)
        val footerLines = wrapText(receipt.footerMessage, paintFooterMsg, contentWidth)
        val itemLinesList = items.map { wrapText(it.productName, paintItemName, contentWidth) }

        // --- Pass 1: Calculate Exact Dynamic Height matching Preview ---
        var measuredH = cardPadding // Top padding
        measuredH += 34f // Store name
        measuredH += 4f // Spacer
        measuredH += (addressLines.size * 26f).coerceAtLeast(26f) // Address lines
        measuredH += 26f // Phone
        measuredH += 12f // Spacer

        measuredH += 28f // Divider 1
        measuredH += 28f * 3 // Meta (No. Struk, Waktu, Pembayaran)
        measuredH += 8f // Spacer

        measuredH += 28f // Divider 2
        measuredH += 30f // Table Header
        measuredH += 8f // Spacer

        // Items height
        for (i in items.indices) {
            val nameLines = itemLinesList[i]
            measuredH += (nameLines.size * 28f).coerceAtLeast(28f)
            measuredH += 26f // qty x price & subtotal
            measuredH += 12f // spacing between items
        }

        measuredH += 28f // Divider 3
        measuredH += 28f // Jumlah Jenis
        measuredH += 28f // Total Item
        measuredH += 10f // Spacer
        measuredH += 38f // TOTAL

        measuredH += 28f // Divider 4

        val barcodeHeight = 160f
        measuredH += barcodeHeight
        measuredH += 8f // Spacer
        measuredH += 24f // * receiptNumber *
        measuredH += 16f // Spacer

        // Footer lines
        measuredH += (footerLines.size * 28f).coerceAtLeast(28f)
        measuredH += 8f // Spacer
        measuredH += 24f // Fixed note: Barang yang sudah dicetak...
        measuredH += 10f // Spacer
        measuredH += 22f // Waktu cetak: ...
        measuredH += cardPadding // Bottom padding

        val totalHeight = maxOf(1600, kotlin.math.ceil(measuredH).toInt())

        // --- Pass 2: Render to Bitmap with Exact Proportions (840x1600) ---
        val bitmap = Bitmap.createBitmap(bitmapWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        // Pure white paper background matching sample image
        canvas.drawColor(AndroidColor.WHITE)

        var y = cardPadding + 28f

        // 1. Store Header
        canvas.drawText(receipt.storeName.uppercase(), centerX, y, paintHeader)
        y += 30f

        if (addressLines.isEmpty()) {
            canvas.drawText(receipt.storeAddress, centerX, y, paintSubHeader)
            y += 26f
        } else {
            for (line in addressLines) {
                canvas.drawText(line, centerX, y, paintSubHeader)
                y += 26f
            }
        }
        canvas.drawText("Telp: ${receipt.storePhone}", centerX, y, paintSubHeader)
        y += 28f

        // 2. Divider
        canvas.drawText(equalDivider, centerX, y, paintDivider)
        y += 28f

        // 3. Meta Data
        canvas.drawText("No. Struk", leftX, y, paintLeftNormal)
        canvas.drawText(receipt.receiptNumber, rightX, y, paintRightBold)
        y += 28f

        canvas.drawText("Waktu", leftX, y, paintLeftNormal)
        canvas.drawText(receipt.formattedDateTime, rightX, y, paintRightNormal)
        y += 28f

        canvas.drawText("Pembayaran", leftX, y, paintLeftNormal)
        canvas.drawText(receipt.paymentMethod, rightX, y, paintRightBold)
        y += 28f

        // 4. Divider
        canvas.drawText(equalDivider, centerX, y, paintDivider)
        y += 28f

        // 5. Table Header
        canvas.drawText("ITEM & HARGA", leftX, y, paintLeftBold)
        canvas.drawText("SUBTOTAL", rightX, y, paintRightBold)
        y += 34f

        // 6. Items List
        for (i in items.indices) {
            val item = items[i]
            val nameLines = itemLinesList[i]
            if (nameLines.isEmpty()) {
                canvas.drawText(item.productName, leftX, y, paintItemName)
                y += 28f
            } else {
                for (nLine in nameLines) {
                    canvas.drawText(nLine, leftX, y, paintItemName)
                    y += 28f
                }
            }
            val qtyPriceStr = "  ${item.quantity} x ${nf.format(item.unitPrice).replace("Rp", "Rp ")}"
            canvas.drawText(qtyPriceStr, leftX, y, paintItemDetail)
            canvas.drawText(nf.format(item.subtotal).replace("Rp", "Rp "), rightX, y, paintRightBold)
            y += 32f
        }

        // 7. Divider
        canvas.drawText(dashDivider, centerX, y, paintDivider)
        y += 28f

        // 8. Totals
        canvas.drawText("Jumlah Jenis", leftX, y, paintLeftNormal)
        canvas.drawText("${receipt.totalItemTypes} produk", rightX, y, paintRightNormal)
        y += 28f

        canvas.drawText("Total Item", leftX, y, paintLeftNormal)
        canvas.drawText("${receipt.totalQuantity} pcs", rightX, y, paintRightNormal)
        y += 36f

        canvas.drawText("TOTAL", leftX, y, paintTotalTitle)
        canvas.drawText(nf.format(receipt.totalAmount).replace("Rp", "Rp "), rightX, y, paintTotalVal)
        y += 34f

        // Distribute remaining vertical space if paper height is 1600px
        val bottomSectionHeight = 28f + barcodeHeight + 8f + 24f + 16f + (footerLines.size * 28f).coerceAtLeast(28f) + 8f + 24f + 10f + 22f + cardPadding
        val extraPaperSpace = (totalHeight - bottomSectionHeight) - y
        if (extraPaperSpace > 0f) {
            y += extraPaperSpace
        }

        // 9. Divider
        canvas.drawText(equalDivider, centerX, y, paintDivider)
        y += 28f

        // 10. Barcode (Matching Preview Canvas)
        val barcodeWidth = 380f // centered matching sample image
        val barcodeLeft = centerX - (barcodeWidth / 2f)
        val barCount = 45
        val step = barcodeWidth / barCount
        val cleanStr = receipt.receiptNumber.ifBlank { "STRUK-0001" }

        for (i in 0 until barCount) {
            val charVal = cleanStr.getOrElse(i % cleanStr.length) { 'A' }.code
            val isThick = (charVal + i) % 3 == 0
            val strokeW = if (isThick) step * 0.75f else step * 0.35f
            paintBar.strokeWidth = strokeW
            val barX = barcodeLeft + (i * step) + (step / 2f)
            canvas.drawLine(barX, y, barX, y + barcodeHeight, paintBar)
        }
        y += barcodeHeight + 22f

        canvas.drawText("* $cleanStr *", centerX, y, paintBarcodeText)
        y += 32f

        // 11. Footer Notes (Synchronized with Tab Toko)
        if (footerLines.isEmpty()) {
            canvas.drawText(receipt.footerMessage, centerX, y, paintFooterMsg)
            y += 26f
        } else {
            for (fLine in footerLines) {
                canvas.drawText(fLine, centerX, y, paintFooterMsg)
                y += 26f
            }
        }
        y += 6f
        canvas.drawText("Barang yang sudah dicetak tidak dapat ditukar/dikembalikan.", centerX, y, paintFooterNote)
        y += 24f
        canvas.drawText("Waktu cetak: ${receipt.printedAtFormatted}", centerX, y, paintFooterTime)

        // Save high-res PNG to cache directory
        val cacheDir = File(context.cacheDir, "receipts").apply { if (!exists()) mkdirs() }
        val safeFileName = "struk_${receipt.receiptNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.png"
        val imageFile = File(cacheDir, safeFileName)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
}
