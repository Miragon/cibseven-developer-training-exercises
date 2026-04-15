package io.miragon.training.adapter.outbound.db

import io.miragon.training.domain.Age
import io.miragon.training.domain.Email
import io.miragon.training.domain.Membership
import io.miragon.training.domain.MembershipId
import io.miragon.training.domain.Name
import org.springframework.stereotype.Component

@Component
class MembershipEntityMapper {

    fun toEntity(membership: Membership) = MembershipEntity(
        id = membership.id.value,
        email = membership.email.value,
        name = membership.name.value,
        age = membership.age.value,
    )

    fun toDomain(entity: MembershipEntity) = Membership(
        id = MembershipId(entity.id),
        email = Email(entity.email),
        name = Name(entity.name),
        age = Age(entity.age),
    )
}
