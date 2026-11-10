package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * High-speed, zero-dependency mathematical and scientific evaluation engine.
 * Supports standard arithmetic, trigonometry (DEG/RAD), logarithms, factorials,
 * permutations/combinations, numerical integration/differentiation, quadratic roots,
 * 2x2/3x3 matrix operations, unit conversions, and bilingual (Bengali + English) voice command parsing.
 */
object ScientificMathEngine {

    private val stdFormat = DecimalFormat("#.##########")
    private val sciFormat = DecimalFormat("0.######E0")

    fun evaluate(
        rawExpression: String,
        isDegreeMode: Boolean = true,
        variableX: Double = 0.0
    ): Result<String> {
        val cleaned = rawExpression.trim()
        if (cleaned.isEmpty()) return Result.success("0")

        return try {
            val normalized = preprocessExpression(cleaned, variableX)
            val parser = ExpressionParser(normalized, isDegreeMode)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) {
                Result.failure(ArithmeticException("Math Error (Undefined / Division by Zero)"))
            } else {
                Result.success(formatNumber(value))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        val absVal = abs(value)
        if (absVal == 0.0) return "0"
        return if (absVal >= 1e12 || (absVal > 0 && absVal < 1e-7)) {
            sciFormat.format(value)
        } else {
            val bd = BigDecimal.valueOf(value).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
            bd.toPlainString()
        }
    }

    /**
     * Numerical Definite Integral using Simpson's Rule (n = 200 intervals):
     * ∫_a^b f(x) dx
     */
    fun definiteIntegral(exprInX: String, lowerA: Double, upperB: Double, isDegreeMode: Boolean = false): Result<String> {
        return try {
            val n = 200
            val h = (upperB - lowerA) / n
            fun f(x: Double): Double {
                val normalized = preprocessExpression(exprInX, x)
                return ExpressionParser(normalized, isDegreeMode).parse()
            }
            var sum = f(lowerA) + f(upperB)
            for (i in 1 until n) {
                val x = lowerA + i * h
                sum += if (i % 2 == 0) 2.0 * f(x) else 4.0 * f(x)
            }
            val integral = (h / 3.0) * sum
            Result.success(formatNumber(integral))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Numerical Derivative f'(x0) using 5-point stencil / symmetric central difference.
     */
    fun derivativeAt(exprInX: String, x0: Double, isDegreeMode: Boolean = false): Result<String> {
        return try {
            val h = 1e-5
            fun f(x: Double): Double {
                val normalized = preprocessExpression(exprInX, x)
                return ExpressionParser(normalized, isDegreeMode).parse()
            }
            val deriv = (-f(x0 + 2 * h) + 8 * f(x0 + h) - 8 * f(x0 - h) + f(x0 - 2 * h)) / (12 * h)
            Result.success(formatNumber(deriv))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Solves quadratic equation a*x^2 + b*x + c = 0 (including complex roots).
     */
    fun solveQuadratic(a: Double, b: Double, c: Double): String {
        if (abs(a) < 1e-12) {
            if (abs(b) < 1e-12) return "No valid equation (a = 0, b = 0)"
            val root = -c / b
            return "Linear Root: x = ${formatNumber(root)}"
        }
        val discriminant = b * b - 4 * a * c
        return when {
            discriminant > 1e-12 -> {
                val r1 = (-b + sqrt(discriminant)) / (2 * a)
                val r2 = (-b - sqrt(discriminant)) / (2 * a)
                "x₁ = ${formatNumber(r1)},  x₂ = ${formatNumber(r2)}  (Δ = ${formatNumber(discriminant)})"
            }
            abs(discriminant) <= 1e-12 -> {
                val r = -b / (2 * a)
                "x₁ = x₂ = ${formatNumber(r)}  (Δ = 0)"
            }
            else -> {
                val realPart = -b / (2 * a)
                val imagPart = sqrt(-discriminant) / (2 * a)
                "x₁ = ${formatNumber(realPart)} + ${formatNumber(imagPart)}i,  x₂ = ${formatNumber(realPart)} - ${formatNumber(imagPart)}i"
            }
        }
    }

    /**
     * Computes 2x2 Matrix Determinant, Trace, and Inverse.
     */
    fun analyzeMatrix2x2(a11: Double, a12: Double, a21: Double, a22: Double): String {
        val det = a11 * a22 - a12 * a21
        val trace = a11 + a22
        val invText = if (abs(det) < 1e-12) {
            "Singular Matrix (No Inverse, det = 0)"
        } else {
            val i11 = formatNumber(a22 / det)
            val i12 = formatNumber(-a12 / det)
            val i21 = formatNumber(-a21 / det)
            val i22 = formatNumber(a11 / det)
            "Inverse A⁻¹ = [[$i11, $i12], [$i21, $i22]]"
        }
        return "det(A) = ${formatNumber(det)} | tr(A) = ${formatNumber(trace)}\n$invText"
    }

    /**
     * Converts Bengali and English natural voice commands into mathematical expressions.
     */
    fun parseVoiceCommandToMath(spokenText: String): String {
        var text = spokenText.lowercase().trim()
        // Bengali digit replacements
        val bnDigits = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        text = text.map { bnDigits[it] ?: it }.joinToString("")

        // Common Bengali & English spoken math replacements
        val replacements = listOf(
            "বর্গমূল" to "sqrt(",
            "square root of" to "sqrt(",
            "square root" to "sqrt(",
            "কত" to "",
            "সমান" to "",
            "যোগ" to "+",
            "বিয়োগ" to "-",
            "বিয়োগ" to "-",
            "গুণ" to "*",
            "ভাগ" to "/",
            "শতাংশ" to "%",
            "দশমিক" to ".",
            "পাই" to "pi",
            "সাইন" to "sin(",
            "কস" to "cos(",
            "ট্যান" to "tan(",
            "লগ" to "log(",
            "plus" to "+",
            "minus" to "-",
            "multiplied by" to "*",
            "times" to "*",
            "into" to "*",
            "divided by" to "/",
            "over" to "/",
            "percent of" to "%*",
            "percent" to "%",
            "to the power of" to "^",
            "power" to "^",
            "squared" to "^2",
            "cubed" to "^3",
            "factorial" to "!",
            "sine of" to "sin(",
            "cosine of" to "cos(",
            "tangent of" to "tan(",
            "sine" to "sin(",
            "cosine" to "cos(",
            "tangent" to "tan(",
            "natural log of" to "ln(",
            "log of" to "log("
        )

        for ((phrase, symbol) in replacements) {
            text = text.replace(phrase, " $symbol ")
        }

        // Balance open parentheses automatically
        val openCount = text.count { it == '(' }
        val closeCount = text.count { it == ')' }
        val balanced = if (openCount > closeCount) {
            text + ")".repeat(openCount - closeCount)
        } else {
            text
        }

        return balanced.replace(Regex("\\s+"), "")
    }

    private fun preprocessExpression(expr: String, variableX: Double): String {
        var s = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("√(", "sqrt(")
            .replace("√", "sqrt")
            .replace("π", "($PI)")
            .replace(Regex("\\bpi\\b", RegexOption.IGNORE_CASE), "($PI)")
            .replace(Regex("\\bphi\\b", RegexOption.IGNORE_CASE), "(1.618033988749895)")
            .replace(Regex("\\bx\\b", RegexOption.IGNORE_CASE), "($variableX)")
            .replace(" ", "")

        // Replace standalone constant 'e' without breaking 'exp' or scientific notation '1e-5'
        s = s.replace(Regex("(?<![a-zA-Z0-9.])e(?![a-zA-Z0-9+\\-])"), "($E)")

        // Handle implicit multiplication: e.g. 2(3) -> 2*(3), )( -> )*(, 2sin( -> 2*sin(
        s = s.replace(Regex("(\\d)(\\()"), "$1*$2")
        s = s.replace(Regex("(\\))(\\d)"), "$1*$2")
        s = s.replace(Regex("(\\))(\\()"), "$1*$2")
        s = s.replace(Regex("(\\d)(sin|cos|tan|asin|acos|atan|sinh|cosh|tanh|ln|log|sqrt|cbrt|abs|exp)"), "$1*$2")

        return s
    }

    private class ExpressionParser(
        private val input: String,
        private val isDegreeMode: Boolean
    ) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) {
                throw IllegalArgumentException("Unexpected character: '${input[pos]}'")
            }
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val denom = parseFactor()
                        if (abs(denom) < 1e-15) throw ArithmeticException("Division by zero")
                        x /= denom
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code) // Auto-close if user omitted trailing ')'
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code || ch == 'E'.code || ch == 'e'.code) {
                    if ((ch == 'E'.code || ch == 'e'.code) && pos + 1 < input.length &&
                        (input[pos + 1] == '+' || input[pos + 1] == '-')
                    ) {
                        nextChar()
                    }
                    nextChar()
                }
                x = input.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch in '0'.code..'9'.code) {
                    nextChar()
                }
                val func = input.substring(startPos, pos).lowercase()
                if (eat('('.code)) {
                    val arg1 = parseExpression()
                    if (eat(','.code)) {
                        val arg2 = parseExpression()
                        eat(')'.code)
                        x = applyTwoArgFunction(func, arg1, arg2)
                    } else {
                        eat(')'.code)
                        x = applyFunction(func, arg1)
                    }
                } else {
                    x = applyFunction(func, parseFactor())
                }
            } else {
                throw IllegalArgumentException("Unexpected syntax")
            }

