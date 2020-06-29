package com.promethist.core.type

class Attributes : LinkedHashMap<String, Attributes.Namespace>() {

    class Namespace : LinkedHashMap<String, Memorable>() {
        fun set(key: String, value: Any) = put(key, Memory(value))
    }

    override operator fun get(key: String): Namespace {
        if (!containsKey(key))
            put(key, Namespace())
        return super.get(key)!!
    }
}