package com.promethist.core.type

class Attributes : LinkedHashMap<String, Attributes.Namespace>() {

    class Namespace : LinkedHashMap<String, Memorable>() {
        fun set(key: String, value: Any) =
                if (containsKey(key) && get(key) is Memory<*>)
                    (get(key)!! as Memory<Any>).apply {
                        if (_type != value::class.simpleName)
                            error("Attribute $key type cannot be changed from $_type to ${value::class.simpleName}")
                        this.value = value
                    }
                else
                    put(key, if (value is Memorable) value else Memory(value))
    }

    override operator fun get(key: String): Namespace {
        if (!containsKey(key))
            put(key, Namespace())
        return super.get(key)!!
    }
}