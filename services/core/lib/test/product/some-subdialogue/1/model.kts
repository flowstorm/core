package `product`.`some-subdialogue`

import com.promethist.core.ResourceLoader
import com.promethist.core.model.*

data class Model1(
        override val resourceLoader: ResourceLoader,
        override val name: String,

        // dialogue properties (always var)
        var i: Int = 1

) : Dialogue(resourceLoader, name)

// export
Model1::class