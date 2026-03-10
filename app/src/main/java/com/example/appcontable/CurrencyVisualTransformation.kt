package com.example.appcontable

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class CurrencyVisualTransformation : VisualTransformation {
    private val symbols = DecimalFormat().decimalFormatSymbols
    private val thousandsSeparator = symbols.groupingSeparator

    override fun filter(text: AnnotatedString): TransformedText {
        // Limpiamos el texto de entrada, quitando cualquier cosa que no sea un dígito
        val inputText = text.text.filter { it.isDigit() }

        val intPart = inputText
            .toLongOrNull()
            ?.let { DecimalFormat("#,###").format(it) }
            ?: ""

        val transformedText = intPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val separators = transformedText.count { it == thousandsSeparator }
                return offset + separators
            }

            override fun transformedToOriginal(offset: Int): Int {
                val separators = transformedText.substring(0, offset).count { it == thousandsSeparator }
                return offset - separators
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}
