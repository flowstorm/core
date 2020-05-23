package com.promethist.core.type

class Attributes : LinkedHashMap<String, Attributes.Namespace>() {

    class Namespace : LinkedHashMap<String, Memorable>() {

        fun put(values: PropertyMap) {
            values.forEach {
                put(it.key, Memory(it.value))
            }
        }
    }

    override operator fun get(key: String): Namespace {
        if (!containsKey(key))
            put(key, Namespace())
        return super.get(key)!!
    }
}