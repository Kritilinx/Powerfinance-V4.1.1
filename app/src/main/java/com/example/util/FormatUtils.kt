package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val indonesianSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val rupiahFormatter = DecimalFormat("#,##0.00", indonesianSymbols)
    private val roundedRupiahFormatter = DecimalFormat("#,##0", indonesianSymbols)

    fun formatRupiah(amount: Double, withCents: Boolean = true): String {
        val formatted = if (withCents || (amount % 1.0 != 0.0)) {
            rupiahFormatter.format(amount)
        } else {
            roundedRupiahFormatter.format(amount)
        }
        return "Rp$formatted"
    }

    fun formatDate(epochMs: Long, pattern: String = "dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
        return sdf.format(Date(epochMs))
    }

    fun formatDateTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
        return sdf.format(Date(epochMs))
    }

    fun parseRupiahInput(input: String): Double? {
        val cleaned = input
            .replace("Rp", "")
            .replace("rp", "")
            .replace(" ", "")
            .replace(".", "") // remove thousand dots
            .replace(",", ".") // convert comma decimal to dot
        return cleaned.toDoubleOrNull()
    }
}
