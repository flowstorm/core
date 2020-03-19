package com.promethist.core.nlp

import com.promethist.core.Context

interface Component {
    fun process(context: Context): Context
}