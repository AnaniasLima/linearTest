package com.example.lineartest.DataModel

import com.example.lineartest.BillAcceptor
import org.json.JSONObject
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class Event(
    var eventType: EventType = EventType.FW_STATUS_RQ,
    var action: String = QUESTION,
//    var requestToSendtimeStamp: String = Date().time.toString(),
    var timestamp: Long = Date().time) {

    companion object {
        val ON = "on"
        val OFF = "off"
        val QUESTION = "question"
        val RESET = "reset"
        val SIMULA5REAIS = "simula5"
        val SIMULA10REAIS = "simula10"
        val SIMULA20REAIS = "simula20"
        val SIMULA50REAIS = "simula50"

        var pktNumber: Int = 0
        var statusPktNumber: Int = 0
        var billAcceptorPktNumber: Int = 0
        var demoPktNumber: Int = 0
        var playPktNumber: Int = 0
        var pinpadPktNumber: Int = 0
        var tabletResetPktNumber: Int = 0
        var ledPktNumber: Int = 0
        var nackPktNumber: Int = 0
//        var noteiroOnTimestamp:String = ""



        fun getCommandData(event: Event): String {
            val commandData = JSONObject()

            when (event.eventType) {
                EventType.FW_STATUS_RQ -> pktNumber = ++statusPktNumber
                EventType.FW_BILL_ACCEPTOR   -> pktNumber = ++billAcceptorPktNumber
                EventType.FW_DEMO      -> pktNumber = ++demoPktNumber
                EventType.FW_PLAY      -> pktNumber = ++playPktNumber
                EventType.FW_PINPAD    -> pktNumber = ++pinpadPktNumber
                EventType.FW_LED       -> pktNumber = ++ledPktNumber
                EventType.FW_NACK      -> pktNumber = ++nackPktNumber
                EventType.FW_TABLET_RESET    -> pktNumber = ++tabletResetPktNumber
            }

            commandData.put("cmd", event.eventType.command)


            when ( event.eventType ) {
                EventType.FW_PINPAD -> {
                    if (event.action == ON) {
                        commandData.put("state", 1)
                    } else {
                        commandData.put("state", 0)
                    }
                }
                EventType.FW_BILL_ACCEPTOR -> {
//                    if ( event.action == ON ) {
//                        noteiroOnTimestamp = BillAcceptor.turnOnTimeStamp
//                    }
                }
                else -> {
                    // do nothing
                }
            }


            if (event.eventType == EventType.FW_PINPAD) { // Mateus porque FW_PINPAD ?
                if (event.action == ON) {
                    commandData.put("state", 1)
                } else {
                    commandData.put("state", 0)
                }
            } else {
                commandData.put("action", event.action)
            }

            commandData.put("packetNumber", pktNumber)
//            commandData.put("timestamp", event.timestamp.toString())
//            commandData.put("noteiroOnTimestamp", noteiroOnTimestamp)
            commandData.put("hour", SimpleDateFormat( "HH:mm:SS", Locale.getDefault()).format(Date()))

            return commandData.toString()
        }


    }
}

data class EventResponse(
    var cmd: String = "",
    var action: String = "",
    var error_n: Int = 0,
    var value: Int = 0,
    var mifare: Long = 0,
    var mifare_pass: Int = 0,
    var premio_n: Int = 0,
    var button_1: String = "",
    var button_2: String = "",
    var ret: String = "",
    var status:String = "",
    var fsm_state: String = "",
    var success: String = "",
    var R: String = "",
    var G: String = "",
    var B: String = "",
    var tR: String = "",
    var tB: String = "",
    var tG: String = "",
//    var timestamp: String = "",
//    var noteiroOnTimestamp: String = "",
    var cordinates: String = "",
    var eventType: EventType = EventType.FW_STATUS_RQ) {
    companion object {
        val OK = "ok"
        val ERROR = "error"
        val BUSY = "busy"
//        val NOK = "nok"
        var invalidJsonPacketsReceived = 0
    }
}

enum class EventType(val type: Int, val command: String) {
    FW_STATUS_RQ(0, "fw_status_rq"),
    FW_PINPAD(1, "fw_pinpad"),
    FW_TABLET_RESET(2, "fw_tablet_reset"),
    FW_PLAY(3, "fw_play"),
    FW_DEMO(4, "fw_demo"),
    FW_BILL_ACCEPTOR(5, "fw_noteiro"),
    FW_LED(6, "fw_led"),
    FW_NACK(7, "fw_nack");

    companion object {
        fun getByCommand(command: String): EventType? {
            for (value in values()) {
                if (value.command == command) {
                    return value
                }
            }
            return null
        }
    }
}
