package io.miragon.training.domain

data class Membership(
    val id: MembershipId = MembershipId(),
    val email: Email,
    val name: Name,
    val age: Age,
)
