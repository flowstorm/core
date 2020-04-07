package com.promethist.core

data class Request(val appKey: String, val sender: String, val sessionId: String, val input: Input)