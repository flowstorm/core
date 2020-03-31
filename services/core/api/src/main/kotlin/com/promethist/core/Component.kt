package com.promethist.core

interface Component {
    fun process(context: Context): Context
}