package org.promethist.core.resources

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.*
import org.promethist.common.security.Authorized
import org.promethist.core.model.DevicePairing
import java.util.*
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/devicePairing")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class DevicePairingResourceImpl : DevicePairingResource {

    @Inject
    lateinit var database: MongoDatabase

    private val devicePairings by lazy { database.getCollection<DevicePairing>() }

    override fun pairDevice(pairingCode: String): DevicePairing =
            devicePairings.find(and(
                    DevicePairing::pairingCode eq pairingCode,
                    DevicePairing::date gte Date(System.currentTimeMillis() - 1000 * 60 * 10)))
                        .singleOrNull()?.apply {
                devicePairings.deleteOneById(_id)
            } ?: throw NotFoundException("Device pairing not found")

    override fun getDevicePairing(deviceId: String): DevicePairing? {
        return devicePairings.find(DevicePairing::deviceId eq deviceId).singleOrNull()
    }

    override fun createOrUpdateDevicePairing(devicePairing: DevicePairing) {
        getDevicePairing(devicePairing.deviceId).let {
            if (it != null) {
                devicePairings.updateOne(DevicePairing::_id eq it._id, devicePairing)
            } else {
                devicePairings.insertOne(devicePairing)
            }
        }
    }
}