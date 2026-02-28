package io.miragon.training.domain

data class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "Name cannot be blank" }
    }
}
