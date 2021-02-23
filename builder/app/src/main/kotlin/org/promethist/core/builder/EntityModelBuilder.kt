package org.promethist.core.builder

import org.promethist.core.model.EntityDataset

interface EntityModelBuilder {
    fun trainEntityModel(dataset: EntityDataset)
}