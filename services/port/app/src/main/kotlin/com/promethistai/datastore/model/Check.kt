package com.promethistai.datastore.model

import com.promethistai.datastore.server.Config

data class Check(val health: Double, val name: String, val namespace: String, val git_ref: String, val git_commit: String, val app_image: String)