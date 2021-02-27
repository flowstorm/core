package ai.flowstorm.core.type

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import kotlin.reflect.jvm.jvmName

class MemoryTypeIdResolver : TypeIdResolverBase() {

    private var superType: JavaType? = null

    override fun init(baseType: JavaType?) {
        superType = baseType
    }

    override fun idFromValue(value: Any): String {
        val innerPattern = "^model\\.[a-z0-9]+\\.Model".toRegex()
        return classToName[value::class] ?: value::class.jvmName.replace(innerPattern, "dialogueScope")
    }

    override fun idFromValueAndType(value: Any?, suggestedType: Class<*>?): String? {
        if (value == null) {
            return suggestedType?.name
        }
        return idFromValue(value)
    }

    override fun typeFromId(context: DatabindContext, id: String): JavaType {
        return context.constructSpecializedType(superType, classFromId(id))
    }

    override fun getMechanism(): JsonTypeInfo.Id {
        return JsonTypeInfo.Id.CUSTOM
    }

    companion object {
        private val nameToClass = mapOf(
            "Boolean" to Boolean::class,
            "String" to String::class,
            "Int" to Int::class,
            "Long" to Long::class,
            "Float" to Float::class,
            "Double" to Double::class,
            "BigDecimal" to Decimal::class,
            "ZonedDateTime" to DateTime::class,
            "Dynamic" to Dynamic::class,
            "Location" to Location::class,

            "BooleanMutableSet" to BooleanMutableSet::class,
            "StringMutableSet" to StringMutableSet::class,
            "IntMutableSet" to IntMutableSet::class,
            "LongMutableSet" to LongMutableSet::class,
            "FloatMutableSet" to FloatMutableSet::class,
            "DoubleMutableSet" to DoubleMutableSet::class,
            "BigDecimalMutableSet" to BigDecimalMutableSet::class,
            "DateTimeMutableSet" to DateTimeMutableSet::class,
            "LocationMutableSet" to LocationMutableSet::class,
            "DynamicMutableSet" to DynamicMutableSet::class,

            "BooleanMutableList" to BooleanMutableList::class,
            "StringMutableList" to StringMutableList::class,
            "IntMutableList" to IntMutableList::class,
            "LongMutableList" to LongMutableList::class,
            "FloatMutableList" to FloatMutableList::class,
            "DoubleMutableList" to DoubleMutableList::class,
            "BigDecimalMutableList" to BigDecimalMutableList::class,
            "DateTimeMutableList" to DateTimeMutableList::class,
            "LocationMutableList" to LocationMutableList::class,
            "DynamicMutableList" to DynamicMutableList::class
        )
        private val classToName = nameToClass.entries.associate { (k, v) -> v to k }

        fun classFromId(id: String, caller: Class<out Any> = MemoryTypeIdResolver::class.java): Class<out Any> {
            return nameToClass[id]?.java ?: id.replace("^dialogueScope".toRegex(), caller.name).let {
                Class.forName(it, true, caller.classLoader)
            }
        }
    }
}