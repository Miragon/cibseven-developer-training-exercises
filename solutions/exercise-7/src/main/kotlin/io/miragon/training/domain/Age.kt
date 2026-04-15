package io.miragon.training.domain

data class Age(val value: Int) {
    init {
        require(value >= 0) { "Age must not be negative" }
    }
}
