package com.promethist.common.query

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(RUNTIME)
@Target(FIELD, VALUE_PARAMETER)
annotation class QueryParams