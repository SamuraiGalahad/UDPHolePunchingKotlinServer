package trotech

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import org.apache.logging.log4j.kotlin.logger

private val logger = logger(name = "UDPHolePunchingServer")

fun main() {

    val port = 9999
    val serverSocket = DatagramSocket(port)
    val previousSessions = mutableMapOf<String, InetSocketAddress>()


    logger.info("UDP Hole Punching Server запущен на порту $port")

    startHttpStatusServer()

    while (true) {
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        serverSocket.receive(packet)

        val message = String(packet.data, 0, packet.length).trim()
        val clientAddress = packet.socketAddress as InetSocketAddress
        logger.info("$clientAddress -- '$message'")

        val sessionId = message
        println(sessionId)
        if (previousSessions.keys.contains(sessionId)) {
            if (previousSessions[sessionId] != clientAddress) {
                val clientA = previousSessions[sessionId]
                val clientB = clientAddress
                var response = "${clientA?.hostString}:${clientA?.port}"
                serverSocket.send(DatagramPacket(response.toByteArray(), response.length, clientB))
                response = "${clientB.hostString}:${clientB.port}"
                serverSocket.send(DatagramPacket(response.toByteArray(), response.length, clientA))
                previousSessions.remove(sessionId)
                logger.info("$clientA >==< '$clientB'")
                print(previousSessions)
            } else {
                continue
            }
        } else {
            previousSessions[sessionId] = clientAddress
        }
    }
}


fun startHttpStatusServer() {
    GlobalScope.launch {
        embeddedServer(Netty, port = 9998) {
            routing {
                get("/status") {
                    call.respondText("UDP Server is running")
                }
            }
        }.start(wait = false)
    }
}
