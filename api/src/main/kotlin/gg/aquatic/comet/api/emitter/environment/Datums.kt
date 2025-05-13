package gg.aquatic.comet.api.emitter.environment

import org.joml.Vector3d
import org.joml.Vector3f
import java.awt.Color

val classDatumMap = mutableMapOf<Class<*>, DatumParser<*>>(
    Boolean::class.java to object : DatumParser<Boolean> {
        override fun parse(value: Boolean): Datum<Boolean> {
            return DatumBool(value)
        }
    },
    String::class.java to object : DatumParser<String> {
        override fun parse(value: String): Datum<String> {
            return DatumStr(value)
        }
    },
    Number::class.java to object : DatumParser<Number> {
        override fun parse(value: Number): Datum<Number> {
            return DatumNum(value)
        }
    },
    Color::class.java to object : DatumParser<Color> {
        override fun parse(value: Color): Datum<Color> {
            return DatumColor(value)
        }
    },
    Vector3d::class.java to object : DatumParser<Vector3d> {
        override fun parse(value: Vector3d): Datum<Vector3d> {
            return DatumVector3d(value)
        }
    },
    Vector3f::class.java to object : DatumParser<Vector3f> {
        override fun parse(value: Vector3f): Datum<Vector3f> {
            return DatumVector3f(value)
        }
    },
)

inline fun <reified T: Any> T.tryAsDatum(): Datum<T>? {
    val parser = (classDatumMap[T::class.java] ?: return null) as DatumParser<T>
    return parser.parse(this)
}

interface Datum<V> {
    val value: V
    fun clone(): Datum<V>
}

interface DatumParser<V> {
    fun parse(value: V): Datum<V>?
}

class DatumColor(
    override val value: Color
) : Datum<Color> {
    override fun clone(): DatumColor {
        return DatumColor(Color(value.red, value.green, value.blue, value.alpha))
    }
}

class DatumStr(
    override val value: String
) : Datum<String> {
    override fun clone(): DatumStr {
        return DatumStr(value)
    }
}

class DatumNum(
    override val value: Number
) : Datum<Number> {
    override fun clone(): DatumNum {
        return DatumNum(value)
    }
}

class DatumBool(
    override val value: Boolean
) : Datum<Boolean> {
    override fun clone(): DatumBool {
        return DatumBool(value)
    }
}

class DatumVector3d(
    override val value: Vector3d
) : Datum<Vector3d> {
    override fun clone(): Datum<Vector3d> {
        return DatumVector3d(value)
    }
}

class DatumVector3f(
    override val value: Vector3f
) : Datum<Vector3f> {
    override fun clone(): Datum<Vector3f> {
        return DatumVector3f(value)
    }
}
