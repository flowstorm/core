package org.promethist.common.security

class NoAuthorizationAdapter : AuthorizationAdapter {
    override fun authorize() {
        //do nothing - always authorize access
    }
}