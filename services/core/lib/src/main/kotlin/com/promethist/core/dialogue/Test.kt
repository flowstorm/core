package com.promethist.core.dialogue

import com.promethist.core.type.Dynamic
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import java.time.ZonedDateTime

object Test {

    data class D(val time: ZonedDateTime)
    @JvmStatic
    fun main(args: Array<String>) {

        //val d = Dynamic()
        val d = LinkedHashMap<String, Any>()
        d["d"] = ZonedDateTime.now()
        val s = mapper.writeValueAsString(d)
println(s)
        val d2 = mapper.readValue(s, Dynamic::class.java)

        println(d2["d"]!!::class.java)
    }
}