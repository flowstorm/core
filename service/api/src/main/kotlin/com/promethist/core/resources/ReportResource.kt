package com.promethist.core.resources

import com.promethist.core.model.Report
import com.promethist.core.model.Report.Aggregation
import com.promethist.core.type.PropertyMap
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["Reports"])
@Produces(MediaType.APPLICATION_JSON)
@Path("/reports")
interface ReportResource {

    @GET
    @Path("/data")
    fun getData(
            @ApiParam(required = false) @QueryParam("granularity") granularity: Report.Granularity,
            @ApiParam(required = false) @QueryParam("aggregations") aggregations: List<Aggregation>
    ): Report

    @GET
    @Path("/metrics")
    fun getMetrics(): List<PropertyMap>

    @GET
    @Path("/namespaces")
    fun getNamespaces(): List<PropertyMap>
}