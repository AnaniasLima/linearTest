package com.example.lineartest

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.lineartest.DataModel.Event
import com.example.lineartest.DataModel.EventResponse
import com.example.lineartest.DataModel.EventType
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


enum class DeviceState  {
    UNKNOW,
    OFF,
    ON,
    RESET;
}

enum class DeviceCommand  {
    OFF,
    ON,
    RESET,
    QUESTION,
    SIMULA5REAIS,
    SIMULA10REAIS,
    SIMULA50REAIS
}


@SuppressLint("StaticFieldLeak")
object BillAcceptor {
    val WAIT_WHEN_OFFLINE = 5000L
    val WAIT_TIME_TO_QUESTION = 10000L
    val WAIT_TIME_TO_RESPONSE = 300L
    val WAIT_TIME_IMEDIATE = 10L
    val BUSY_LIMIT_COUNTER = 10

    var mainActivity: AppCompatActivity? = null
    var context: Context? = null

    var billAcceptorHandler = Handler()

    var desiredState : DeviceState = DeviceState.OFF
    var inBusyStateCounter = 0
    var inErrorStateCounter = 0         // TODO: Teoricamente nunca deve acontecer. Vamos criar uma forma de tratar caso fique > 0
    var changeStateRequestCounter = 0

    var receivedState: DeviceState = DeviceState.UNKNOW

    var receivedValue = 0  // when receive values store here until

    var turnOnTimeStamp: String = ""
    var stateMachineRunning = false


    private fun mostraNaTela(str:String) {
        (mainActivity as MainActivity).mostraNaTela(str)
    }

    private fun mostraEmHistory(str:String) {
        (mainActivity as MainActivity).mostraEmHistory(str)
    }

    fun fakeBillAccept(value: Int) {
        when (value) {
            5 -> sendCommandToDevice(DeviceCommand.SIMULA5REAIS)
            10 -> sendCommandToDevice(DeviceCommand.SIMULA10REAIS)
            50 -> sendCommandToDevice(DeviceCommand.SIMULA50REAIS)
        }
//        deviceChecking(WAIT_TIME_TO_RESPONSE)
    }


    fun isEnabled() : Boolean {
        return receivedState == DeviceState.ON
    }

    fun StartStateMachine()  {
        stateMachineRunning = true
        desiredState = DeviceState.ON
        deviceChecking(WAIT_TIME_TO_RESPONSE)
    }

    fun StopStateMachine()  {
        stateMachineRunning = false
    }

    fun SendTurnOn() {
        sendCommandToDevice(DeviceCommand.ON)
        sendCommandToDevice(DeviceCommand.QUESTION)
    }

    fun SendTurnOff() {
        sendCommandToDevice(DeviceCommand.OFF)
    }

    fun SendQuestion() {
        sendCommandToDevice(DeviceCommand.QUESTION)
    }

    fun SendReset() {
        sendCommandToDevice(DeviceCommand.RESET)
    }

    fun turnOn() {
        if ( stateMachineRunning ) {
            desiredState = DeviceState.ON
            deviceChecking(WAIT_TIME_TO_RESPONSE)
        } else {
            sendCommandToDevice(DeviceCommand.ON)
            sendCommandToDevice(DeviceCommand.QUESTION)
        }
    }

    fun turnOff() {
        if ( stateMachineRunning ) {
            desiredState = DeviceState.OFF
            deviceChecking(WAIT_TIME_TO_RESPONSE)
        } else {
            sendCommandToDevice(DeviceCommand.OFF)
            sendCommandToDevice(DeviceCommand.QUESTION)
        }
    }


