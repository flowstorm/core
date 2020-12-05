package org.promethist.core.model

import org.litote.kmongo.Id

interface Entity<T: Any> {
    val _id: Id<T>
}