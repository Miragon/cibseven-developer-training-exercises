package io.miragon.training.adapter.outbound.db

import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.Membership
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Component

@Component
class MembershipPersistenceAdapter(
    private val jpaRepository: MembershipJpaRepository,
    private val mapper: MembershipEntityMapper,
) : MembershipRepository {

    override fun find(id: MembershipId): Membership {
        val entity = jpaRepository.findById(id.value)
            .orElseThrow { NoSuchElementException("Membership not found: ${id.value}") }
        return mapper.toDomain(entity)
    }

    override fun save(membership: Membership): Membership {
        val entity = mapper.toEntity(membership)
        jpaRepository.save(entity)
        return membership
    }
}
