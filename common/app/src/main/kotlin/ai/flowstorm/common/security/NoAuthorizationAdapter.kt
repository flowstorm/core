package ai.flowstorm.common.security

class NoAuthorizationAdapter : AuthorizationAdapter {
    override fun authorize() {
        //do nothing - always authorize access
    }
}