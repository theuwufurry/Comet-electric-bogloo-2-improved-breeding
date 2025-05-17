package gg.aquatic.comet.api.packet

import java.util.concurrent.ConcurrentHashMap

object PassengerManager {
    val passengerMap: MutableMap<Int, MutableList<Int>> = ConcurrentHashMap()
}