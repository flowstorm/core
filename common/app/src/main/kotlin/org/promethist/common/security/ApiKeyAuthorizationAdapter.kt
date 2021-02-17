package org.promethist.common.security

import org.promethist.common.AppConfig
import org.promethist.common.security.AuthorizationAdapter.AuthorizationFailed

class ApiKeyAuthorizationAdapter : AbstractTokenAdapter() {
    override fun authorize() {
        if (token != AppConfig.instance["security.apiKey"]) {
            throw AuthorizationFailed("Wrong api key")
        }
    }
}