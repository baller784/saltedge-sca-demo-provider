/*
 * @author Constantin Chelban (constantink@saltedge.com)
 * Copyright (c) 2022 Salt Edge.
 */
package com.saltedge.provider.demo.tools.security

import com.saltedge.provider.demo.errors.BadRequest
import com.saltedge.provider.demo.tools.JsonTools
import io.jsonwebtoken.*
import io.jsonwebtoken.jackson.io.JacksonSerializer
import java.security.PrivateKey
import java.security.PublicKey
import java.time.Instant
import java.util.*

object JwsTools {

    public fun encode(requestData: Any, expiresAt: Instant, key: PrivateKey): String {
        val jws: String = Jwts.builder()
            .serializeToJsonWith(JacksonSerializer(JsonTools.defaultMapper))
            .claim("data", requestData)
            .signWith(key)
            .setExpiration(Date.from(expiresAt))
            .compact()
        val sections: MutableList<String> = jws.split(".").toMutableList()
        sections[1] = ""
        return sections.joinToString(".")
    }

    @Throws(BadRequest::class)
    fun isSignatureValid(jwsSignature: String, rawRequestBody: String, key: PublicKey): Boolean {
        if (rawRequestBody.isEmpty()) throw BadRequest.WrongRequestFormat()
        try {
            val jwsParts = jwsSignature.split(".").toMutableList()
            jwsParts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(rawRequestBody.toByteArray(Charsets.UTF_8))
            val encodedJws = jwsParts.joinToString(".")
            val claims: Jws<Claims> = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(encodedJws)
            return true
        } catch (e: ExpiredJwtException) {
            e.printStackTrace()
            throw BadRequest.SignatureExpired()
        } catch (e: JwtException) {
            e.printStackTrace()
            throw BadRequest.InvalidSignature(e.message ?: "JwtException")
        } catch (e: Exception) {
            e.printStackTrace()
            throw BadRequest.WrongRequestFormat(e.message ?: "WrongRequestFormat")
        }
    }
}
