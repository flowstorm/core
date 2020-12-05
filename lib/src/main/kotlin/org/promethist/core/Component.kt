package org.promethist.core

interface Component {
    fun process(context: Context): Context
}