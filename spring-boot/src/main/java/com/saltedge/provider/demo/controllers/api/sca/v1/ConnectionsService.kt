/*
 * @author Constantin Chelban (constantink@saltedge.com)
 * Copyright (c) 2022 Salt Edge.
 */
package com.saltedge.provider.demo.controllers.api.sca.v1

import com.saltedge.provider.demo.callback.AccessTokenWrapper
import com.saltedge.provider.demo.callback.ScaServiceCallback
import com.saltedge.provider.demo.config.DemoApplicationProperties
import com.saltedge.provider.demo.config.SCA_CONNECT_QUERY_PREFIX
import com.saltedge.provider.demo.config.SCA_USER_ID
import com.saltedge.provider.demo.controllers.api.sca.v1.model.CreateConnectionRequestData
import com.saltedge.provider.demo.controllers.api.sca.v1.model.CreateConnectionResponseData
import com.saltedge.provider.demo.controllers.provider.ScaAuthController
import com.saltedge.provider.demo.errors.BadRequest
import com.saltedge.provider.demo.errors.NotFound
import com.saltedge.provider.demo.model.ScaConnectionEntity
import com.saltedge.provider.demo.model.ScaConnectionsRepository
import com.saltedge.provider.demo.tools.encryptAccessToken
import com.saltedge.provider.demo.tools.getRandomString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class ConnectionsService {
    @Autowired
    lateinit var demoApplicationProperties: DemoApplicationProperties
    @Autowired
    lateinit var connectionsRepository: ScaConnectionsRepository
    @Autowired
    lateinit var callbackService: ScaServiceCallback

    @Throws(NotFound.UserNotFound::class, BadRequest.WrongRequestFormat::class)
    fun processConnectRequest(data: CreateConnectionRequestData, authRsaPublicKeyPem: String): CreateConnectionResponseData {
        val userId = data.connectQuery?.replace(SCA_CONNECT_QUERY_PREFIX, "")
        return when {
            userId == null -> {
                val authenticationUrl = ScaAuthController.authenticationPageUrl(
                    applicationUrl = demoApplicationProperties.applicationUrl,
                    connectionId = data.connectionId
                )
                createScaConnectionEntity(data, authRsaPublicKeyPem, userId)
                CreateConnectionResponseData(authenticationUrl)
            }
            userId != SCA_USER_ID -> {
                onFailAuthentication(
                    connectionId = data.connectionId,
                    returnUrl = data.returnUrl,
                    message = "Invalid connect query [${data.connectQuery}], user not found"
                )
            }
            connectionsRepository.findFirstByConnectionId(data.connectionId) != null -> {
                throw BadRequest.WrongRequestFormat(errorMessage = "Connection with id:${data.connectionId} exist")
            }
            else -> {
                val authenticationUrl = ScaAuthController.authenticationPageUrl(
                    applicationUrl = demoApplicationProperties.applicationUrl,
                    connectionId = data.connectionId,
                    connectQuery = data.connectQuery
                )
                createScaConnectionEntity(data, authRsaPublicKeyPem, userId)
                CreateConnectionResponseData(authenticationUrl)
            }
        }
    }

    private fun onFailAuthentication(connectionId: String, returnUrl: String, message: String): CreateConnectionResponseData {
        callbackService.sendFailAuthenticationCallback(scaConnectionId = connectionId, failMessage = message)
        return CreateConnectionResponseData(errorAuthRedirect(returnUrl = returnUrl, error = "Unauthorized request"))
    }

    fun authorizeConnection(scaConnectionId: String, userId: String): String {
        val connection = connectionsRepository.findFirstByConnectionIdAndRevokedIsFalse(scaConnectionId)
        return if (userId != SCA_USER_ID || connection == null) {
            callbackService.sendFailAuthenticationCallback(
                scaConnectionId = scaConnectionId,
                failMessage = "Unauthorized request"
            )
            errorAuthRedirect(returnUrl = connection?.returnUrl ?: "", error = "Unauthorized request")
        } else {
            val accessToken = getRandomString(32)
            connection.accessToken = accessToken
            connection.userId = userId
            connectionsRepository.save(connection)

            callbackService.sendSuccessAuthenticationCallback(
                scaConnectionId = scaConnectionId,
                accessToken = accessToken,
                rsaPublicKey = connection.rsaPublicKey
            )

            val encryptedToken = encryptAccessToken(accessToken, connection.rsaPublicKey)
            val encodedToken = URLEncoder.encode(encryptedToken, "UTF-8")
            UriComponentsBuilder
                .fromUriString(connection.returnUrl)
                .queryParam("access_token", encodedToken)
                .build().toUriString()
        }
    }

    fun revokeConnection(connectionId: String) {
        connectionsRepository.findFirstByConnectionIdAndRevokedIsFalse(connectionId)?.let {
            it.revoked = true
            connectionsRepository.save(it)
        } ?: throw NotFound.ConnectionNotFound()
    }

    private fun errorAuthRedirect(returnUrl: String, error: String): String {
        return UriComponentsBuilder.fromUriString(returnUrl)
            .queryParam("error_class", "AUTHENTICATION_FAILED")
            .queryParam("error_message", URLEncoder.encode(error, StandardCharsets.UTF_8.toString()))
            .build().toUriString()
    }

    private fun createScaConnectionEntity(data: CreateConnectionRequestData, authRsaPublicKeyPem: String, userId: String?) {
        val entity = ScaConnectionEntity()
        entity.connectionId = data.connectionId
        entity.rsaPublicKey = authRsaPublicKeyPem
        entity.returnUrl = data.returnUrl
        userId?.let { entity.userId = it }
        connectionsRepository.save(entity)
    }
}
