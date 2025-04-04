package misk.client

import com.google.inject.Key
import com.google.inject.Provider
import com.squareup.moshi.Moshi
import misk.inject.KAbstractModule
import misk.inject.asSingleton
import okhttp3.OkHttpClient
import jakarta.inject.Inject

/** Provides an [OkHttpClient] and [ProtoMessageHttpClient] for a peer service */
class HttpClientModule @JvmOverloads constructor(
  private val name: String,
  private val annotation: Annotation? = null
) : KAbstractModule() {
  override fun configure() {
    val httpClientKey =
      if (annotation == null) Key.get(OkHttpClient::class.java)
      else Key.get(OkHttpClient::class.java, annotation)
    val protoMessageHttpClientKey =
      if (annotation == null) Key.get(ProtoMessageHttpClient::class.java)
      else Key.get(ProtoMessageHttpClient::class.java, annotation)
    bind(httpClientKey)
      .toProvider(HttpClientProvider(name))
      .asSingleton()
    bind(protoMessageHttpClientKey)
      .toProvider(ProtoMessageHttpClientProvider(name, getProvider(httpClientKey)))
      .asSingleton()

    // Use requestInjection() to check that our configuration exists when the injector is created.
    requestInjection(object : Any() {
      @Inject fun injectHttpClientsConfig(httpClientsConfig: HttpClientsConfig) {
        require(name in httpClientsConfig.endpointNames()) {
          "No HTTP endpoint configured for '$name'... update your yaml to include it?"
        }
      }
    })
  }

  private class HttpClientProvider(private val name: String) : Provider<OkHttpClient> {
    /** Use a provider because we don't know the test client's URL until its test server starts. */
    @Inject lateinit var httpClientsConfigProvider: Provider<HttpClientsConfig>
    @Inject lateinit var httpClientFactory: HttpClientFactory

    override fun get() = httpClientFactory.create(httpClientsConfigProvider.get()[name], name)
  }

  private class ProtoMessageHttpClientProvider(
    private val name: String,
    private val httpClientProvider: Provider<OkHttpClient>
  ) : Provider<ProtoMessageHttpClient> {
    @Inject lateinit var moshi: Moshi

    /** Use a provider because we don't know the test client's URL until its test server starts. */
    @Inject lateinit var httpClientsConfigProvider: Provider<HttpClientsConfig>
    @Inject lateinit var httpClientConfigUrlProvider: HttpClientConfigUrlProvider

    override fun get(): ProtoMessageHttpClient {
      val endpointConfig = httpClientsConfigProvider.get()[name]
      val httpClient = httpClientProvider.get()

      return ProtoMessageHttpClient(
        httpClientConfigUrlProvider.getUrl(endpointConfig), moshi, httpClient
      )
    }
  }
}
