package com.promethist.core.type

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
open class Dynamic : LinkedHashMap<String, Any>, MutablePropertyMap {

    constructor() : super()
    constructor(dynamic: Dynamic) : super(dynamic)
    constructor(map: Map<String, Any?>) { putAll(map) }
    constructor(vararg pairs: Pair<String, Any>) { putAll(pairs) }

    data class Value<T: Any>(var value: T)

    interface Object {
        val dynamic: Dynamic
    }

    companion object {
        val EMPTY = Dynamic()

        fun <V: Any> defaultValue(clazz: KClass<*>, other: () -> Any = { error("unsupported $clazz") }): Any =
                when (clazz) {
                    Int::class -> 0
                    Long::class -> 0L
                    Float::class -> 0.0F
                    Double::class -> 0.0
                    Decimal::class -> Decimal(0)
                    String::class -> ""
                    Boolean::class -> false
                    MutableList::class -> mutableListOf<V>()
                    DateTime::class -> ZERO_TIME
                    else -> other()
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

    inline operator fun <reified T : Any> invoke(): T = proxy(T::class.java)

    fun <T: Any> proxy(type: Class<T>): T = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _: Any, method: Method, args: Array<Any>? ->
        val name = method.name
        if (name == Any::toString.name)
            "proxy:" + this@Dynamic.toString()
        else if (name == "getDynamic" && Object::class.java.isAssignableFrom(type))
            this@Dynamic
        else if (name == Any::equals.name && args?.size == 1)
            this@Dynamic == args[0]
        else if (name == Any::hashCode.name)
            this@Dynamic.hashCode()
        else if ((name.startsWith("get") || name.startsWith("is")) && (args == null || args.isEmpty())) {
            val pos = if (name.startsWith("get")) 3 else 2
            val key = name.substring(pos, pos + 1).toLowerCase() + name.substring(pos + 1)
            val kClass = method.returnType.kotlin
            put(key, kClass, { defaultValue<Any>(kClass) { Dynamic() } }) {
                when {
                    method.returnType == Dynamic.javaClass -> value
                    value is Dynamic -> (value as Dynamic).proxy(method.returnType)
                    else -> value
                }
            }
        } else if (name.startsWith("set") && args?.size == 1) {
            val key = name.substring(3, 4).toLowerCase() + name.substring(4)
            put(key, args[0])
            Unit
        } else {
            error("Dynamic proxy cannot invoke method $name (only property get/set is supported)")
        }
    } as T
}

inline fun <reified T: Any> dynamic(noinline init: (T.() -> Unit)? = null) = Dynamic().proxy(T::class.java).also {
    init?.invoke(it)
}