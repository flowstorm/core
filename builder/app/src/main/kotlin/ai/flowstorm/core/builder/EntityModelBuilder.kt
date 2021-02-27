package ai.flowstorm.core.builder

import ai.flowstorm.core.model.EntityDataset

interface EntityModelBuilder {
    fun trainEntityModel(dataset: EntityDataset)
}