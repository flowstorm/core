package org.promethist.core

import org.promethist.core.type.PropertyMap

data class Request(val appKey: String, val sender: String, val token: String? = null, val sessionId: String, val initiationId: String? = null, val input: Input, var attributes: PropertyMap)