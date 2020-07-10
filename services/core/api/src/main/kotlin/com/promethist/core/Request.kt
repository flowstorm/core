package com.promethist.core

import com.promethist.core.type.PropertyMap

data class Request(val appKey: String, val sender: String, val token: String? = null, val sessionId: String, val input: Input, val attributes: PropertyMap)