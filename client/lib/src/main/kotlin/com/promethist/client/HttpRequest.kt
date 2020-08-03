package com.promethist.client

data class HttpRequest(val method: String = "GET", val contentType: String = "application/json", val headers: Map<String, String> = emptyMap(), val body: ByteArray = ByteArray(0))