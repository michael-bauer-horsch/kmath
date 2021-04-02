package space.kscience.kmath.estree

import space.kscience.kmath.ast.MstRing
import space.kscience.kmath.expressions.invoke
import space.kscience.kmath.operations.ByteRing
import space.kscience.kmath.operations.invoke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TestESTreeVariables {
    @Test
    fun testVariable() {
        val expr = MstRing{ bindSymbol("x") }.compileToExpression(ByteRing)
        assertEquals(1.toByte(), expr("x" to 1.toByte()))
    }

    @Test
    fun testUndefinedVariableFails() {
        val expr = MstRing { bindSymbol("x") }.compileToExpression(ByteRing)
        assertFailsWith<NoSuchElementException> { expr() }
    }
}