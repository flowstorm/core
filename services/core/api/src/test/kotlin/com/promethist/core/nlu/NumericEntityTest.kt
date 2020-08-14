package com.promethist.core.nlu

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NumericEntityTest {
    val mapper = ObjectUtil.defaultMapper

    private fun getInput(name: String) =
            mapper.readValue(this::class.java.getResourceAsStream(name), object : TypeReference<List<NumericEntity>>(){})

    @Test
    fun testTime() {
        val res: List<NumericEntity> = getInput("time.json")
        assertEquals(6, (res[0].structuredValue as NumericEntity.GrainedTime).value.hour)
        assertEquals("+02:00", (res[0].structuredValue as NumericEntity.GrainedTime).value.zone.id)
        assertEquals("TIME", res[0].className)

        assertEquals(17, (res[1].structuredValue as NumericEntity.Interval).from.value.hour)
        assertEquals(21, (res[1].structuredValue as NumericEntity.Interval).to.value.hour)
    }

    @Test
    fun testQuantity() {
        val res: List<NumericEntity> = getInput("quantity.json")
        assertEquals(2F, (res[0].structuredValue as NumericEntity.Quantity).value)
        assertEquals("coffee", (res[0].structuredValue as NumericEntity.Quantity).product)
        assertEquals("cup", (res[0].structuredValue as NumericEntity.Quantity).unit)
        assertEquals("QUANTITY", res[0].className)

        assertEquals(3.5F, (res[1].structuredValue as NumericEntity.Quantity).value)
        assertEquals("cup", (res[1].structuredValue as NumericEntity.Quantity).unit)
    }

    @Test
    fun testAmountOfMoney() {
        val res: List<NumericEntity> = getInput("amount-of-money.json")
        assertEquals(22F, (res[0].structuredValue as NumericEntity.Unit).value)
        assertEquals("EUR", (res[0].structuredValue as NumericEntity.Unit).unit)
        assertEquals("MONEY", res[0].className)

        assertEquals(5F, (res[1].structuredValue as NumericEntity.Unit).value)
        assertEquals("$", (res[1].structuredValue as NumericEntity.Unit).unit)
    }

    @Test
    fun testDistance() {
        val res: List<NumericEntity> = getInput("distance.json")
        assertEquals(6005F, (res[0].structuredValue as NumericEntity.Unit).value)
        assertEquals("metre", (res[0].structuredValue as NumericEntity.Unit).unit)
        assertEquals("DISTANCE", res[0].className)
    }

    @Test
    fun testEmail() {
        val res: List<NumericEntity> = getInput("email.json")
        assertEquals("dummy@promethist.ai", (res[0].structuredValue as NumericEntity.StringValue).value)
        assertEquals("EMAIL", res[0].className)
    }

    @Test
    fun testNumber() {
        val res: List<NumericEntity> = getInput("number.json")
        assertEquals(22000F, (res[0].structuredValue as NumericEntity.NumericValue).value)
        assertEquals("NUMBER", res[0].className)
    }

    @Test
    fun testOrdinal() {
        val res: List<NumericEntity> = getInput("ordinal.json")
        assertEquals(3F, (res[0].structuredValue as NumericEntity.NumericValue).value)
        assertEquals("ORDINAL", res[0].className)
    }

    @Test
    fun testPhoneNumber() {
        val res: List<NumericEntity> = getInput("phone-number.json")
        assertEquals("420777777777", (res[0].structuredValue as NumericEntity.StringValue).value)
        assertEquals("PHONE-NUMBER", res[0].className)
    }

    @Test
    fun testTemperature() {
        val res: List<NumericEntity> = getInput("temperature.json")
        assertEquals(34F, (res[0].structuredValue as NumericEntity.Unit).value)
        assertEquals("celsius", (res[0].structuredValue as NumericEntity.Unit).unit)
        assertEquals("TEMPERATURE", res[0].className)
    }

    @Test
    fun testURL() {
        val res: List<NumericEntity> = getInput("url.json")
        assertEquals("www.promethist.ai/info", (res[0].structuredValue as NumericEntity.URL).value)
        assertEquals("promethist.ai", (res[0].structuredValue as NumericEntity.URL).domain)
        assertEquals("URL", res[0].className)
    }

    @Test
    fun testVolume() {
        val res: List<NumericEntity> = getInput("volume.json")
        assertEquals(4F, (res[0].structuredValue as NumericEntity.Unit).value)
        assertEquals("litre", (res[0].structuredValue as NumericEntity.Unit).unit)
        assertEquals("VOLUME", res[0].className)
    }
}