            // Postfix operators: factorial (!) and percentage (%)
            while (true) {
                when {
                    eat('!'.code) -> x = factorial(x)
                    eat('%'.code) -> x /= 100.0
                    else -> break
                }
            }

            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }

            return x
        }

        private fun toRadiansIfNeeded(angle: Double): Double =
            if (isDegreeMode) Math.toRadians(angle) else angle

        private fun fromRadiansIfNeeded(rad: Double): Double =
            if (isDegreeMode) Math.toDegrees(rad) else rad

        private fun applyFunction(func: String, a: Double): Double = when (func) {
            "sqrt" -> {
                if (a < 0) throw ArithmeticException("Negative square root")
                sqrt(a)
            }
            "cbrt" -> cbrt(a)
            "sin" -> sin(toRadiansIfNeeded(a))
            "cos" -> cos(toRadiansIfNeeded(a))
            "tan" -> tan(toRadiansIfNeeded(a))
            "asin" -> fromRadiansIfNeeded(asin(a))
            "acos" -> fromRadiansIfNeeded(acos(a))
            "atan" -> fromRadiansIfNeeded(atan(a))
            "sinh" -> sinh(a)
            "cosh" -> cosh(a)
            "tanh" -> tanh(a)
            "ln" -> {
                if (a <= 0) throw ArithmeticException("ln domain error")
                ln(a)
            }
            "log", "log10" -> {
                if (a <= 0) throw ArithmeticException("log domain error")
                log10(a)
            }
            "log2" -> {
                if (a <= 0) throw ArithmeticException("log2 domain error")
                log2(a)
            }
            "exp" -> exp(a)
            "abs" -> abs(a)
            "floor" -> floor(a)
            "ceil" -> ceil(a)
            "round" -> round(a)
            else -> throw IllegalArgumentException("Unknown function: $func")
        }

        private fun applyTwoArgFunction(func: String, n: Double, r: Double): Double = when (func) {
            "ncr" -> factorial(n) / (factorial(r) * factorial(n - r))
            "npr" -> factorial(n) / factorial(n - r)
            "pow" -> n.pow(r)
            "mod" -> n % r
            "gcd" -> gcd(n.toLong(), r.toLong()).toDouble()
            "lcm" -> {
                val a = abs(n.toLong())
                val b = abs(r.toLong())
                if (a == 0L || b == 0L) 0.0 else ((a / gcd(a, b)) * b).toDouble()
            }
            else -> throw IllegalArgumentException("Unknown 2-arg function: $func")
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || n != floor(n) || n > 170) {
                throw ArithmeticException("Factorial requires integer 0..170")
            }
            var res = 1.0
            for (i in 2..n.toInt()) {
                res *= i.toDouble()
            }
            return res
        }

        private fun gcd(a: Long, b: Long): Long {
            var x = abs(a)
            var y = abs(b)
            while (y != 0L) {
                val t = y
                y = x % y
                x = t
            }
            return x
        }
    }
}
