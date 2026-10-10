package br.com.helldiversbr.app.ui.screens

/** A color overlay that retains source detail and never changes source alpha. */
internal fun tcsWarningMatrix(red: Float, green: Float, blue: Float, strength: Float): FloatArray {
    val amount = strength.coerceIn(0f, .72f)
    val matrix = FloatArray(20)
    listOf(red, green, blue).forEachIndexed { row, value ->
        listOf(.213f, .715f, .072f).forEachIndexed { column, weight ->
            matrix[row * 5 + column] = amount * value * weight
        }
        matrix[row * 5 + row] += 1f - amount
    }
    matrix[18] = 1f
    return matrix
}
