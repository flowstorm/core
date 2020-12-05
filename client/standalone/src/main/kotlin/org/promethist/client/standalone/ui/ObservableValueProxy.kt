package org.promethist.client.standalone.ui

import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue

class ObservableValueProxy<T>(val observableValue: ObservableValue<T>, val valueHandler: (T) -> T) : ObservableValue<T> {
    override fun removeListener(listener: ChangeListener<in T>?) = observableValue.removeListener(listener)
    override fun removeListener(listener: InvalidationListener?) = observableValue.removeListener(listener)
    override fun addListener(listener: InvalidationListener?) = observableValue.addListener(listener)
    override fun addListener(listener: ChangeListener<in T>?) = observableValue.addListener(listener)
    override fun getValue(): T = valueHandler(observableValue.value)
}