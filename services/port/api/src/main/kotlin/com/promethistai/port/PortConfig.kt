package com.promethistai.port

import com.promethistai.common.DataObject
import com.promethistai.port.model.Contract

data class PortConfig(val host: String, val contract: Contract)