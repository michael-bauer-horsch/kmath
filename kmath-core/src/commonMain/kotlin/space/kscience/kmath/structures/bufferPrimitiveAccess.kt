package space.kscience.kmath.structures

import space.kscience.kmath.misc.UnstableKMathAPI

/**
 * Non-boxing access to primitive [Double]
 */
@OptIn(UnstableKMathAPI::class)
public fun Buffer<Double>.getDouble(index: Int): Double = if (this is BufferView) {
    val originIndex = originIndex(index)
    if( originIndex>=0) {
        origin.getDouble(originIndex)
    } else {
        get(index)
    }
} else if (this is DoubleBuffer) {
    array[index]
} else {
    get(index)
}

/**
 * Non-boxing access to primitive [Int]
 */
@OptIn(UnstableKMathAPI::class)
public fun Buffer<Int>.getInt(index: Int): Int = if (this is BufferView) {
    val originIndex = originIndex(index)
    if( originIndex>=0) {
        origin.getInt(originIndex)
    } else {
        get(index)
    }
} else if (this is IntBuffer) {
    array[index]
} else {
    get(index)
}