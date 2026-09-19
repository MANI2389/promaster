package com.example.promaster.domain.automation

/**
 * The PROMASTER safe Android automation framework orchestrator.
 *
 * PIPELINE:
 * Voice/Text Input
 *  → IntentRouter
 *  → CommandValidator
 *  → AutomationExecutor
 *  → Android API
 *  → Result
 */
interface SafeAutomationFramework {
    suspend fun process(input: String, isConfirmed: Boolean = false): AutomationResult
    suspend fun executeCommand(command: AutomationCommand): AutomationResult
}

class DefaultSafeAutomationFramework(
    private val intentRouter: AutomationIntentRouter = DefaultAutomationIntentRouter(),
    private val validator: CommandValidator = DefaultCommandValidator(),
    private val executor: AutomationExecutor = DefaultAutomationExecutor()
) : SafeAutomationFramework {

    override suspend fun process(input: String, isConfirmed: Boolean): AutomationResult {
        // 1. IntentRouter: Classifies voice/text into structured AutomationCommand
        val command = intentRouter.route(input, isConfirmed)

        // 2. CommandValidator: Enforces normal app bounds, confirmation requirements, parameter checks
        val validationResult = validator.validate(command)

        // 3. AutomationExecutor -> Android API -> AutomationResult
        return executor.execute(command, validationResult)
    }

    override suspend fun executeCommand(command: AutomationCommand): AutomationResult {
        val validationResult = validator.validate(command)
        return executor.execute(command, validationResult)
    }
}
