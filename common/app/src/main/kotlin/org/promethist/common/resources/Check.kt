package org.promethist.common.resources

data class Check(val health: Double, val name: String?, val namespace: String?, val `package`: String?, val git_ref: String?, val git_commit: String?, val app_image: String?)