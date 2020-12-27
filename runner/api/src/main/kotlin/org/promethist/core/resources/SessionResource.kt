package org.promethist.core.resources

import io.swagger.annotations.Api
import org.litote.kmongo.Id
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.security.Authenticated
import javax.ws.rs.GET
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Sessions"])
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
interface SessionResource {

    @GET
    fun getSessions():List<Session>

    fun get(sessionId: String): Session?
    fun create(session: Session)
    fun getForUser(userId: Id<User>): List<Session>
    fun update(session: Session)
}