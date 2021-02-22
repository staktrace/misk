package misk.grpc

import com.google.inject.Provides
import grpc.reflection.v1alpha.GrpcServerReflectionClient
import grpc.reflection.v1alpha.ServerReflectionClient
import javax.inject.Named
import javax.inject.Singleton
import misk.client.GrpcClientModule
import misk.client.HttpClientConfig
import misk.client.HttpClientEndpointConfig
import misk.client.HttpClientSSLConfig
import misk.client.HttpClientsConfig
import misk.grpc.reflect.GrpcReflectModule
import misk.inject.KAbstractModule
import misk.security.ssl.SslLoader
import misk.security.ssl.TrustStoreConfig
import misk.web.WebTestingModule
import misk.web.jetty.JettyService
import okhttp3.HttpUrl

class GrpcReflectTestingModule : KAbstractModule() {
  override fun configure() {
    install(WebTestingModule(webConfig = WebTestingModule.TESTING_WEB_CONFIG.copy(http2 = true)))
    install(GrpcClientModule.create<ServerReflectionClient, GrpcServerReflectionClient>("default"))
    install(GrpcReflectModule())
  }

  @Provides
  @Named("grpc server")
  fun provideServerUrl(jetty: JettyService) = jetty.httpsServerUrl!!

  @Provides
  @Singleton
  fun provideHttpClientsConfig(@Named("grpc server") url: HttpUrl): HttpClientsConfig {
    return HttpClientsConfig(
      endpoints = mapOf(
        "default" to HttpClientEndpointConfig(
          url = url.toString(),
          clientConfig = HttpClientConfig(
            ssl = HttpClientSSLConfig(
              cert_store = null,
              trust_store = TrustStoreConfig(
                resource = "classpath:/ssl/server_cert.pem",
                format = SslLoader.FORMAT_PEM
              )
            )
          )
        )
      )
    )
  }
}
