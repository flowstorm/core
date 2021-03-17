package ai.flowstorm.core.binding

import ai.flowstorm.common.config.Config
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.glassfish.hk2.api.Factory
import javax.inject.Inject

class ElasticClientFactory : Factory<RestHighLevelClient> {

    @Inject
    lateinit var config: Config

    override fun provide(): RestHighLevelClient {
        return with(config) {
            RestHighLevelClient(
                RestClient.builder(
                    HttpHost(
                        get("es.host"),
                        (getOrNull("es.port") ?: "9243").toInt(),
                        getOrNull("es.scheme") ?: "https"
                    )
                ).apply {
                    getOrNull("es.user")?.let { user ->
                        setHttpClientConfigCallback {
                            it.setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                                setCredentials(
                                    AuthScope.ANY,
                                    UsernamePasswordCredentials(user, get("es.password", ""))
                                )
                            })
                        }
                    }
                }
            )
        }
    }

    override fun dispose(instance: RestHighLevelClient) {}
}