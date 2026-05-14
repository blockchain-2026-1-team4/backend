package com.blockchain2026.team4.backend.common.security

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DevAuthenticationBootstrapper(
    private val devAuthenticationService: DevAuthenticationService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        devAuthenticationService.ensureDevUserIfEnabled()
    }
}
