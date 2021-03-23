package ai.flowstorm.security

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object X509TrustAllManager : X509TrustManager {

    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}

    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}

    override fun getAcceptedIssuers(): Array<X509Certificate?>? = arrayOf()

    val sslSocketFactory: SSLSocketFactory by lazy {
        SSLContext.getInstance("SSL").run {
            init(null, arrayOf<TrustManager>(X509TrustAllManager), SecureRandom())
            socketFactory
        }
    }
}