package com.promethist.util

import java.util.*

class Cache(val dataLimit: Int): ArrayList<Cache.Item>() {

    data class Item(val name: String, val dataSize: Int, val data: Any, var lastAccess: Long): Comparable<Item> {
        override fun compareTo(other: Item): Int = lastAccess.compareTo(other.lastAccess)
    }

    var dataSize: Int = 0

    override fun add(element: Item): Boolean {
        sort()
        while (dataSize + element.dataSize >= dataLimit) {
            dataSize -= removeAt(0).dataSize
        }
        dataSize += element.dataSize
        return super.add(element)
    }

    fun get(name: String): Item? = find {
        if (it.name == name) {
            it.lastAccess = System.currentTimeMillis()
            true
        } else {
            false
        }
    }
}