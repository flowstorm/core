package com.promethist.core.type

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import com.promethist.core.type.value.*
import com.promethist.core.type.value.Number
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DucklingEntityTest {/*//temporatily commented - needs to FIX some dependency issue
    val mapper = ObjectUtil.defaultMapper

    private fun getInput(name: String) =
            mapper.readValue<List<DucklingEntity>>(this::class.java.getResourceAsStream(name), object : TypeReference<List<DucklingEntity>>(){})

    @Test
    fun testTime() {
        val res: List<DucklingEntity> = getInput("time.json")
        assertEquals(6, (res[0].value as GrainedTime).value.hour)
        assertEquals("+02:00", (res[0].value as GrainedTime).value.zone.id)
        assertEquals("GrainedTime", res[0].className)

        assertEquals(17, (res[1].value as Interval).from!!.value.hour)
        assertEquals(21, (res[1].value as Interval).to!!.value.hour)

        assertEquals(Month.FEBRUARY, (res[2].value as Interval).from!!.value.month)
        assertNull((res[2].value as Interval).to)
    }

    @Test
    fun testQuantity() {
        val res: List<DucklingEntity> = getInput("quantity.json")
        assertEquals(2F, (res[0].value as Quantity).value)
        assertEquals("coffee", (res[0].value as Quantity).product)
        assertEquals("cup", (res[0].value as Quantity).unit)
        assertEquals("Quantity", res[0].className)

        assertEquals(3.5F, (res[1].value as Quantity).value)
        assertEquals("cup", (res[1].value as Quantity).unit)
    }

    @Test
    fun testAmountOfMoney() {
        val res: List<DucklingEntity> = getInput("amount-of-money.json")
        assertEquals("22".toBigDecimal(), (res[0].value as Currency).value)
        assertEquals("EUR", (res[0].value as Currency).unit)
        assertEquals("Currency", res[0].className)

        assertEquals("5".toBigDecimal(), (res[1].value as Currency).value)
        assertEquals("$", (res[1].value as Currency).unit)
    }

    @Test
    fun testDistance() {
        val res: List<DucklingEntity> = getInput("distance.json")
        assertEquals("6005".toBigDecimal(), (res[0].value as Distance).value)
        assertEquals("metre", (res[0].value as Distance).unit)
        assertEquals("Distance", res[0].className)
    }

    @Test
    fun testEmail() {
        val res: List<DucklingEntity> = getInput("email.json")
        assertEquals("dummy@promethist.ai", (res[0].value as Email).value)
        assertEquals("Email", res[0].className)
    }

    @Test
    fun testNumber() {
        val res: List<DucklingEntity> = getInput("number.json")
        assertEquals(22000F, (res[0].value as Number).value)
        assertEquals("Number", res[0].className)
    }

    @Test
    fun testOrdinal() {
        val res: List<DucklingEntity> = getInput("ordinal.json")
        assertEquals(3F, (res[0].value as Ordinal).value)
        assertEquals("Ordinal", res[0].className)
    }

    @Test
    fun testPhoneNumber() {
        val res: List<DucklingEntity> = getInput("phone-number.json")
        assertEquals("420777777777", (res[0].value as Phone).value)
        assertEquals("Phone", res[0].className)
    }

    @Test
    fun testTemperature() {
        val res: List<DucklingEntity> = getInput("temperature.json")
        assertEquals("34".toBigDecimal(), (res[0].value as Temperature).value)
        assertEquals("celsius", (res[0].value as Temperature).unit)
        assertEquals("Temperature", res[0].className)
    }

    @Test
    fun testURL() {
        val res: List<DucklingEntity> = getInput("url.json")
        assertEquals("www.promethist.ai/info", (res[0].value as URL).value)
        assertEquals("promethist.ai", (res[0].value as URL).domain)
        assertEquals("URL", res[0].className)
    }

    @Test
    fun testVolume() {
        val res: List<DucklingEntity> = getInput("volume.json")
        assertEquals("4".toBigDecimal(), (res[0].value as Volume).value)
        assertEquals("litre", (res[0].value as Volume).unit)
        assertEquals("Volume", res[0].className)
    }*/
}