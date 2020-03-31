package com.promethist.core.model

data class FileObject(
        var name: String,
        var size: Long,
        var contentType: String,
        var createTime: Long,
        var updateTime: Long,
        var metadata: Map<String, String>? = mapOf()
)