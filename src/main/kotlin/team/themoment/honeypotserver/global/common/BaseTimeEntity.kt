package team.themoment.honeypotserver.global.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set
}
