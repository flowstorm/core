package com.promethist.core.nlp

interface Component {
    fun process(context: Context): Context
}