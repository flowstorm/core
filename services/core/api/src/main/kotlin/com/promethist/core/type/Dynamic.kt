package com.promethist.core.type

import com.promethist.core.Defaults
import java.math.BigDecimal
import java.time.ZonedDateTime
import kotlin.collections.LinkedHashMap
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class Dynamic : LinkedHashMap<String, Any>, MutablePropertyMap {

    constructor() : super()
    constructor(dynamic: Dynamic) : super(dynamic)
    constructor(map: PropertyMap) { putAll(map) }

    companion object {

        val EMPTY = Dynamic()
        val ZERO_TIME = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, Defaults.zoneId)

        fun <V: Any> defaultValue(clazz: KClass<*>): Any =
                when (clazz) {
                    Int::class -> 0
                    Long::class -> 0L
                    Float::class -> 0.0F
                    Double::class -> 0.0
                    BigDecimal::class -> BigDecimal(0)
                    String::class -> ""
                    Boolean::class -> false
                    MutableSet::class -> mutableSetOf<V>()
                    MutableList::class -> mutableListOf<V>()
                    TimeInt::class -> TimeInt(0, ZERO_TIME)
                    TimeLong::class -> TimeLong(0, ZERO_TIME)
                    TimeFloat::class -> TimeFloat(0.0F, ZERO_TIME)
                    TimeDouble::class -> TimeDouble(0.0, ZERO_TIME)
                    TimeString::class -> TimeString("", ZERO_TIME)
                    TimeBoolean::class -> TimeBoolean(false, ZERO_TIME)
                    TimeBigDecimal::class -> TimeBigDecimal(BigDecimal.ZERO, ZERO_TIME)
                    ZonedDateTime::class -> ZERO_TIME
                    else -> error("unsupported $clazz")
                }
    }

    override fun put(key: String, value: Any): Any? {
        return if (value is Map<*, *> && value !is Dynamic) {
            super.put(key, Dynamic(value as PropertyMap))
        } else {
            super.put(key, value)
        }
    }

    override fun putAll(from: Map<out String, Any>) = from.forEach { put(it.key, it.value) }

    private fun item(key: String): Triple<Dynamic, String, Any?> {
        var obj = this
        var pos = 0
        while (true) {
            val pos2 = key.indexOf('.', pos)
            if (pos2 <= 0)
                break
            val name = key.substring(pos, pos2)
            var any = obj.get(name)
            if (any == null) {
                any = Dynamic()
                obj.put(name, any)
            }
            if (any !is Dynamic)
                error(key.substring(0, pos2) + " is not dynamic object")
            obj = any
            pos = pos2 + 1
        }
        val name = key.substring(pos)
        return Triple(obj, name, obj.get(name))
    }

    fun <V: Any> put(key: String, clazz: KClass<*>, default: (() -> V)? = null, eval: (Value<V>.() -> Any)): Any {
        val triple = item(key)
        val any = triple.third ?: default?.invoke() ?: defaultValue<V>(clazz)
        return if (any is Value<*> && any !is TimeValue<*>) {
            eval(any as Value<V>)
        } else {
            val value = Value(any as V)
            val ret = eval(value)
            triple.first[triple.second] = value.value
            ret
        }
    }

    inline operator fun <reified V: Any> invoke(key: String, any: V) = put<V>(key, V::class) { value = any; Unit }

    inline operator fun <reified V: Any> invoke(key: String, noinline eval: (Value<V>.() -> Any)): Any =
            put(key, V::class, null, eval)

    inline fun <reified V> set(key: String, noinline eval: (Value<MutableSet<V>>.() -> Any)): Any =
            put(key, MutableSet::class, null, eval)

    inline fun <reified V> list(key: String, noinline eval: (Value<MutableList<V>>.() -> Any)): Any =
            put(key, MutableList::class, null, eval)

    operator fun invoke(key: String): Any = item(key).third?:error("missing item $key")

    fun <V> set(key: String): MutableSet<V> = invoke(key) as MutableSet<V>

    fun <V> list(key: String): MutableList<V> = invoke(key) as MutableList<V>
}