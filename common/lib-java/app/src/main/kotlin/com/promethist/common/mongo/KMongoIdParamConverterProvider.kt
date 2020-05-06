package com.promethist.common.mongo

import org.litote.kmongo.Id
import org.litote.kmongo.id.ObjectIdGenerator
import java.lang.reflect.Type
import javax.ws.rs.ext.ParamConverter
import javax.ws.rs.ext.ParamConverterProvider

class KMongoIdParamConverterProvider : ParamConverterProvider {
    override fun <T : Any?> getConverter(p0: Class<T>?, p1: Type?, p2: Array<out Annotation>?): ParamConverter<T>? =
            if (p0 == Id::class.java) IdParamConverter() else null

    class IdParamConverter<T> : ParamConverter<T> {
        override fun fromString(string: String): T {
            @Suppress("UNCHECKED_CAST")
            return ObjectIdGenerator.create(string) as T
        }

        override fun toString(value: T): String {
            return value.toString()
        }
    }
}