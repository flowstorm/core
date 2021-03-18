package ai.flowstorm.core.builder

import ai.flowstorm.common.model.Entity
import ai.flowstorm.core.model.Space
import com.fasterxml.jackson.annotation.JsonSetter
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class EntityDataset(
    override val _id: Id<EntityDataset> = newId(),
    var space_id: Id<Space>?,
    val name: String,
    val language: String = "en",
    val valueSamples: Map<String, List<String>> = mapOf(),
    var params: Map<String, Any>? = null,
    var data: List<String> = listOf()
): Entity<EntityDataset> {
    @JsonSetter("samples")
    fun loadData(samples: List<DataSample>) {
        data = samples.map { it.text }
    }

    data class DataSample(val text: String, var _id: Int = -1)

    data class Status(val status: String)
}