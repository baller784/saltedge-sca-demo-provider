/*
 * @author Constantin Chelban (constantink@saltedge.com)
 * Copyright (c) 2022 Salt Edge.
 */
package com.saltedge.provider.demo.controllers.provider

import com.saltedge.provider.demo.callback.ScaServiceCallback
import com.saltedge.provider.demo.config.APP_LINK_PREFIX_CONNECT
import com.saltedge.provider.demo.config.DemoApplicationProperties
import com.saltedge.provider.demo.config.SCA_CONNECT_QUERY
import com.saltedge.provider.demo.model.ScaConnectionsRepository
import com.saltedge.provider.demo.tools.QrTools
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.ModelAndView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletResponse

@Controller
class ProviderConnectionsController {

    @Autowired
    lateinit var demoApplicationProperties: DemoApplicationProperties
    @Autowired
    lateinit var repository: ScaConnectionsRepository
    @Autowired
    lateinit var callbackService: ScaServiceCallback

    @GetMapping("/connections")
    fun showConnections(): ModelAndView {
        val connections = repository.findAll(Sort.by(Sort.Direction.DESC, "id"))
        return ModelAndView("connections").addObject("connections", connections)
    }

    /**
     * http://localhost:8080/connections/qr
     */
    @GetMapping("/connections/qr")
    fun connectQRImage(response: HttpServletResponse) {
        val appLink: String = createConnectAppLink()
        QrTools.encodeTextAsQrPngImage(appLink, 256, 256)?.let { image ->
            response.contentType = "image/png"
            val outputStream = response.outputStream
            outputStream.write(image)
            outputStream.flush()
            outputStream.close()
        }
    }

    @GetMapping("/connections/{connection_id}/revoke")
    fun revokeConnection(@PathVariable("connection_id") connectionId: Long): ModelAndView {
        repository.findById(connectionId).orElse(null)?.let {
            it.revoked = true
            repository.save(it)
            callbackService.sendRevokeConnectionCallback(it.connectionId)
        }
        return ModelAndView("redirect:/connections")
    }

    /**
     * authenticator://saltedge.com/connect?configuration=https://saltedge.com/configuration&connect_query=A12345678
     */
    private fun createConnectAppLink(): String {
        val configurationUrl = "${demoApplicationProperties.scaServiceUrl}/api/authenticator/v2/configurations/${demoApplicationProperties.scaProviderId}"
        val encodedConfigurationUrl = URLEncoder.encode(configurationUrl, StandardCharsets.UTF_8.toString())
        val encodedConnectQuery = URLEncoder.encode(SCA_CONNECT_QUERY, StandardCharsets.UTF_8.toString())
        return "$APP_LINK_PREFIX_CONNECT$encodedConfigurationUrl&connect_query=$encodedConnectQuery"
    }
}
