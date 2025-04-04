package misk.web.actions

import com.squareup.protos.test.grpc.GreetResponse
import com.squareup.protos.test.grpc.GreeterGreetBlockingServer
import com.squareup.protos.test.parsing.Shipment
import com.squareup.protos.test.parsing.Warehouse
import com.squareup.wire.Service
import com.squareup.wire.WireRpc
import jakarta.inject.Inject
import misk.MiskCaller
import misk.MiskTestingServiceModule
import misk.inject.KAbstractModule
import misk.logging.LogCollectorModule
import misk.scope.ActionScoped
import misk.security.authz.AccessAnnotationEntry
import misk.security.authz.AccessControlModule
import misk.security.authz.Authenticated
import misk.security.authz.ExcludeFromAllowAnyService
import misk.security.authz.FakeCallerAuthenticator
import misk.security.authz.MiskCallerAuthenticator
import misk.security.authz.Unauthenticated
import misk.web.Get
import misk.web.Post
import misk.web.RequestBody
import misk.web.RequestContentType
import misk.web.ResponseContentType
import misk.web.WebActionModule
import misk.web.WebServerTestingModule
import misk.web.interceptors.LogRequestResponse
import misk.web.mediatype.MediaTypes
import misk.web.toResponseBody

// Common module for web action-related tests to use that bind up some sample web actions
class TestWebActionModule : KAbstractModule() {
  override fun configure() {
    install(WebServerTestingModule())
    install(MiskTestingServiceModule())
    install(AccessControlModule())
    install(LogCollectorModule())

    install(WebActionModule.create<CustomServiceAccessAction>())
    install(WebActionModule.create<CustomCapabilityAccessAction>())
    install(WebActionModule.create<RequestTypeAction>())
    install(WebActionModule.create<GrpcAction>())
    install(WebActionModule.create<GreetServiceWebAction>())
    install(WebActionModule.create<EmptyAuthenticatedAccessAction>())
    install(WebActionModule.create<EmptyAuthenticatedWithCustomAnnototationAccessAction>())
    install(WebActionModule.create<AllowAnyServiceAccessAction>())
    install(WebActionModule.create<AllowAnyServicePlusAuthenticatedAccessAction>())
    install(WebActionModule.create<AllowAnyUserAccessAction>())
    install(WebActionModule.create<AuthenticatedServiceWithCustomAnnotations>())

    multibind<AccessAnnotationEntry>().toInstance(
      AccessAnnotationEntry<CustomServiceAccess>(services = listOf("payments"))
    )
    multibind<AccessAnnotationEntry>().toInstance(
      AccessAnnotationEntry<CustomServiceAccess2>(services = listOf("some_other_service"))
    )
    multibind<AccessAnnotationEntry>().toInstance(
      AccessAnnotationEntry<CustomCapabilityAccess>(capabilities = listOf("admin"))
    )
    multibind<AccessAnnotationEntry>().toInstance(
      AccessAnnotationEntry<CustomCapabilityAccess2>(capabilities = listOf("some_other_group"))
    )
    multibind<MiskCallerAuthenticator>().to<FakeCallerAuthenticator>()

    multibind<String, ExcludeFromAllowAnyService>().toInstance("web-proxy")
    multibind<String, ExcludeFromAllowAnyService>().toInstance("access-proxy")
  }

  // TODO(jwilson): get Wire to generate this interface.
  interface ShippingGetDestinationWarehouseBlockingServer : Service {
    @WireRpc(
      path = "/test/GetDestinationWarehouse",
      requestAdapter = "com.squareup.protos.test.parsing.Shipment#ADAPTER",
      responseAdapter = "com.squareup.protos.test.parsing.Warehouse#ADAPTER"
    )
    fun GetDestinationWarehouse(requestType: Shipment): Warehouse
  }
}

class CustomServiceAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/custom_service_access")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @CustomServiceAccess
  @CustomServiceAccess2
  fun get() = "${scopedCaller.get()} authorized as custom service".toResponseBody()
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CustomServiceAccess

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CustomServiceAccess2

class CustomCapabilityAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/custom_capability_access")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @CustomCapabilityAccess
  @CustomCapabilityAccess2
  fun get() = "${scopedCaller.get()} authorized with custom capability".toResponseBody()
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CustomCapabilityAccess

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CustomCapabilityAccess2

class RequestTypeAction @Inject constructor() : WebAction {
  @Post("/request_type")
  @RequestContentType(MediaTypes.APPLICATION_JSON)
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @Unauthenticated
  fun shipment(@RequestBody requestType: Shipment) = "request: $requestType".toResponseBody()
}

class GreetServiceWebAction @Inject constructor() : WebAction, GreeterGreetBlockingServer {
  @Unauthenticated
  @LogRequestResponse
  override fun Greet(request: Unit): GreetResponse {
    return GreetResponse.Builder()
      .message("Hola")
      .build()
  }
}

class GrpcAction @Inject constructor() :
  TestWebActionModule.ShippingGetDestinationWarehouseBlockingServer,
  WebAction {
  @Unauthenticated
  override fun GetDestinationWarehouse(requestType: Shipment): Warehouse {
    return Warehouse.Builder()
      .warehouse_id(7777L)
      .build()
  }
}

class EmptyAuthenticatedAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/empty_authorized_access")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @Authenticated
  fun get() = "${scopedCaller.get()} authorized with empty Authenticated".toResponseBody()
}

class EmptyAuthenticatedWithCustomAnnototationAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/empty_authorized_and_custom_capability_access")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @Authenticated
  @CustomCapabilityAccess
  fun get() = "${scopedCaller.get()} authorized with CustomCapabilityAccess".toResponseBody()
}

class AuthenticatedServiceWithCustomAnnotations @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/auth-and-custom-capability")
  @Authenticated(services = ["dingo"])
  @CustomCapabilityAccess
  fun get() = "${scopedCaller.get()} authorized with custom capability".toResponseBody()
}

class AllowAnyServiceAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/allow_any_service_access")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @Authenticated(allowAnyService = true)
  fun get() = "${scopedCaller.get()} authorized as any service".toResponseBody()
}

class AllowAnyServicePlusAuthenticatedAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/allow_any_service_plus_authenticated")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @Authenticated(services = ["web-proxy"], capabilities = ["admin"], allowAnyService = true)
  fun get() = "${scopedCaller.get()} authorized as any service".toResponseBody()
}

class AllowAnyUserAccessAction @Inject constructor() : WebAction {
  @Inject
  lateinit var scopedCaller: ActionScoped<MiskCaller?>

  @Get("/allow_any_user_access")
  @ResponseContentType(MediaTypes.TEXT_PLAIN_UTF8)
  @Authenticated(allowAnyUser = true)
  fun get() = "${scopedCaller.get()} authorized as any user".toResponseBody()
}
