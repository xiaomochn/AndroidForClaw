package com.xiaomo.androidforclaw.gateway.protocol

/**
 * Gateway 错误异常
 */
class GatewayError(
    val code: String,
    message: String,
    val details: Any? = null
) : Exception(message)
