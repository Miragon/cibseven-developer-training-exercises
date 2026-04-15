package io.miragon.training.domain

data class Email(val value: String) {
    init {
        require(value.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            "Invalid email address: $value"
        }
    }
}
