package trotech

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.kotlin.logger

private val logger = logger(name = "UDPHolePunchingServer")

fun main() {

    val port = 9999
    val serverSocket = DatagramSocket(port)
    val previousSessions = ConcurrentHashMap<String, InetSocketAddress>()


    logger.info("UDP Hole Punching Server запущен на порту $port")

    while (true) {
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        serverSocket.receive(packet)

        val message = String(packet.data, 0, packet.length).trim()
        val clientAddress = packet.socketAddress as InetSocketAddress
        logger.info("$clientAddress -- '$message'")

        val sessionId = message

        if (previousSessions.contains(sessionId)) {
            if (previousSessions[sessionId]?.address != clientAddress.address) {
                val clientA = previousSessions[sessionId]
                val clientB = clientAddress
                var response = "${clientA?.hostString}:${clientA?.port}"
                serverSocket.send(DatagramPacket(response.toByteArray(), response.length, clientB))
                response = "${clientB.hostString}:${clientB.port}"
                serverSocket.send(DatagramPacket(response.toByteArray(), response.length, clientA))
                previousSessions.remove(sessionId)
                logger.info("$clientA >==< '$clientB'")
            } else {
                continue
            }
        } else {
            previousSessions[sessionId] = clientAddress
        }
    }
}
