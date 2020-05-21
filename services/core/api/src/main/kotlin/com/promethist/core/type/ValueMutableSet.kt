package com.promethist.core.type

open class ValueMutableSet<V : Any>(col: Collection<Value<V>>) : HashSet<Value<V>>(col) {

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