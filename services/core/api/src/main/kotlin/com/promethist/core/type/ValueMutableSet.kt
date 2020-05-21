package com.promethist.core.type

open class ValueMutableSet<V : Any> : LinkedHashSet<Value<V>>() {

    override fun add(e: Value<V>): Boolean {
        for (v in this) {
            if (v == e) {
                e.count++
                e.time = DateTime.now()
                return true
            }
        }
        return super.add(e)
    }
}