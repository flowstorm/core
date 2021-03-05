package ai.flowstorm.core.builder

import com.fasterxml.jackson.annotation.JsonSetter

open class EntityDataset(
    val name: String,
    val language: String = "en",
    val valueSamples: Map<String, List<String>> = mapOf(),
    var params: Map<String, Any>? = null,
    var data: List<String> = listOf()
) {
    @JsonSetter("samples")
    fun loadData(samples: List<DataSample>) {
        data = samples.map { it.text }
    }

    data class DataSample(val text: String, var _id: Int = -1)
}