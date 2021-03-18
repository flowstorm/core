package ai.flowstorm.core.builder.resources

import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.builder.*
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/builder")
@Produces(MediaType.APPLICATION_JSON)
class BuilderResourceImpl : BuilderResource {

    @Inject
    lateinit var dialogueBuilder: DialogueBuilder

    @Inject
    lateinit var entityModelBuilder: EntityModelBuilder

    override fun info(): Info {
        val buf = StringBuilder()
        if (dialogueBuilder.kotlinc("-version") { buf.appendLine(it) } != 0)
            error(buf)
        return Info(buf.toString().trim())
    }

    @Authorized
    override fun build(request: Request): Response = with (request) {
        val builder = dialogueBuilder.create(sourceCode)
        val build = builder.build(oodExamples)
        builder.deploy()
        return Response(build)
    }

    @Authorized
    override fun trainEntityModel(dataset: EntityDataset) {
        entityModelBuilder.trainEntityModel(dataset)
    }

    @Authorized
    override fun modelStatus(id: String): EntityDataset.Status = entityModelBuilder.modelStatus(id) ?: throw NotFoundException()
}