package com.promethist.core.runtime

import com.promethist.common.AppConfig
import com.promethist.core.dialogue.BasicDialogue

class SangixApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    data class Patient(val id: Int, val name: String? = "", val surname: String? = "", val email: String? = "", val phone: String? = "",
                       val phone2: String? = "", val phone3: String? = "", var apptList: List<Appointment>? = listOf()) {
    }

    data class Appointment(val apptId: String, val timestamp: String, val length: Int? = 0, val referenceNumber: String? = "") {
    }

    private val target get() = target(AppConfig.instance["sangix.url"])
    private val apiKey = java.util.Base64.getEncoder().encodeToString(AppConfig.instance["sangix.credentials"].toByteArray())
    private val headers get() = mapOf("Authorization" to "Basic ${apiKey}", "Accept" to "application/json")

    fun getPatient(birthdate: String, phone: String)
            = get<Patient>(target.path("/patient")
            .queryParam("birthdate", birthdate)
            .queryParam("phone", phone), headers)

    fun getAppointments(from: String, to: String, limit: Int)
            = get<Map<String, List<Any>>>(target.path("/appointments")
            .queryParam("from", from)
            .queryParam("to", to)
            .queryParam("limit", limit), headers)["apptList"]

    fun getAppointments(around: String, limit: Int)
            = get<Map<String, List<Any>>>(target.path("/appointments")
            .queryParam("around", around)
            .queryParam("limit", limit), headers)["apptList"]

    fun cancelAppointment(appt: String)
            = delete<String>(target.path("/appointment/${appt}"), headers)

    fun bookAppointment(appt: String, user: Int)
            = post<String>(target.path("/appointment/${appt}/${user}"),"", headers)

}

val BasicDialogue.sangix get() = DialogueApi.get<SangixApi>(this)