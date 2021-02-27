package ai.flowstorm.core.model

import org.litote.kmongo.Id

class NullId<T> : Id<T> {
    override fun toString(): String = "NULL"
}