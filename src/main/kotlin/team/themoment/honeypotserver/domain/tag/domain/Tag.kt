package team.themoment.honeypotserver.domain.tag.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tags")
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true, length = 30)
    val name: String,
)
