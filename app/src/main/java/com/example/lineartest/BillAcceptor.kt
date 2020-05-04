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
    SIMULA20REAIS,
    SIMULA50REAIS
}


@SuppressLint("StaticFieldLeak")
object BillAcceptor {
    private const val WAIT_WHEN_OFFLINE = 5000L
    private const val WAIT_TIME_TO_QUESTION = 10000L
    private const val WAIT_TIME_TO_RESPONSE = 300L
    private const val BUSY_LIMIT_COUNTER = 10

    var mainActivity: AppCompatActivity? = null
    var context: Context? = null

    private var billAcceptorHandler = Handler()

    private var desiredState : DeviceState = DeviceState.OFF
    private var inBusyStateCounter = 0
    private var inErrorStateCounter = 0         // TODO: Teoricamente nunca deve acontecer. Vamos criar uma forma de tratar caso fique > 0

    private var receivedState: DeviceState = DeviceState.UNKNOW

    private var receivedValue = 0  // when receive values store here until

    private var turnOnTimeStamp: String = ""
    private var stateMachineRunning = false


    private fun mostraNaTela(str:String) {
        (mainActivity as MainActivity).mostraNaTela(str)
    }

    private fun mostraEmHistory(str:String) {
        (mainActivity as MainActivity).mostraEmHistory(str)
    }

    private fun mostraEmResult(valor : Int) {
        (mainActivity as MainActivity).mostraEmResult(valor)
    }

    fun fakeBillAccept(value: Int) {
        when (value) {
            5 -> sendCommandToDevice(DeviceCommand.SIMULA5REAIS)
            10 -> sendCommandToDevice(DeviceCommand.SIMULA10REAIS)
            20 -> sendCommandToDevice(DeviceCommand.SIMULA20REAIS)
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
        var dropLog= false

        if ( stateMachineRunning ) {
            var delayToNext = delay

            if ( ! ConnectThread.isConnected ) {
                delayToNext = WAIT_WHEN_OFFLINE
            } else {
                if ( delayToNext == 0L ) {
                    delayToNext = WAIT_TIME_TO_QUESTION
                    dropLog = true
                }
            }

            if ( ! dropLog) {
                Timber.i("agendando deviceChecking %s (%s)",
                    SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time.time.plus(delayToNext)) ,
                    delayToNext.toString())
            }

            billAcceptorHandler.removeCallbacks(deviceCheckRunnable)
            billAcceptorHandler.postDelayed(deviceCheckRunnable, delayToNext)
        }
    }

    private var deviceCheckRunnable = Runnable {

        if ( receivedState == desiredState ) {
            Timber.i("receivedState=%s  desiredState=%s", receivedState, desiredState)
            deviceChecking(WAIT_TIME_TO_QUESTION ) // Ao receber a resposta de QUESTION vai agendar um novo QUESTION
            sendCommandToDevice(DeviceCommand.QUESTION)
        } else {
            when (desiredState) {
                DeviceState.RESET -> {
                    resetCredits()
                }
                DeviceState.ON -> {
                    sendCommandToDevice(DeviceCommand.ON)
                }
                DeviceState.OFF -> {
                    sendCommandToDevice(DeviceCommand.OFF)
                }
                else -> {
                    desiredState = DeviceState.ON
                    sendCommandToDevice(DeviceCommand.ON)
                    println("ATENÇÃO: CCC Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                }
            }
        }
    }



    fun processReceivedResponse(response : EventResponse) {

        // Check if response is about BILL_ACCEPTOR
        if ( response.cmd != EventType.FW_BILL_ACCEPTOR.command ) {
            return
        }

        when ( response.ret ) {
            EventResponse.ERROR -> {
                if ( response.action == Event.SIMULA5REAIS) {
                    mostraEmHistory("REJEITOU NOTA 5")
                }
                else if ( response.action == Event.SIMULA10REAIS) {
                    mostraEmHistory("REJEITOU NOTA 10")
                }
                else if ( response.action == Event.SIMULA20REAIS) {
                    mostraEmHistory("REJEITOU NOTA 20")
                }
                else if ( response.action == Event.SIMULA50REAIS) {
                    mostraEmHistory("REJEITOU NOTA 50")
                } else {
                    inErrorStateCounter++
                    Timber.e("ERROR($inErrorStateCounter): ${response}")
                }
                return
            }
            EventResponse.BUSY -> {
                if ( ++inBusyStateCounter > BUSY_LIMIT_COUNTER ) {
                    // TODO: Avaliar o que fazer
                }
                return
            }
            EventResponse.OK -> {
                if ( ! stateMachineRunning ) {
                    return
                }

                inBusyStateCounter = 0

                when (response.action ) {
                    Event.ON -> {
                        // Lets check if command ON turn on the Bill Acceptor
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.OFF -> {
                        // Lets check if command OFF turn off the Bill Acceptor
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }

                    Event.QUESTION -> {
                        when(response.status) {
                            Event.ON -> {
                                mainActivity?.runOnUiThread(java.lang.Runnable {
                                    (mainActivity as MainActivity).btnBillAcceptorColor.setBackgroundResource(R.drawable.green_bill_acceptor)
                                })
                                receivedState = DeviceState.ON
                            }
                            Event.OFF -> {
                                mainActivity?.runOnUiThread(java.lang.Runnable {
                                    (mainActivity as MainActivity).btnBillAcceptorColor.setBackgroundResource(R.drawable.red_bill_acceptor)
                                })
                                receivedState = DeviceState.OFF
                            }
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

                        // Podemos reagendar o proximo question automatico
                        deviceChecking(0L)

                    }
                    Event.RESET -> {
                        if ( response.value > 0 ) {
                            println("ATENÇÃO: BBB Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                        }

                        if ( receivedValue > 0 ) {
                            sendCreditToController(receivedValue)
                            receivedValue = 0
                        }

                        if ( (mainActivity as MainActivity).checkBoxBillAcceptorAutomatic.isChecked) {
                            desiredState = DeviceState.ON
                            sendCommandToDevice(DeviceCommand.ON)
                        }

                    }
                    Event.SIMULA5REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.SIMULA10REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.SIMULA20REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.SIMULA50REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    else -> {
                        Timber.e("Invalid Response from BillAcceptor")
                    }
                }
            }
        }
    }


    fun sendCommandToDevice(cmd : DeviceCommand) {

        when (cmd) {
            DeviceCommand.OFF -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.OFF)
            }
            DeviceCommand.ON -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.ON)
                turnOnTimeStamp = Date().time.toString()
            }
            DeviceCommand.QUESTION -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.QUESTION)
            }
            DeviceCommand.RESET -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.RESET)
            }
            DeviceCommand.SIMULA5REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA5REAIS)
            }
            DeviceCommand.SIMULA10REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA10REAIS)
            }
            DeviceCommand.SIMULA20REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA20REAIS)
            }
            DeviceCommand.SIMULA50REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA50REAIS)
            }
        }
    }


    private fun sendCreditToController(value : Int) {
        Timber.i("CREDITAR ${value}") // TODO: integrar com interface
        mostraEmHistory("CREDITAR ${value}")
        mostraEmResult(value)

    }

    private fun resetCredits() {
        if ( desiredState != DeviceState.RESET ) {
            Timber.i("Vamos iniciar RESET desligando o noteiro.  Enviando OFF. (Modo atual: ${receivedState})")
            sendCommandToDevice(DeviceCommand.OFF)
            desiredState = DeviceState.RESET
        } else {
            Timber.i("executando procedimento para resetCredits.  Desired:${desiredState}  receivedState:${receivedState}")
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
        }
    }
}

