package ai.flowstorm.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*
import kotlin.math.abs

data class DevicePairing(
        val _id: Id<DevicePairing> = newId(),
        val deviceId: String,
        val pairingCode: String = (abs(deviceId.hashCode()).rem(90000) + 10000).toString(),
        val date: Date = Date()
)