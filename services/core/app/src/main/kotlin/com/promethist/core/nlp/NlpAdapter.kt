package com.promethist.core.nlp

import com.promethist.core.Context

interface NlpAdapter {
    fun process(context: Context): Context
}