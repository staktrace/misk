package misk.grpc.reflect

import grpc.reflection.v1alpha.ServerReflectionRequest
import org.junit.jupiter.api.Test
import routeguide.Feature
import routeguide.Point
import routeguide.RouteGuideGetFeatureBlockingServer

internal class RealServiceReflectorTest {

  @Test
  internal fun name() {
    val api = object : RouteGuideGetFeatureBlockingServer {
      override fun GetFeature(request: Point): Feature {
        error("reflection only")
      }
    }

    val reflector = RealServiceReflector(listOf(api))

    reflector.process(ServerReflectionRequest.Builder()
      .list_services("")
      .build())
  }
}
