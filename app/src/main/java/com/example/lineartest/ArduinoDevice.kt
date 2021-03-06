package com.example.lineartest

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.lineartest.DataModel.Event
import com.example.lineartest.DataModel.EventResponse
import com.example.lineartest.DataModel.EventType
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


enum class FunctionType {
    FX_RX,
    FX_TX
}


@SuppressLint("StaticFieldLeak")
object ArduinoDevice {

    const val ACTION_USB_PERMISSION = "permission"
    private const val USB_SERIAL_REQUEST_INTERVAL = 30000L
    private const val USB_SERIAL_TIME_TO_CONNECT_INTERVAL = 10000L

    var mainActivity: AppCompatActivity? = null
    var myContext: Context? = null
    var usbManager  : UsbManager? = null

    private var usbSerialRequestHandler = Handler()
//    private var EVENT_LIST: MutableList<Event> = mutableListOf()
    private var connectThread: ConnectThread? = null
    private var rxLogLevel = 0
    private var txLogLevel = 0


    private fun mostraNaTela(str:String) {
        (mainActivity as MainActivity).mostraNaTela(str)
    }

    private fun mostraEmHistory(str:String) {
        (mainActivity as MainActivity).mostraEmHistory(str)
    }


    private var usbSerialRunnable = Runnable {
        if ( ConnectThread.isConnected ) {
//            mostraNaTela("usbSerialRunnable Conectado")
        } else {
            mostraNaTela("usbSerialRunnable NAO Conectado")
            connect()
        }

        usbSerialContinueChecking()
    }


    fun usbSerialContinueChecking() {
        var delayToNext: Long = USB_SERIAL_REQUEST_INTERVAL

        if ( ! ConnectThread.isConnected ) {
            delayToNext = USB_SERIAL_TIME_TO_CONNECT_INTERVAL
            mostraNaTela("agendando proximo STATUS_REQUEST para:---" + SimpleDateFormat("HH:mm:ss").format(
                Calendar.getInstance().time.time.plus(delayToNext)) + "(" + delayToNext.toString() + ")")
        }

        usbSerialRequestHandler.removeCallbacks(usbSerialRunnable)
        usbSerialRequestHandler.postDelayed(usbSerialRunnable, delayToNext)
    }

    fun usbSerialImediateChecking(delayToNext: Long) {

        mostraNaTela("agendando STATUS_REQUEST para:---" + SimpleDateFormat("HH:mm:ss").format(
            Calendar.getInstance().time.time.plus(delayToNext)) + "(" + delayToNext.toString() + ")")

        usbSerialRequestHandler.removeCallbacks(usbSerialRunnable)
        usbSerialRequestHandler.postDelayed(usbSerialRunnable, delayToNext)
    }

    var lastDif = 0

    fun onEventResponse(eventResponse: EventResponse) {

        val dif = eventResponse.packetNumber.toInt() - eventResponse.numPktResp.toInt()

        if ( dif != lastDif) {
            Timber.e("========= Perdeu pacote ======")
            lastDif = dif
            mostraEmHistory("Perdeu ${lastDif} pacotes (${eventResponse.packetNumber})")
        }

        when ( eventResponse.eventType ) {
            EventType.FW_PLAY -> {
                Timber.e("=============== FW_PLAY =======================: ${eventResponse.toString()}")
            }
            EventType.FW_BILL_ACCEPTOR -> {
//                Timber.e("FW_BILL_ACCEPTOR =====> ${eventResponse.toString()}")
                BillAcceptor.processReceivedResponse(eventResponse)
            }
            EventType.FW_LED -> {
//                Timber.e("FW_LED =====> ${eventResponse.toString()}")
            }
            else -> {
                println("===> Falta tratar resposta para comando ${eventResponse.eventType} ")
            }
        }
    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if ( intent != null && usbManager != null) {
                mostraNaTela("WWW------------------------- intent.action = " + intent.action.toString())
                when (intent.action!!) {
                    ACTION_USB_PERMISSION -> {
                        val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                        mostraNaTela("ACTION_USB_PERMISSION------------------------- Permmissao concedida = ${granted.toString()}")
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        mostraNaTela("ACTION_USB_DEVICE_ATTACHED")
                        connect()
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        mostraNaTela("ACTION_USB_DEVICE_DETACHED")
                        disconnect()
                    }
                }
            }
        }
    }

    fun connect() {
        mostraNaTela("Verificando conexão...")

        if ( ConnectThread.isConnected ) {
            if ( connectThread == null) {
                throw IllegalStateException("Erro interno 001")
            }
            mostraNaTela("Já estava connectado.")
            return
        }

        if ( usbManager != null ) {
            if ( usbManager!!.deviceList.size > 0  ) {
                mostraNaTela("Tentando connect...")
                connectThread = ConnectThread(ConnectThread.CONNECT, usbManager!!, mainActivity!!, myContext!!)
                if (connectThread != null ) {
                    Timber.i("Startando thread para tratar da conexao")
                    connectThread!!.priority = Thread.MAX_PRIORITY

                    connectThread!!.start()

                } else {
                    Timber.e("Falha na criação da thread ")
                }
            }
        }
    }



    fun disconnect() {
        mostraNaTela("Vai verificar usbSerialDevice em disconnect...")
        if ( connectThread != null ) {
            Timber.i("connectThread not null em disconnect vamos chamar finish")
            connectThread!!.finish()
            Timber.i("fazendo connectThread = NULL")
            connectThread = null
        } else {
            Timber.i("Disparando thread para desconectar")
            ConnectThread(ConnectThread.DISCONNECT, usbManager!!, mainActivity!!, myContext!!).start()
        }
    }


    fun requestToSend(eventType: EventType, action: String) : Boolean {
        var ret = false

        if ( ConnectThread.isConnected ) {
            try {
                when(eventType) {
                    EventType.FW_STATUS_RQ -> {
                        ret = connectThread!!.requestToSend(eventType = EventType.FW_STATUS_RQ, action=action)
                    }
                    EventType.FW_BILL_ACCEPTOR -> {
                        ret = connectThread!!.requestToSend(eventType = EventType.FW_BILL_ACCEPTOR, action=action)
                    }
                    EventType.FW_LED -> {
                        ret = connectThread!!.requestToSend(eventType = EventType.FW_LED, action=action)
                    }
                    else -> {
                        // do nothing
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ret
    }

    fun usbSetFilters() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        myContext!!.registerReceiver(broadcastReceiver, filter)
    }



    fun getLogLevel(function : FunctionType) : Boolean {
        when ( function) {
            FunctionType.FX_RX -> {
                return  ( (mainActivity as MainActivity).checkBoxLogRX.isChecked )
            }
            FunctionType.FX_TX -> {
                return  ( (mainActivity as MainActivity).checkBoxLogTX.isChecked )
            }
        }
    }

}