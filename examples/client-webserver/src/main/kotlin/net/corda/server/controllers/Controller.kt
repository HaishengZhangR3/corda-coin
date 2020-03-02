package net.corda.server.controllers

import net.corda.server.NodeRPCConnection
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/loc")
class Controller(rpc: NodeRPCConnection) {
    private val proxy = rpc.proxy

    @GetMapping(value = ["/test"], produces = [MediaType.TEXT_PLAIN_VALUE])
    private fun status() = "from loc test web service"
}
