package team.themoment.honeypotserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HoneypotServerApplication

fun main(args: Array<String>) {
    runApplication<HoneypotServerApplication>(*args)
}
