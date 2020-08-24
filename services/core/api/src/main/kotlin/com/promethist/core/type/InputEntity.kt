package com.promethist.core.type

import com.promethist.core.Input
import com.promethist.core.type.value.Text
import com.promethist.core.type.value.Value
import java.util.NoSuchElementException

open class InputEntity(var className: String,
                       open var text: String,
                       var confidence: Float,
                       val modelId: String) {

    open val value: Value get() = Text(text)

    companion object {

        fun fromAnnotation(words: List<Input.Word>): MutableMap<String, MutableList<InputEntity>> {
            val map = mutableMapOf<String, MutableList<InputEntity>>()
            var prevOutside = true
            words.forEach { word ->
                word.classes.forEach {
                    if (it.type == Input.Class.Type.Entity) {
                        val beginning = it.name.startsWith("B-")
                        val inside = it.name.startsWith("I-")
                        val className = if (beginning || inside) it.name.substring(2) else it.name
                        if (!map.containsKey(className))
                            map[className] = mutableListOf()
                        if (inside) {
                            try {
                                // May throw NoSuchElementException if the annotation is not valid
                                val last = map[className]!!.last { last -> last.modelId == it.model_id && !prevOutside && last.className == className }
                                if (last.modelId == it.model_id) {
                                    last.text += " " + word.text
                                    var length = last.text.split(" ").size
                                    last.confidence += (it.score - last.confidence) / length
                                }
                            } catch (e: NoSuchElementException) {
                                // Invalid annotation (an entity starts with I tag). Treating I as B
                                map[className]!!.add(InputEntity(className, word.text, it.score, it.model_id))
                            }
                        }
                        if (!inside)
                            map[className]!!.add(InputEntity(className, word.text, it.score, it.model_id))
                    }
                }
                prevOutside = !word.classes.any { it.type == Input.Class.Type.Entity }
            }
            return map
        }
    }

    override fun toString(): String {
        return "InputEntity(className='$className', text='$text', confidence=$confidence, modelId='$modelId')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InputEntity

        if (className != other.className) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }


}