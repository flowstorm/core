package com.promethist.core.resources

import com.promethist.core.model.Session
import com.promethist.core.model.User
import io.swagger.annotations.Api
import io.swagger.annotations.Authorization
import org.litote.kmongo.Id
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Sessions"])
@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
interface SessionResource {

    @GET
    fun getSessions():List<Session>

    fun get(sessionId: String): Session?
    fun create(session: Session)
    fun getForUser(userId: Id<User>): List<Session>
    fun update(session: Session)
}