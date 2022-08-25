/*
 * @author Constantin Chelban (constantink@saltedge.com)
 * Copyright (c) 2022 Salt Edge.
 */
package com.saltedge.provider.demo.model

import org.springframework.data.jpa.repository.JpaRepository

interface ScaConnectionsRepository : JpaRepository<ScaConnectionEntity, Long> {
    fun findByRevokedIsFalse(): List<ScaConnectionEntity>
    fun findFirstByConnectionId(connectionId: String?): ScaConnectionEntity?
    fun findByUserId(userId: String?): List<ScaConnectionEntity>
    fun findFirstByConnectionIdAndRevokedIsFalse(connectionId: String?): ScaConnectionEntity?
}
