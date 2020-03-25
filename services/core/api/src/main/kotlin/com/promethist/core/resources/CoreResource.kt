package com.promethist.core.resources

import io.swagger.annotations.Api
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(description = "Core Service")
@Produces(MediaType.APPLICATION_JSON)
interface CoreResource :BotService {


}