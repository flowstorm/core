package com.promethist.core

import com.promethist.core.Input

data class Request(val key: String, val sender: String, val sessionId: String, val input: Input)