package com.promethist.core.model

data class Report(
        val columns: List<String>,
        val dataSets: MutableList<DataSet> = mutableListOf()
) {
    data class DataSet(val label: String, val data: MutableList<Long>)

    enum class Granularity { /*MINUTE,*/ HOUR, DAY, /*WEEK,*/ MONTH }
    enum class Aggregation { USER, NAMESPACE, METRIC, APPLICATION }
}

