package org.promethist.core.builder.resources

import org.promethist.core.builder.DialogueBuilder
import org.promethist.core.builder.Info
import org.promethist.core.builder.Request
import org.promethist.core.builder.Response
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/builder")
@Produces(MediaType.APPLICATION_JSON)
class BuilderResourceImpl : BuilderResource {

    @Inject
    lateinit var dialogueBuilder: DialogueBuilder

    override fun info(): Info {
        val buf = StringBuilder()
        if (dialogueBuilder.kotlinc("-version") { buf.appendLine(it) } != 0)
            error(buf)
        return Info(buf.toString().trim())
    }

    override fun build(request: Request): Response = with (request) {
        val builder = dialogueBuilder.create(sourceCode)
        val build = builder.build(oodExamples)
        builder.deploy()
        return Response(build)
    }

}