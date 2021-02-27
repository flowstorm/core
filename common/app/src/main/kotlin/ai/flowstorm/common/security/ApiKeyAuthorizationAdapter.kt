package ai.flowstorm.common.security

import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.security.AuthorizationAdapter.AuthorizationFailed

class ApiKeyAuthorizationAdapter : AbstractTokenAdapter() {
    override fun authorize() {
        if (token != AppConfig.instance["security.apiKey"]) {
            throw AuthorizationFailed("Wrong api key")
        }
    }
}