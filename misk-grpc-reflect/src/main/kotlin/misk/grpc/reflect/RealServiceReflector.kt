package misk.grpc.reflect

import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse

class RealServiceReflector(
  val services: List<Any>
) : ServiceReflector {
  override fun process(request: ServerReflectionRequest): ServerReflectionResponse {
    TODO()
  }
}
