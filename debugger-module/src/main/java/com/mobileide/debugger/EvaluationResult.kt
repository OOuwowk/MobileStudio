package com.mobileide.debugger

/**
 * Result of evaluating an expression in the debugger
 *
 * @property success Whether the evaluation was successful
 * @property message A message describing the result
 * @property value The result value, or null if the evaluation failed
 */
data class EvaluationResult(
    val success: Boolean,
    val message: String,
    val value: String?
)