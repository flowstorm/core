package org.promethist.core.type

object Throwables {
    val Throwable.root: Throwable
        get() {
            var root = this
            while (root.cause != null)
                root = root.cause!!
            return root
        }
}