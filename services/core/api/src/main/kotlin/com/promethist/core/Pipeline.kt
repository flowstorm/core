package com.promethist.core

import java.util.*

interface Pipeline {

    val components: LinkedList<Component>

    fun process(context: Context): Context
}