    fun deviceChecking(delay: Long) {
        if ( stateMachineRunning ) {
            var delayToNext = delay

            if ( ! ConnectThread.isConnected ) {
                delayToNext = WAIT_WHEN_OFFLINE
            } else {
                if ( delayToNext == 0L ) {
                    delayToNext = WAIT_TIME_TO_QUESTION
                }
            }

            Timber.i("agendando deviceChecking %s (%s)",
                SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time.time.plus(delayToNext)) ,
                delayToNext.toString())

            billAcceptorHandler.removeCallbacks(deviceCheckRunnable)
            billAcceptorHandler.postDelayed(deviceCheckRunnable, delayToNext)
        }
    }

    private var deviceCheckRunnable = Runnable {

        if ( receivedState == desiredState ) {
            Timber.i("receivedState=%s  desiredState=%s", receivedState, desiredState)
            sendCommandToDevice(DeviceCommand.QUESTION)
            deviceChecking(WAIT_TIME_TO_QUESTION ) // Ao receber a resposta de QUESTION vai agendar um novo QUESTION
        } else {

            when (desiredState) {
                DeviceState.RESET -> {
                    resetCredits()
                }
                DeviceState.ON -> {
                    sendCommandToDevice(DeviceCommand.ON)
//                    sendCommandToDevice(DeviceCommand.QUESTION)
                }
                DeviceState.OFF -> {
                    sendCommandToDevice(DeviceCommand.OFF)
//                    sendCommandToDevice(DeviceCommand.QUESTION)
                }
                else -> {
                    desiredState = DeviceState.ON
                    sendCommandToDevice(DeviceCommand.ON)
//                    sendCommandToDevice(DeviceCommand.QUESTION)
                    println("ATENÇÃO: CCC Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                }
            }
            deviceChecking(WAIT_TIME_TO_RESPONSE)
        }
    }



    fun processReceivedResponse(response : EventResponse) {
        var resetTimer= 0L

        if ( ! stateMachineRunning ) {
            return
        }

        // Check if response is about BILL_ACCEPTOR
        if ( response.cmd != EventType.FW_BILL_ACCEPTOR.command ) {
            return
        }

        if (response.ret == EventResponse.ERROR ) {
            Timber.e("ERROR: ${response}")
            inErrorStateCounter++
            return
        }

        if (response.ret == EventResponse.BUSY ) {
            if ( ++inBusyStateCounter > BUSY_LIMIT_COUNTER ) {
               // TODO: Avaliar o que fazer
            }
            return
        }

        if (response.ret == EventResponse.OK ) {
            inBusyStateCounter = 0

            when (response.action ) {
                Event.ON -> {
                    sendCommandToDevice(DeviceCommand.QUESTION)
                }
                Event.OFF -> {
                    sendCommandToDevice(DeviceCommand.QUESTION)
                }

                Event.QUESTION -> {
                    when(response.status) {
                        Event.ON -> receivedState = DeviceState.ON
                        Event.OFF -> receivedState = DeviceState.OFF
                    }
                    // Quando receber uma resposta com um valor > 0
                    // Vamos mandar desligar o noteiro e quando ele estiver desligado e com
                    // valor > 0 vamos enviar um comando de reset. Quando recebermos a resposta
                    // do Reset com o valor ZERADO vamos contabilizar o valor armazenado em
                    // receivedValue
                    if ( response.value > 0 ) {
                        if ( receivedValue > 0) {
                            if ( receivedValue != response.value) {
                                println("ATENÇÃO: AAA Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                            }
                        }
                        receivedValue = response.value
                        resetCredits()
                    }
                }
                Event.RESET -> {
                    if ( response.value > 0 ) {
                        println("ATENÇÃO: BBB Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                    }

                    if ( receivedValue > 0 ) {
                        sendCreditToController(receivedValue)
                        receivedValue = 0

                        if ( (mainActivity as MainActivity).checkBoxBillAcceptorAutomatic.isChecked) {
                            desiredState = DeviceState.ON
                            sendCommandToDevice(DeviceCommand.ON)
                        }
                    }
                }
                Event.SIMULA5REAIS  -> {
                    Timber.i("Not processes : ${response.action}")
                    sendCommandToDevice(DeviceCommand.QUESTION)
                }
                Event.SIMULA10REAIS  -> {
                    Timber.i("Not processes : ${response.action}")
                    sendCommandToDevice(DeviceCommand.QUESTION)
                }
                Event.SIMULA50REAIS  -> {
                    Timber.i("Not processes : ${response.action}")
                    sendCommandToDevice(DeviceCommand.QUESTION)
                }
                else -> {
                    Timber.e("Invalid Response from BillAcceptor")
                }
            }
        } else {

            Timber.e("Invalid ret in response of mcd:${response.cmd} : ret:${response.ret} action:${response.action} ")
            if ( response.action == Event.SIMULA5REAIS) {
                mostraEmHistory("REJEITOU NOTA 5")
            }
            if ( response.action == Event.SIMULA10REAIS) {
                mostraEmHistory("REJEITOU NOTA 10")
            }
            if ( response.action == Event.SIMULA50REAIS) {
                mostraEmHistory("REJEITOU NOTA 50")
            }
        }

        // Sempre vamos resetar o tempo da execução automatica
        if ( resetTimer > 0 ) {
            deviceChecking(resetTimer)
        }
    }


    fun sendCommandToDevice(cmd : DeviceCommand) {

        when (cmd) {
            DeviceCommand.OFF -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.OFF)
            }
            DeviceCommand.ON -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.ON)
                turnOnTimeStamp = Date().time.toString()
            }
            DeviceCommand.QUESTION -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.QUESTION)
            }
            DeviceCommand.RESET -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.RESET)
            }
            DeviceCommand.SIMULA5REAIS -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA5REAIS)
            }
            DeviceCommand.SIMULA10REAIS -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA10REAIS)
            }
            DeviceCommand.SIMULA50REAIS -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA50REAIS)
            }
            else -> {
                println("Invalid cmd to device")
            }
        }
    }


    private fun sendCreditToController(value : Int) {
        Timber.i("CREDITAR ${value}") // TODO: integrar com interface
        mostraEmHistory("CREDITAR ${value}")
    }

    private fun resetCredits() {
        Timber.i("resetCredits  Desired:${desiredState}  receivedState:${receivedState}")
        if ( desiredState == DeviceState.RESET ) {
            if ( receivedValue == 0) {
                Timber.e("Estavamos aguardando zerar receivedValue. Como ZEROU vamos mandar colocar estado target = OFF")
                desiredState = DeviceState.OFF
//                sendCommandToDevice(DeviceCommand.OFF)
                sendCommandToDevice(DeviceCommand.QUESTION)
            } else {
                // Só vamos aceitar credito quando recebermos um valor no estado OFF
                if ( receivedState == DeviceState.OFF) {
                    Timber.i("Estando no estado OFF, podemos mandar RESET")
                    sendCommandToDevice(DeviceCommand.RESET)
                } else {
                    Timber.e("Ainda não esta no estado OFF, vamos tentar desligar de novo")
                    sendCommandToDevice(DeviceCommand.OFF)
                }
            }
        } else {
            Timber.i("Vamos iniciar modo RESET enviando um OFF e um QUESTION")
            sendCommandToDevice(DeviceCommand.OFF)
            desiredState = DeviceState.RESET
        }
    }
}

