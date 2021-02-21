package org.promethist.core.model

import com.fasterxml.jackson.annotation.JsonSetter
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import org.promethist.common.model.Entity

data class EntityDataset(
    override val _id: Id<EntityDataset> = newId(),
    val name: String,
    val language: String = "en",
    val valueSamples: Map<String, List<String>> = mapOf(),
    var params: Map<String, Any>?,
    var data: List<String> = listOf()
): Entity<EntityDataset> {

    @JsonSetter("samples")
    fun loadData(samples: List<DataSample>) {
        data = samples.map { it.text }
    }

    data class DataSample(val text: String, var _id: Int = -1)
}