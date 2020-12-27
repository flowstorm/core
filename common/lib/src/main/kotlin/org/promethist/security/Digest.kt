package org.promethist.security

import java.security.MessageDigest
import java.util.*

object Digest {

    private val MD5 = MessageDigest.getInstance("MD5")
    private val SHA1 = MessageDigest.getInstance("SHA-1")
    private val random = Random()

    fun digest(digest: MessageDigest, input: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val bytes = digest.digest(input)
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString().toLowerCase()
    }

    fun md5(input: ByteArray) = digest(MD5, input)
    fun md5() = md5(random.nextLong().toString().toByteArray())
    fun sha1(input: ByteArray) = digest(SHA1, input)
    fun sha1() = sha1(random.nextLong().toString().toByteArray())
}