package space.kscience.kmath.estree

import space.kscience.kmath.ast.MstExtendedField
import space.kscience.kmath.expressions.invoke
import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.operations.invoke
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestESTreeOperationsSupport {
    @Test
    fun testUnaryOperationInvocation() {
        val expression = MstExtendedField { -bindSymbol("x") }.compileToExpression(DoubleField)
        val res = expression("x" to 2.0)
        assertEquals(-2.0, res)
    }

    @Test
    fun testBinaryOperationInvocation() {
        val expression = MstExtendedField { -bindSymbol("x") + number(1.0) }.compileToExpression(DoubleField)
        val res = expression("x" to 2.0)
        assertEquals(-1.0, res)
    }

    @Test
    fun testConstProductInvocation() {
        val res = MstExtendedField { bindSymbol("x") * 2 }.compileToExpression(DoubleField)("x" to 2.0)
        assertEquals(4.0, res)
    }

    @Test
    fun testMultipleCalls() {
        val e =
            MstExtendedField { sin(bindSymbol("x")).pow(4) - 6 * bindSymbol("x") / tanh(bindSymbol("x")) }
                .compileToExpression(DoubleField)
        val r = Random(0)
        var s = 0.0
        repeat(1000000) { s += e("x" to r.nextDouble()) }
        println(s)
    }
}