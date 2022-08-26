/*
 * @author Constantin Chelban (constantink@saltedge.com)
 * Copyright (c) 2022 Salt Edge.
 */
package com.saltedge.provider.demo.controllers.api.sca.v1.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateActionResponse(
    @JsonProperty("data") var data: UpdateActionResponseData
)

data class UpdateActionResponseData(
    @JsonProperty("close_action") var close_action: Boolean
)
