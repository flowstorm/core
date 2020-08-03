package com.promethist.client.util

import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

data class InetInterface(val networkInterface: NetworkInterface, val inetAddress: InetAddress, val hardwareAddress: String) {

    companion object {

        fun getActive(): InetInterface? {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList().sortedWith(Comparator<NetworkInterface> { ni, _ ->
                if (ni.name == "en0" || ni.name == "eth0" || ni.name == "wlan0") Int.MIN_VALUE else Int.MAX_VALUE
            })
            //println(networkInterfaces)
            for (networkInterface in networkInterfaces) {
                if (networkInterface.isLoopback)
                    continue
                if (!networkInterface.isUp)
                    continue
                if (networkInterface.isVirtual)
                    continue
                for (address in networkInterface.inetAddresses) {
                    // look only for ipv4 addresses
                    if (address is Inet6Address)
                        continue

                    // use a timeout big enough for your needs
                    if (!address.isReachable(3000))
                        continue

                    return InetInterface(networkInterface, address, networkInterface.hardwareAddress.joinToString(":") { String.format("%02X", it) })
                }
            }
            return null
        }

        @JvmStatic
        fun main(args: Array<String>) {
            println(InetInterface.Companion.getActive())
        }
    }
}