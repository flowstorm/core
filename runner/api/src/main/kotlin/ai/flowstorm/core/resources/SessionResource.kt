package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import org.litote.kmongo.Id
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn
import ai.flowstorm.core.model.User
import ai.flowstorm.security.Authenticated
import javax.ws.rs.GET
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Sessions"])
@Produces(MediaType.APPLICATION_JSON)
interface SessionResource {

    @GET
    fun find(): List<Session>

    fun findBy(sessionId: String): Session?
    fun create(session: Session): Session
    fun create(turn: Turn): Turn
    fun update(session: Session): Session
}