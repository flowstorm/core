package org.promethist.core.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import org.promethist.core.model.Report
import org.promethist.core.type.PropertyMap
import org.promethist.security.Authenticated
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@Api(tags = ["Reports"])
interface ReportResource {

    @GET
    @Path("/data")
    fun getData(
            @ApiParam(required = false) @QueryParam("granularity") granularity: Report.Granularity,
            @ApiParam(required = false) @QueryParam("aggregations") aggregations: List<String>
    ): Report

    @GET
    @Path("/metrics")
    fun getMetrics(): List<PropertyMap>

    @GET
    @Path("/namespaces")
    fun getNamespaces(): List<PropertyMap>
}