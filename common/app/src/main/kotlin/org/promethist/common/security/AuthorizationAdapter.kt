package org.promethist.common.security

interface AuthorizationAdapter {

    /**
     * Throws AuthorizationFailed when the client is not authorized to access resource.
     */
    fun authorize()

    class AuthorizationFailed(override val message: String? = null) : Throwable()
}