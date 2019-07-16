package com.promethistai.datastore.model

import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path("users")
@Produces(APPLICATION_JSON)
class UserResource {
    private val users = HashMap<String, User>()

    init {
        users += "Gerard" to User("Gerard", "secret", 25)
    }

    @GET @Path("{username}")
    fun getUser(@PathParam("username") username: String): User? {
        return users[username]
    }

    @POST
    fun createUser(user: User) {
        users += user.username to user
    }

    @PUT @Path("{username}")
    fun updateUser(@PathParam("username") username: String, user: User) {
        users -= username
        users += user.username to user
    }

    @DELETE @Path("{username}")
    fun deleteUser(@PathParam("username") username: String): User? {
        return users.remove(username)
    }
}