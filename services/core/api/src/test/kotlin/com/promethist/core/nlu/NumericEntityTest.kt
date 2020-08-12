package com.promethist.core.nlu

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NumericEntityTest {
    val mapper = ObjectUtil.defaultMapper

    @Test
    fun testTimeDeserialization() {
        val res: List<NumericEntity> = mapper.readValue(this::class.java.getResourceAsStream("time.json"), object : TypeReference<List<NumericEntity>>(){})
        assertEquals(6, (res[0].structuredValue as NumericEntity.GrainedTime).value.hour)
        assertEquals("+02:00", (res[0].structuredValue as NumericEntity.GrainedTime).value.zone.id)

        assertEquals(17, (res[1].structuredValue as NumericEntity.Interval).from.value.hour)
        assertEquals(21, (res[1].structuredValue as NumericEntity.Interval).to.value.hour)
    }

    @Test
    fun testUnitDeserialization() {
        val res: List<NumericEntity> = mapper.readValue(this::class.java.getResourceAsStream("unit.json"), object : TypeReference<List<NumericEntity>>(){})
        assertEquals(2F, (res[0].structuredValue as NumericEntity.Quantity).value)
        assertEquals("coffee", (res[0].structuredValue as NumericEntity.Quantity).product)
        assertEquals("cup", (res[0].structuredValue as NumericEntity.Quantity).unit)

        assertEquals(3.5F, (res[1].structuredValue as NumericEntity.Quantity).value)
        assertEquals("cup", (res[1].structuredValue as NumericEntity.Quantity).unit)
    }
}