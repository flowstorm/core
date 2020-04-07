package com.promethist.core

import java.time.LocalDate
import java.time.LocalDateTime

open class DialogueScript {
    val now get() = LocalDateTime.now()
    val today get() = LocalDate.now()
}