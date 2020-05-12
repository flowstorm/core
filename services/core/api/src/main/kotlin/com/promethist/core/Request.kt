package com.promethist.core

import com.promethist.core.type.Dynamic

data class Request(val appKey: String, val sender: String, val sessionId: String, val input: Input, val attributes: Dynamic = Dynamic())