package ai.flowstorm.common.security

import ai.flowstorm.common.config.ConfigValue
import ai.flowstorm.common.security.AuthorizationAdapter.AuthorizationFailed

class ApiKeyAuthorizationAdapter(@ConfigValue("security.apiKey") private val apiKey: String) : AbstractTokenAdapter() {
    override fun authorize() {
        if (token != apiKey) {
            throw AuthorizationFailed("Wrong api key")
        }
    }
}