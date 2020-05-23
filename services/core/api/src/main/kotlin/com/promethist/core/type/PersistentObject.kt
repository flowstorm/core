package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes(
        JsonSubTypes.Type(value = Value::class, name = "Value"),
        JsonSubTypes.Type(value = ValueMutableSet::class, name = "ValueMutableSet"),
        JsonSubTypes.Type(value = ValueMutableList::class, name = "ValueMutableList")
)
interface PersistentObject