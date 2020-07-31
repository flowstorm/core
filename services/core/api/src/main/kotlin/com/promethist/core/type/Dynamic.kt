package com.promethist.core.type

import java.math.BigDecimal
import java.time.ZonedDateTime
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
open class Dynamic : LinkedHashMap<String, Any>, MutablePropertyMap {

    constructor() : super()
    constructor(dynamic: Dynamic) : super(dynamic)
    constructor(map: Map<String, Any?>) { putAll(map) }
    constructor(vararg pairs: Pair<String, Any>) { putAll(pairs) }

    data class Value<T: Any>(var value: T)

    companion object {
        val EMPTY = Dynamic()

        fun <V: Any> defaultValue(clazz: KClass<*>): Any =
                when (clazz) {
                    Int::class -> 0
                    Long::class -> 0L
                    Float::class -> 0.0F
                    Double::class -> 0.0
                    BigDecimal::class -> BigDecimal(0)
                    String::class -> ""
                    Boolean::class -> false
                    MutableList::class -> mutableListOf<V>()
                    ZonedDateTime::class -> ZERO_TIME
                    else -> error("unsupported $clazz")
                }
    }

    override fun put(key: String, value: Any) = put(key, value as Any?)

    @JvmName("putNullable")
    fun put(key: String, value: Any?): Any? {
        return if (value is Map<*, *> && value !is Dynamic) {
            super.put(key, Dynamic(value as PropertyMap))
        } else if (value is List<*>) {
            super.put(key, value.map { if (it is Map<*, *> && it !is Dynamic) Dynamic(it as Map<String, Any?>) else it })
        } else {
            super.put(key, value ?: EMPTY)
        }
    }

    override fun putAll(from: Map<out String, Any>) = from.forEach { put(it.key, it.value) }

    @JvmName("putAllNullable")
    fun putAll(from: Map<out String, Any?>) = from.forEach { put(it.key, it.value) }

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

        val value = Value(any as V)
        val ret = eval(value)
        triple.first[triple.second] = value.value
        return ret
    }

    inline operator fun <reified V: Any> invoke(key: String, any: V) = put<V>(key, V::class) { value = any; Unit }

    inline operator fun <reified V: Any> invoke(key: String, noinline eval: (Value<V>.() -> Any)): Any =
            put(key, V::class, null, eval)

    operator fun invoke(key: String): Any = item(key).third?:error("missing item $key")

    override fun containsKey(key: String): Boolean {
        return item(key).third != null
    }

    fun list(key: String) = invoke(key) as List<Dynamic>
}