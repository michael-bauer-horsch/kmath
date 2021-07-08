/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.tensors.core

import space.kscience.kmath.nd.as1D
import space.kscience.kmath.nd.as2D
import space.kscience.kmath.tensors.api.AnalyticTensorAlgebra
import space.kscience.kmath.tensors.api.LinearOpsTensorAlgebra
import space.kscience.kmath.tensors.api.Tensor
import space.kscience.kmath.tensors.api.TensorPartialDivisionAlgebra
import space.kscience.kmath.tensors.core.internal.*
import kotlin.math.*

/**
 * Implementation of basic operations over double tensors and basic algebra operations on them.
 */
public open class DoubleTensorAlgebra :
    TensorPartialDivisionAlgebra<Double>,
    AnalyticTensorAlgebra<Double>,
    LinearOpsTensorAlgebra<Double> {

    public companion object : DoubleTensorAlgebra()


    /**
     * Returns a single tensor value of unit dimension if tensor shape equals to [1].
     *
     * @return a nullable value of a potentially scalar tensor.
     */
    override fun Tensor<Double>.valueOrNull(): Double? = if (tensor.shape contentEquals intArrayOf(1))
        tensor.mutableBuffer.array()[tensor.bufferStart] else null

    /**
     * Returns a single tensor value of unit dimension. The tensor shape must be equal to [1].
     *
     * @return the value of a scalar tensor.
     */
    override fun Tensor<Double>.value(): Double = valueOrNull()
        ?: throw IllegalArgumentException("The tensor shape is $shape, but value method is allowed only for shape [1]")

    /**
     * Constructs a tensor with the specified shape and data.
     *
     * @param shape the desired shape for the tensor.
     * @param buffer one-dimensional data array.
     * @return tensor with the [shape] shape and [buffer] data.
     */
    public fun fromArray(shape: IntArray, buffer: DoubleArray): DoubleTensor {
        checkEmptyShape(shape)
        checkEmptyDoubleBuffer(buffer)
        checkBufferShapeConsistency(shape, buffer)
        return DoubleTensor(shape, buffer, 0)
    }

    /**
     * Constructs a tensor with the specified shape and initializer.
     *
     * @param shape the desired shape for the tensor.
     * @param initializer mapping tensor indices to values.
     * @return tensor with the [shape] shape and data generated by the [initializer].
     */
    public fun produce(shape: IntArray, initializer: (IntArray) -> Double): DoubleTensor =
        fromArray(
            shape,
            TensorLinearStructure(shape).indices().map(initializer).toMutableList().toDoubleArray()
        )

    override operator fun Tensor<Double>.get(i: Int): DoubleTensor {
        val lastShape = tensor.shape.drop(1).toIntArray()
        val newShape = if (lastShape.isNotEmpty()) lastShape else intArrayOf(1)
        val newStart = newShape.reduce(Int::times) * i + tensor.bufferStart
        return DoubleTensor(newShape, tensor.mutableBuffer.array(), newStart)
    }

    /**
     * Creates a tensor of a given shape and fills all elements with a given value.
     *
     * @param value the value to fill the output tensor with.
     * @param shape array of integers defining the shape of the output tensor.
     * @return tensor with the [shape] shape and filled with [value].
     */
    public fun full(value: Double, shape: IntArray): DoubleTensor {
        checkEmptyShape(shape)
        val buffer = DoubleArray(shape.reduce(Int::times)) { value }
        return DoubleTensor(shape, buffer)
    }

    /**
     * Returns a tensor with the same shape as `input` filled with [value].
     *
     * @param value the value to fill the output tensor with.
     * @return tensor with the `input` tensor shape and filled with [value].
     */
    public fun Tensor<Double>.fullLike(value: Double): DoubleTensor {
        val shape = tensor.shape
        val buffer = DoubleArray(tensor.numElements) { value }
        return DoubleTensor(shape, buffer)
    }

    /**
     * Returns a tensor filled with the scalar value 0.0, with the shape defined by the variable argument [shape].
     *
     * @param shape array of integers defining the shape of the output tensor.
     * @return tensor filled with the scalar value 0.0, with the [shape] shape.
     */
    public fun zeros(shape: IntArray): DoubleTensor = full(0.0, shape)

    /**
     * Returns a tensor filled with the scalar value 0.0, with the same shape as a given array.
     *
     * @return tensor filled with the scalar value 0.0, with the same shape as `input` tensor.
     */
    public fun Tensor<Double>.zeroesLike(): DoubleTensor = tensor.fullLike(0.0)

    /**
     * Returns a tensor filled with the scalar value 1.0, with the shape defined by the variable argument [shape].
     *
     * @param shape array of integers defining the shape of the output tensor.
     * @return tensor filled with the scalar value 1.0, with the [shape] shape.
     */
    public fun ones(shape: IntArray): DoubleTensor = full(1.0, shape)

    /**
     * Returns a tensor filled with the scalar value 1.0, with the same shape as a given array.
     *
     * @return tensor filled with the scalar value 1.0, with the same shape as `input` tensor.
     */
    public fun Tensor<Double>.onesLike(): DoubleTensor = tensor.fullLike(1.0)

    /**
     * Returns a 2-D tensor with shape ([n], [n]), with ones on the diagonal and zeros elsewhere.
     *
     * @param n the number of rows and columns
     * @return a 2-D tensor with ones on the diagonal and zeros elsewhere.
     */
    public fun eye(n: Int): DoubleTensor {
        val shape = intArrayOf(n, n)
        val buffer = DoubleArray(n * n) { 0.0 }
        val res = DoubleTensor(shape, buffer)
        for (i in 0 until n) {
            res[intArrayOf(i, i)] = 1.0
        }
        return res
    }

    /**
     * Return a copy of the tensor.
     *
     * @return a copy of the `input` tensor with a copied buffer.
     */
    public fun Tensor<Double>.copy(): DoubleTensor {
        return DoubleTensor(tensor.shape, tensor.mutableBuffer.array().copyOf(), tensor.bufferStart)
    }

    override fun Double.plus(other: Tensor<Double>): DoubleTensor {
        val resBuffer = DoubleArray(other.tensor.numElements) { i ->
            other.tensor.mutableBuffer.array()[other.tensor.bufferStart + i] + this
        }
        return DoubleTensor(other.shape, resBuffer)
    }

    override fun Tensor<Double>.plus(value: Double): DoubleTensor = value + tensor

    override fun Tensor<Double>.plus(other: Tensor<Double>): DoubleTensor {
        checkShapesCompatible(tensor, other.tensor)
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[i] + other.tensor.mutableBuffer.array()[i]
        }
        return DoubleTensor(tensor.shape, resBuffer)
    }

    override fun Tensor<Double>.plusAssign(value: Double) {
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] += value
        }
    }

    override fun Tensor<Double>.plusAssign(other: Tensor<Double>) {
        checkShapesCompatible(tensor, other.tensor)
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] +=
                other.tensor.mutableBuffer.array()[tensor.bufferStart + i]
        }
    }

    override fun Double.minus(other: Tensor<Double>): DoubleTensor {
        val resBuffer = DoubleArray(other.tensor.numElements) { i ->
            this - other.tensor.mutableBuffer.array()[other.tensor.bufferStart + i]
        }
        return DoubleTensor(other.shape, resBuffer)
    }

    override fun Tensor<Double>.minus(value: Double): DoubleTensor {
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[tensor.bufferStart + i] - value
        }
        return DoubleTensor(tensor.shape, resBuffer)
    }

    override fun Tensor<Double>.minus(other: Tensor<Double>): DoubleTensor {
        checkShapesCompatible(tensor, other)
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[i] - other.tensor.mutableBuffer.array()[i]
        }
        return DoubleTensor(tensor.shape, resBuffer)
    }

    override fun Tensor<Double>.minusAssign(value: Double) {
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] -= value
        }
    }

    override fun Tensor<Double>.minusAssign(other: Tensor<Double>) {
        checkShapesCompatible(tensor, other)
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] -=
                other.tensor.mutableBuffer.array()[tensor.bufferStart + i]
        }
    }

    override fun Double.times(other: Tensor<Double>): DoubleTensor {
        val resBuffer = DoubleArray(other.tensor.numElements) { i ->
            other.tensor.mutableBuffer.array()[other.tensor.bufferStart + i] * this
        }
        return DoubleTensor(other.shape, resBuffer)
    }

    override fun Tensor<Double>.times(value: Double): DoubleTensor = value * tensor

    override fun Tensor<Double>.times(other: Tensor<Double>): DoubleTensor {
        checkShapesCompatible(tensor, other)
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[tensor.bufferStart + i] *
                    other.tensor.mutableBuffer.array()[other.tensor.bufferStart + i]
        }
        return DoubleTensor(tensor.shape, resBuffer)
    }

    override fun Tensor<Double>.timesAssign(value: Double) {
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] *= value
        }
    }

    override fun Tensor<Double>.timesAssign(other: Tensor<Double>) {
        checkShapesCompatible(tensor, other)
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] *=
                other.tensor.mutableBuffer.array()[tensor.bufferStart + i]
        }
    }

    override fun Double.div(other: Tensor<Double>): DoubleTensor {
        val resBuffer = DoubleArray(other.tensor.numElements) { i ->
            this / other.tensor.mutableBuffer.array()[other.tensor.bufferStart + i]
        }
        return DoubleTensor(other.shape, resBuffer)
    }

    override fun Tensor<Double>.div(value: Double): DoubleTensor {
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[tensor.bufferStart + i] / value
        }
        return DoubleTensor(shape, resBuffer)
    }

    override fun Tensor<Double>.div(other: Tensor<Double>): DoubleTensor {
        checkShapesCompatible(tensor, other)
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[other.tensor.bufferStart + i] /
                    other.tensor.mutableBuffer.array()[other.tensor.bufferStart + i]
        }
        return DoubleTensor(tensor.shape, resBuffer)
    }

    override fun Tensor<Double>.divAssign(value: Double) {
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] /= value
        }
    }

    override fun Tensor<Double>.divAssign(other: Tensor<Double>) {
        checkShapesCompatible(tensor, other)
        for (i in 0 until tensor.numElements) {
            tensor.mutableBuffer.array()[tensor.bufferStart + i] /=
                other.tensor.mutableBuffer.array()[tensor.bufferStart + i]
        }
    }

    override fun Tensor<Double>.unaryMinus(): DoubleTensor {
        val resBuffer = DoubleArray(tensor.numElements) { i ->
            tensor.mutableBuffer.array()[tensor.bufferStart + i].unaryMinus()
        }
        return DoubleTensor(tensor.shape, resBuffer)
    }

    override fun Tensor<Double>.transpose(i: Int, j: Int): DoubleTensor {
        val ii = tensor.minusIndex(i)
        val jj = tensor.minusIndex(j)
        checkTranspose(tensor.dimension, ii, jj)
        val n = tensor.numElements
        val resBuffer = DoubleArray(n)

        val resShape = tensor.shape.copyOf()
        resShape[ii] = resShape[jj].also { resShape[jj] = resShape[ii] }

        val resTensor = DoubleTensor(resShape, resBuffer)

        for (offset in 0 until n) {
            val oldMultiIndex = tensor.linearStructure.index(offset)
            val newMultiIndex = oldMultiIndex.copyOf()
            newMultiIndex[ii] = newMultiIndex[jj].also { newMultiIndex[jj] = newMultiIndex[ii] }

            val linearIndex = resTensor.linearStructure.offset(newMultiIndex)
            resTensor.mutableBuffer.array()[linearIndex] =
                tensor.mutableBuffer.array()[tensor.bufferStart + offset]
        }
        return resTensor
    }


    override fun Tensor<Double>.view(shape: IntArray): DoubleTensor {
        checkView(tensor, shape)
        return DoubleTensor(shape, tensor.mutableBuffer.array(), tensor.bufferStart)
    }

    override fun Tensor<Double>.viewAs(other: Tensor<Double>): DoubleTensor =
        tensor.view(other.shape)

    override infix fun Tensor<Double>.dot(other: Tensor<Double>): DoubleTensor {
        if (tensor.shape.size == 1 && other.shape.size == 1) {
            return DoubleTensor(intArrayOf(1), doubleArrayOf(tensor.times(other).tensor.mutableBuffer.array().sum()))
        }

        var newThis = tensor.copy()
        var newOther = other.copy()

        var penultimateDim = false
        var lastDim = false
        if (tensor.shape.size == 1) {
            penultimateDim = true
            newThis = tensor.view(intArrayOf(1) + tensor.shape)
        }
        if (other.shape.size == 1) {
            lastDim = true
            newOther = other.tensor.view(other.shape + intArrayOf(1))
        }

        val broadcastTensors = broadcastOuterTensors(newThis.tensor, newOther.tensor)
        newThis = broadcastTensors[0]
        newOther = broadcastTensors[1]

        val l = newThis.shape[newThis.shape.size - 2]
        val m1 = newThis.shape[newThis.shape.size - 1]
        val m2 = newOther.shape[newOther.shape.size - 2]
        val n = newOther.shape[newOther.shape.size - 1]
        check(m1 == m2) {
            "Tensors dot operation dimension mismatch: ($l, $m1) x ($m2, $n)"
        }

        val resShape = newThis.shape.sliceArray(0..(newThis.shape.size - 2)) + intArrayOf(newOther.shape.last())
        val resSize = resShape.reduce { acc, i -> acc * i }
        val resTensor = DoubleTensor(resShape, DoubleArray(resSize))

        for ((res, ab) in resTensor.matrixSequence().zip(newThis.matrixSequence().zip(newOther.matrixSequence()))) {
            val (a, b) = ab
            dotHelper(a.as2D(), b.as2D(), res.as2D(), l, m1, n)
        }

        if (penultimateDim) {
            return resTensor.view(
                resTensor.shape.dropLast(2).toIntArray() +
                        intArrayOf(resTensor.shape.last())
            )
        }
        if (lastDim) {
            return resTensor.view(resTensor.shape.dropLast(1).toIntArray())
        }
        return resTensor
    }

    override fun diagonalEmbedding(diagonalEntries: Tensor<Double>, offset: Int, dim1: Int, dim2: Int):
            DoubleTensor {
        val n = diagonalEntries.shape.size
        val d1 = minusIndexFrom(n + 1, dim1)
        val d2 = minusIndexFrom(n + 1, dim2)

        check(d1 != d2) {
            "Diagonal dimensions cannot be identical $d1, $d2"
        }
        check(d1 <= n && d2 <= n) {
            "Dimension out of range"
        }

        var lessDim = d1
        var greaterDim = d2
        var realOffset = offset
        if (lessDim > greaterDim) {
            realOffset *= -1
            lessDim = greaterDim.also { greaterDim = lessDim }
        }

        val resShape = diagonalEntries.shape.slice(0 until lessDim).toIntArray() +
                intArrayOf(diagonalEntries.shape[n - 1] + abs(realOffset)) +
                diagonalEntries.shape.slice(lessDim until greaterDim - 1).toIntArray() +
                intArrayOf(diagonalEntries.shape[n - 1] + abs(realOffset)) +
                diagonalEntries.shape.slice(greaterDim - 1 until n - 1).toIntArray()
        val resTensor = zeros(resShape)

        for (i in 0 until diagonalEntries.tensor.numElements) {
            val multiIndex = diagonalEntries.tensor.linearStructure.index(i)

            var offset1 = 0
            var offset2 = abs(realOffset)
            if (realOffset < 0) {
                offset1 = offset2.also { offset2 = offset1 }
            }
            val diagonalMultiIndex = multiIndex.slice(0 until lessDim).toIntArray() +
                    intArrayOf(multiIndex[n - 1] + offset1) +
                    multiIndex.slice(lessDim until greaterDim - 1).toIntArray() +
                    intArrayOf(multiIndex[n - 1] + offset2) +
                    multiIndex.slice(greaterDim - 1 until n - 1).toIntArray()

            resTensor[diagonalMultiIndex] = diagonalEntries[multiIndex]
        }

        return resTensor.tensor
    }

    /**
     * Applies the [transform] function to each element of the tensor and returns the resulting modified tensor.
     *
     * @param transform the function to be applied to each element of the tensor.
     * @return the resulting tensor after applying the function.
     */
    public fun Tensor<Double>.map(transform: (Double) -> Double): DoubleTensor {
        return DoubleTensor(
            tensor.shape,
            tensor.mutableBuffer.array().map { transform(it) }.toDoubleArray(),
            tensor.bufferStart
        )
    }

    /**
     * Compares element-wise two tensors with a specified precision.
     *
     * @param other the tensor to compare with `input` tensor.
     * @param epsilon permissible error when comparing two Double values.
     * @return true if two tensors have the same shape and elements, false otherwise.
     */
    public fun Tensor<Double>.eq(other: Tensor<Double>, epsilon: Double): Boolean =
        tensor.eq(other) { x, y -> abs(x - y) < epsilon }

    /**
     * Compares element-wise two tensors.
     * Comparison of two Double values occurs with 1e-5 precision.
     *
     * @param other the tensor to compare with `input` tensor.
     * @return true if two tensors have the same shape and elements, false otherwise.
     */
    public infix fun Tensor<Double>.eq(other: Tensor<Double>): Boolean = tensor.eq(other, 1e-5)

    private fun Tensor<Double>.eq(
        other: Tensor<Double>,
        eqFunction: (Double, Double) -> Boolean,
    ): Boolean {
        checkShapesCompatible(tensor, other)
        val n = tensor.numElements
        if (n != other.tensor.numElements) {
            return false
        }
        for (i in 0 until n) {
            if (!eqFunction(
                    tensor.mutableBuffer[tensor.bufferStart + i],
                    other.tensor.mutableBuffer[other.tensor.bufferStart + i]
                )
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Returns a tensor of random numbers drawn from normal distributions with 0.0 mean and 1.0 standard deviation.
     *
     * @param shape the desired shape for the output tensor.
     * @param seed the random seed of the pseudo-random number generator.
     * @return tensor of a given shape filled with numbers from the normal distribution
     * with 0.0 mean and 1.0 standard deviation.
     */
    public fun randomNormal(shape: IntArray, seed: Long = 0): DoubleTensor =
        DoubleTensor(shape, getRandomNormals(shape.reduce(Int::times), seed))

    /**
     * Returns a tensor with the same shape as `input` of random numbers drawn from normal distributions
     * with 0.0 mean and 1.0 standard deviation.
     *
     * @param seed the random seed of the pseudo-random number generator.
     * @return tensor with the same shape as `input` filled with numbers from the normal distribution
     * with 0.0 mean and 1.0 standard deviation.
     */
    public fun Tensor<Double>.randomNormalLike(seed: Long = 0): DoubleTensor =
        DoubleTensor(tensor.shape, getRandomNormals(tensor.shape.reduce(Int::times), seed))

    /**
     * Concatenates a sequence of tensors with equal shapes along the first dimension.
     *
     * @param tensors the [List] of tensors with same shapes to concatenate
     * @return tensor with concatenation result
     */
    public fun stack(tensors: List<Tensor<Double>>): DoubleTensor {
        check(tensors.isNotEmpty()) { "List must have at least 1 element" }
        val shape = tensors[0].shape
        check(tensors.all { it.shape contentEquals shape }) { "Tensors must have same shapes" }
        val resShape = intArrayOf(tensors.size) + shape
        val resBuffer = tensors.flatMap {
            it.tensor.mutableBuffer.array().drop(it.tensor.bufferStart).take(it.tensor.numElements)
        }.toDoubleArray()
        return DoubleTensor(resShape, resBuffer, 0)
    }

    /**
     * Builds tensor from rows of input tensor
     *
     * @param indices the [IntArray] of 1-dimensional indices
     * @return tensor with rows corresponding to rows by [indices]
     */
    public fun Tensor<Double>.rowsByIndices(indices: IntArray): DoubleTensor {
        return stack(indices.map { this[it] })
    }

    internal fun Tensor<Double>.fold(foldFunction: (DoubleArray) -> Double): Double =
        foldFunction(tensor.toDoubleArray())

    internal fun Tensor<Double>.foldDim(
        foldFunction: (DoubleArray) -> Double,
        dim: Int,
        keepDim: Boolean,
    ): DoubleTensor {
        check(dim < dimension) { "Dimension $dim out of range $dimension" }
        val resShape = if (keepDim) {
            shape.take(dim).toIntArray() + intArrayOf(1) + shape.takeLast(dimension - dim - 1).toIntArray()
        } else {
            shape.take(dim).toIntArray() + shape.takeLast(dimension - dim - 1).toIntArray()
        }
        val resNumElements = resShape.reduce(Int::times)
        val resTensor = DoubleTensor(resShape, DoubleArray(resNumElements) { 0.0 }, 0)
        for (index in resTensor.linearStructure.indices()) {
            val prefix = index.take(dim).toIntArray()
            val suffix = index.takeLast(dimension - dim - 1).toIntArray()
            resTensor[index] = foldFunction(DoubleArray(shape[dim]) { i ->
                tensor[prefix + intArrayOf(i) + suffix]
            })
        }

        return resTensor
    }

    override fun Tensor<Double>.sum(): Double = tensor.fold { it.sum() }

    override fun Tensor<Double>.sum(dim: Int, keepDim: Boolean): DoubleTensor =
        foldDim({ x -> x.sum() }, dim, keepDim)

    override fun Tensor<Double>.min(): Double = this.fold { it.minOrNull()!! }

    override fun Tensor<Double>.min(dim: Int, keepDim: Boolean): DoubleTensor =
        foldDim({ x -> x.minOrNull()!! }, dim, keepDim)

    override fun Tensor<Double>.max(): Double = this.fold { it.maxOrNull()!! }

    override fun Tensor<Double>.max(dim: Int, keepDim: Boolean): DoubleTensor =
        foldDim({ x -> x.maxOrNull()!! }, dim, keepDim)


    /**
     * Returns the index of maximum value of each row of the input tensor in the given dimension [dim].
     *
     * If [keepDim] is true, the output tensor is of the same size as
     * input except in the dimension [dim] where it is of size 1.
     * Otherwise, [dim] is squeezed, resulting in the output tensor having 1 fewer dimension.
     *
     * @param dim the dimension to reduce.
     * @param keepDim whether the output tensor has [dim] retained or not.
     * @return the the index of maximum value of each row of the input tensor in the given dimension [dim].
     */
    public fun Tensor<Double>.argMax(dim: Int, keepDim: Boolean): DoubleTensor =
        foldDim({ x ->
            x.withIndex().maxByOrNull { it.value }?.index!!.toDouble()
        }, dim, keepDim)


    override fun Tensor<Double>.mean(): Double = this.fold { it.sum() / tensor.numElements }

    override fun Tensor<Double>.mean(dim: Int, keepDim: Boolean): DoubleTensor =
        foldDim(
            { arr ->
                check(dim < dimension) { "Dimension $dim out of range $dimension" }
                arr.sum() / shape[dim]
            },
            dim,
            keepDim
        )

    override fun Tensor<Double>.std(): Double = this.fold { arr ->
        val mean = arr.sum() / tensor.numElements
        sqrt(arr.sumOf { (it - mean) * (it - mean) } / (tensor.numElements - 1))
    }

    override fun Tensor<Double>.std(dim: Int, keepDim: Boolean): DoubleTensor = foldDim(
        { arr ->
            check(dim < dimension) { "Dimension $dim out of range $dimension" }
            val mean = arr.sum() / shape[dim]
            sqrt(arr.sumOf { (it - mean) * (it - mean) } / (shape[dim] - 1))
        },
        dim,
        keepDim
    )

    override fun Tensor<Double>.variance(): Double = this.fold { arr ->
        val mean = arr.sum() / tensor.numElements
        arr.sumOf { (it - mean) * (it - mean) } / (tensor.numElements - 1)
    }

    override fun Tensor<Double>.variance(dim: Int, keepDim: Boolean): DoubleTensor = foldDim(
        { arr ->
            check(dim < dimension) { "Dimension $dim out of range $dimension" }
            val mean = arr.sum() / shape[dim]
            arr.sumOf { (it - mean) * (it - mean) } / (shape[dim] - 1)
        },
        dim,
        keepDim
    )

    private fun cov(x: DoubleTensor, y: DoubleTensor): Double {
        val n = x.shape[0]
        return ((x - x.mean()) * (y - y.mean())).mean() * n / (n - 1)
    }

    /**
     * Returns the covariance matrix M of given vectors.
     *
     * M[i, j] contains covariance of i-th and j-th given vectors
     *
     * @param tensors the [List] of 1-dimensional tensors with same shape
     * @return the covariance matrix
     */
    public fun cov(tensors: List<Tensor<Double>>): DoubleTensor {
        check(tensors.isNotEmpty()) { "List must have at least 1 element" }
        val n = tensors.size
        val m = tensors[0].shape[0]
        check(tensors.all { it.shape contentEquals intArrayOf(m) }) { "Tensors must have same shapes" }
        val resTensor = DoubleTensor(
            intArrayOf(n, n),
            DoubleArray(n * n) { 0.0 }
        )
        for (i in 0 until n) {
            for (j in 0 until n) {
                resTensor[intArrayOf(i, j)] = cov(tensors[i].tensor, tensors[j].tensor)
            }
        }
        return resTensor
    }

    override fun Tensor<Double>.exp(): DoubleTensor = tensor.map(::exp)

    override fun Tensor<Double>.ln(): DoubleTensor = tensor.map(::ln)

    override fun Tensor<Double>.sqrt(): DoubleTensor = tensor.map(::sqrt)

    override fun Tensor<Double>.cos(): DoubleTensor = tensor.map(::cos)

    override fun Tensor<Double>.acos(): DoubleTensor = tensor.map(::acos)

    override fun Tensor<Double>.cosh(): DoubleTensor = tensor.map(::cosh)

    override fun Tensor<Double>.acosh(): DoubleTensor = tensor.map(::acosh)

    override fun Tensor<Double>.sin(): DoubleTensor = tensor.map(::sin)

    override fun Tensor<Double>.asin(): DoubleTensor = tensor.map(::asin)

    override fun Tensor<Double>.sinh(): DoubleTensor = tensor.map(::sinh)

    override fun Tensor<Double>.asinh(): DoubleTensor = tensor.map(::asinh)

    override fun Tensor<Double>.tan(): DoubleTensor = tensor.map(::tan)

    override fun Tensor<Double>.atan(): DoubleTensor = tensor.map(::atan)

    override fun Tensor<Double>.tanh(): DoubleTensor = tensor.map(::tanh)

    override fun Tensor<Double>.atanh(): DoubleTensor = tensor.map(::atanh)

    override fun Tensor<Double>.ceil(): DoubleTensor = tensor.map(::ceil)

    override fun Tensor<Double>.floor(): DoubleTensor = tensor.map(::floor)

    override fun Tensor<Double>.inv(): DoubleTensor = invLU(1e-9)

    override fun Tensor<Double>.det(): DoubleTensor = detLU(1e-9)

    /**
     * Computes the LU factorization of a matrix or batches of matrices `input`.
     * Returns a tuple containing the LU factorization and pivots of `input`.
     *
     * @param epsilon permissible error when comparing the determinant of a matrix with zero
     * @return pair of `factorization` and `pivots`.
     * The `factorization` has the shape ``(*, m, n)``, where``(*, m, n)`` is the shape of the `input` tensor.
     * The `pivots`  has the shape ``(∗, min(m, n))``. `pivots` stores all the intermediate transpositions of rows.
     */
    public fun Tensor<Double>.luFactor(epsilon: Double): Pair<DoubleTensor, IntTensor> =
        computeLU(tensor, epsilon)
            ?: throw IllegalArgumentException("Tensor contains matrices which are singular at precision $epsilon")

    /**
     * Computes the LU factorization of a matrix or batches of matrices `input`.
     * Returns a tuple containing the LU factorization and pivots of `input`.
     * Uses an error of ``1e-9`` when calculating whether a matrix is degenerate.
     *
     * @return pair of `factorization` and `pivots`.
     * The `factorization` has the shape ``(*, m, n)``, where``(*, m, n)`` is the shape of the `input` tensor.
     * The `pivots`  has the shape ``(∗, min(m, n))``. `pivots` stores all the intermediate transpositions of rows.
     */
    public fun Tensor<Double>.luFactor(): Pair<DoubleTensor, IntTensor> = luFactor(1e-9)

    /**
     * Unpacks the data and pivots from a LU factorization of a tensor.
     * Given a tensor [luTensor], return tensors (P, L, U) satisfying ``P * luTensor = L * U``,
     * with `P` being a permutation matrix or batch of matrices,
     * `L` being a lower triangular matrix or batch of matrices,
     * `U` being an upper triangular matrix or batch of matrices.
     *
     * @param luTensor the packed LU factorization data
     * @param pivotsTensor the packed LU factorization pivots
     * @return triple of P, L and U tensors
     */
    public fun luPivot(
        luTensor: Tensor<Double>,
        pivotsTensor: Tensor<Int>,
    ): Triple<DoubleTensor, DoubleTensor, DoubleTensor> {
        checkSquareMatrix(luTensor.shape)
        check(
            luTensor.shape.dropLast(2).toIntArray() contentEquals pivotsTensor.shape.dropLast(1).toIntArray() ||
                    luTensor.shape.last() == pivotsTensor.shape.last() - 1
        ) { "Inappropriate shapes of input tensors" }

        val n = luTensor.shape.last()
        val pTensor = luTensor.zeroesLike()
        pTensor
            .matrixSequence()
            .zip(pivotsTensor.tensor.vectorSequence())
            .forEach { (p, pivot) -> pivInit(p.as2D(), pivot.as1D(), n) }

        val lTensor = luTensor.zeroesLike()
        val uTensor = luTensor.zeroesLike()

        lTensor.matrixSequence()
            .zip(uTensor.matrixSequence())
            .zip(luTensor.tensor.matrixSequence())
            .forEach { (pairLU, lu) ->
                val (l, u) = pairLU
                luPivotHelper(l.as2D(), u.as2D(), lu.as2D(), n)
            }

        return Triple(pTensor, lTensor, uTensor)
    }

    /**
     * QR decomposition.
     *
     * Computes the QR decomposition of a matrix or a batch of matrices, and returns a pair `(Q, R)` of tensors.
     * Given a tensor `input`, return tensors (Q, R) satisfying ``input = Q * R``,
     * with `Q` being an orthogonal matrix or batch of orthogonal matrices
     * and `R` being an upper triangular matrix or batch of upper triangular matrices.
     *
     * @param epsilon permissible error when comparing tensors for equality.
     * Used when checking the positive definiteness of the input matrix or matrices.
     * @return pair of Q and R tensors.
     */
    public fun Tensor<Double>.cholesky(epsilon: Double): DoubleTensor {
        checkSquareMatrix(shape)
        checkPositiveDefinite(tensor, epsilon)

        val n = shape.last()
        val lTensor = zeroesLike()

        for ((a, l) in tensor.matrixSequence().zip(lTensor.matrixSequence()))
            for (i in 0 until n) choleskyHelper(a.as2D(), l.as2D(), n)

        return lTensor
    }

    override fun Tensor<Double>.cholesky(): DoubleTensor = cholesky(1e-6)

    override fun Tensor<Double>.qr(): Pair<DoubleTensor, DoubleTensor> {
        checkSquareMatrix(shape)
        val qTensor = zeroesLike()
        val rTensor = zeroesLike()
        tensor.matrixSequence()
            .zip(
                (qTensor.matrixSequence()
                    .zip(rTensor.matrixSequence()))
            ).forEach { (matrix, qr) ->
                val (q, r) = qr
                qrHelper(matrix.asTensor(), q.asTensor(), r.as2D())
            }

        return qTensor to rTensor
    }

    override fun Tensor<Double>.svd(): Triple<DoubleTensor, DoubleTensor, DoubleTensor> =
        svd(epsilon = 1e-10)

    /**
     * Singular Value Decomposition.
     *
     * Computes the singular value decomposition of either a matrix or batch of matrices `input`.
     * The singular value decomposition is represented as a triple `(U, S, V)`,
     * such that ``input = U.dot(diagonalEmbedding(S).dot(V.T))``.
     * If input is a batch of tensors, then U, S, and Vh are also batched with the same batch dimensions as input.
     *
     * @param epsilon permissible error when calculating the dot product of vectors,
     * i.e. the precision with which the cosine approaches 1 in an iterative algorithm.
     * @return triple `(U, S, V)`.
     */
    public fun Tensor<Double>.svd(epsilon: Double): Triple<DoubleTensor, DoubleTensor, DoubleTensor> {
        val size = tensor.dimension
        val commonShape = tensor.shape.sliceArray(0 until size - 2)
        val (n, m) = tensor.shape.sliceArray(size - 2 until size)
        val uTensor = zeros(commonShape + intArrayOf(min(n, m), n))
        val sTensor = zeros(commonShape + intArrayOf(min(n, m)))
        val vTensor = zeros(commonShape + intArrayOf(min(n, m), m))

        tensor.matrixSequence()
            .zip(
                uTensor.matrixSequence()
                    .zip(
                        sTensor.vectorSequence()
                            .zip(vTensor.matrixSequence())
                    )
            ).forEach { (matrix, USV) ->
                val matrixSize = matrix.shape.reduce { acc, i -> acc * i }
                val curMatrix = DoubleTensor(
                    matrix.shape,
                    matrix.mutableBuffer.array().slice(matrix.bufferStart until matrix.bufferStart + matrixSize)
                        .toDoubleArray()
                )
                svdHelper(curMatrix, USV, m, n, epsilon)
            }

        return Triple(uTensor.transpose(), sTensor, vTensor.transpose())
    }

    override fun Tensor<Double>.symEig(): Pair<DoubleTensor, DoubleTensor> =
        symEig(epsilon = 1e-15)

    /**
     * Returns eigenvalues and eigenvectors of a real symmetric matrix input or a batch of real symmetric matrices,
     * represented by a pair (eigenvalues, eigenvectors).
     *
     * @param epsilon permissible error when comparing tensors for equality
     * and when the cosine approaches 1 in the SVD algorithm.
     * @return a pair (eigenvalues, eigenvectors)
     */
    public fun Tensor<Double>.symEig(epsilon: Double): Pair<DoubleTensor, DoubleTensor> {
        checkSymmetric(tensor, epsilon)
        val (u, s, v) = tensor.svd(epsilon)
        val shp = s.shape + intArrayOf(1)
        val utv = u.transpose() dot v
        val n = s.shape.last()
        for (matrix in utv.matrixSequence())
            cleanSymHelper(matrix.as2D(), n)

        val eig = (utv dot s.view(shp)).view(s.shape)
        return eig to v
    }

    /**
     * Computes the determinant of a square matrix input, or of each square matrix in a batched input
     * using LU factorization algorithm.
     *
     * @param epsilon error in the LU algorithm - permissible error when comparing the determinant of a matrix with zero
     * @return the determinant.
     */
    public fun Tensor<Double>.detLU(epsilon: Double = 1e-9): DoubleTensor {

        checkSquareMatrix(tensor.shape)
        val luTensor = tensor.copy()
        val pivotsTensor = tensor.setUpPivots()

        val n = shape.size

        val detTensorShape = IntArray(n - 1) { i -> shape[i] }
        detTensorShape[n - 2] = 1
        val resBuffer = DoubleArray(detTensorShape.reduce(Int::times)) { 0.0 }

        val detTensor = DoubleTensor(
            detTensorShape,
            resBuffer
        )

        luTensor.matrixSequence().zip(pivotsTensor.vectorSequence()).forEachIndexed { index, (lu, pivots) ->
            resBuffer[index] = if (luHelper(lu.as2D(), pivots.as1D(), epsilon))
                0.0 else luMatrixDet(lu.as2D(), pivots.as1D())
        }

        return detTensor
    }

    /**
     * Computes the multiplicative inverse matrix of a square matrix input, or of each square matrix in a batched input
     * using LU factorization algorithm.
     * Given a square matrix `a`, return the matrix `aInv` satisfying
     * ``a.dot(aInv) = aInv.dot(a) = eye(a.shape[0])``.
     *
     * @param epsilon error in the LU algorithm - permissible error when comparing the determinant of a matrix with zero
     * @return the multiplicative inverse of a matrix.
     */
    public fun Tensor<Double>.invLU(epsilon: Double = 1e-9): DoubleTensor {
        val (luTensor, pivotsTensor) = luFactor(epsilon)
        val invTensor = luTensor.zeroesLike()

        val seq = luTensor.matrixSequence().zip(pivotsTensor.vectorSequence()).zip(invTensor.matrixSequence())
        for ((luP, invMatrix) in seq) {
            val (lu, pivots) = luP
            luMatrixInv(lu.as2D(), pivots.as1D(), invMatrix.as2D())
        }

        return invTensor
    }

    /**
     * LUP decomposition
     *
     * Computes the LUP decomposition of a matrix or a batch of matrices.
     * Given a tensor `input`, return tensors (P, L, U) satisfying ``P * input = L * U``,
     * with `P` being a permutation matrix or batch of matrices,
     * `L` being a lower triangular matrix or batch of matrices,
     * `U` being an upper triangular matrix or batch of matrices.
     *
     * @param epsilon permissible error when comparing the determinant of a matrix with zero
     * @return triple of P, L and U tensors
     */
    public fun Tensor<Double>.lu(epsilon: Double = 1e-9): Triple<DoubleTensor, DoubleTensor, DoubleTensor> {
        val (lu, pivots) = tensor.luFactor(epsilon)
        return luPivot(lu, pivots)
    }

    override fun Tensor<Double>.lu(): Triple<DoubleTensor, DoubleTensor, DoubleTensor> = lu(1e-9)
}


