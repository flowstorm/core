package org.promethist.common.model

import java.util.*

interface TimeEntity<T: Any> : Entity<T> {
    var datetime: Date
}