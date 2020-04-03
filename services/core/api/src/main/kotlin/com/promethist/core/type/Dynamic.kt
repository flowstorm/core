package com.promethist.core.type

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.LinkedHashMap
import kotlin.reflect.KClass

class Dynamic : LinkedHashMap<String, Any> {

    constructor() : super()
    constructor(dynamic: Dynamic) : super(dynamic)
    constructor(map: Map<String, Any>) { putAll(map) }

    override fun put(key: String, value: Any): Any? {
        return if (value is Map<*, *>) {
            super.put(key, Dynamic(value as Map<String, Any>))
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
            if (pos2 < 0)
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

    fun <V: Any> put(key: String, clazz: KClass<*>, eval: (Value<V>.() -> Any)): Any {
        val triple = item(key)
        var any = triple.third?:when (clazz) {
            Int::class -> 0
            Long::class -> 0L
            Double::class -> 0.0
            String::class -> ""
            Boolean::class -> false
            MutableSet::class -> mutableSetOf<V>()
            MutableList::class -> mutableListOf<V>()
            TimeInt::class -> TimeInt(0)
            TimeString::class -> TimeString("")
            TimeBoolean::class -> TimeBoolean(false)
            LocalDate::class -> LocalDate.now()
            LocalDateTime::class -> LocalDateTime.now()
            else -> error("unsupported $clazz")
        }
        return if (any is Value<*>) {
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
            put(key, V::class, eval)

    inline fun <reified V> set(key: String, noinline eval: (Value<MutableSet<V>>.() -> Any)): Any =
            put(key, MutableSet::class, eval)

    inline fun <reified V> list(key: String, noinline eval: (Value<MutableList<V>>.() -> Any)): Any =
            put(key, MutableList::class, eval)

    operator fun invoke(key: String): Any = item(key).third?:error("missing item $key")

    fun <V> set(key: String): MutableSet<V> = invoke(key) as MutableSet<V>

    fun <V> list(key: String): MutableList<V> = invoke(key) as MutableList<V>
}