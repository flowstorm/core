package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import ai.flowstorm.core.model.DevicePairing
import ai.flowstorm.security.Authenticated
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Device Pairing"], authorizations = [Authorization("Authorization")])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface DevicePairingResource {

    @GET
    @Path("/pair")
    fun pairDevice(@ApiParam(required = true) @QueryParam("pairingCode") pairingCode: String): DevicePairing

    fun getDevicePairing(deviceId: String): DevicePairing?

    fun createOrUpdateDevicePairing(devicePairing: DevicePairing)
}