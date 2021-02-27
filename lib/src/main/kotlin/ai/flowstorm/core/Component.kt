package ai.flowstorm.core

interface Component {
    fun process(context: Context): Context
}