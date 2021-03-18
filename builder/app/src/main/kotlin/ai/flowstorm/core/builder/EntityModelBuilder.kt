package ai.flowstorm.core.builder


interface EntityModelBuilder {
    fun trainEntityModel(dataset: EntityDataset)
    fun modelStatus(id: String): EntityDataset.Status?
}