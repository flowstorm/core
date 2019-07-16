package com.promethistai.datastore.model

import com.google.cloud.Timestamp
import com.google.cloud.datastore.*
import java.io.Serializable
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import java.util.*

@Path("object")
@Produces(MediaType.APPLICATION_JSON)
class ObjectResource: DatastoreResource() {
    
    @GET @Path("{component}/{type}")
    fun getObjects(@PathParam("component") component: String, @PathParam("type") type: String, @QueryParam("orderBy") orderBy: String = "_created"): List<Map<String, Serializable>> {
        val queryBuilder =
            Query.newEntityQueryBuilder()
                .setNamespace("$namespace/$component")
                .setKind(type)
                .setOrderBy(StructuredQuery.OrderBy.asc(orderBy))

        val query = queryBuilder.build()
        val queryResults = datastore.run(query)
        val objects = Vector<Map<String, Serializable>>()
        for (entity in queryResults) {
            objects.add(entity.properties)
        }
        return objects
    }

    @GET @Path("{component}/{type}/{id}")
    fun getObject(@PathParam("component") component: String, @PathParam("type") type: String, @PathParam("id") id: Long): Map<String, Serializable>? {
        val key = getKeyFactory(component, type).newKey(id)
        return datastore.get(key).properties
    }


    @POST @Path("{component}/{type}")
    fun createObject(@PathParam("component") component: String, @PathParam("type") type: String, @QueryParam("key") apiKey: String, @QueryParam("scope") scope: String = "private"): Key {
        val entity = createEntity(component, type, apiKey, scope)
        datastore.put(entity)
        return entity.key
    }
    /*

    @PUT
    @Path("{id}")
    fun updateObject(@PathParam("id") id: String) {
        //TODO
    }
    */

    @DELETE
    @Path("{component}/{type}/{id}")
    fun deleteObject(@PathParam("component") component: String, @PathParam("type") type: String, @PathParam("id") id: Long) {
        deleteEntity(component, type, id)
    }
}