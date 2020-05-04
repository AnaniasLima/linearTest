package com.example.lineartest

import android.content.Context
import android.hardware.usb.UsbManager
import android.opengl.GLES32
import android.opengl.GLES32.GL_DEBUG_SOURCE_API
import android.opengl.GLES32.glDebugMessageControl
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.lineartest.DataModel.Event
import com.example.lineartest.DataModel.EventType
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.microedition.khronos.opengles.GL10.GL_DONT_CARE


class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
//            Timber.plant(MyDebugTree())
            Timber.plant(Timber.DebugTree())
        }

        //
        // ----- ArduinoSerialDevice
        //
        ArduinoSerialDevice.usbManager =
            applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        ArduinoSerialDevice.myContext = applicationContext
        ArduinoSerialDevice.mainActivity = this
        ArduinoSerialDevice.usbSetFilters()
        ArduinoSerialDevice.usbSerialContinueChecking()

        //
        // ----- BillAcceptor
        //
        BillAcceptor.context = applicationContext
        BillAcceptor.mainActivity = this
//        BillAcceptor.StartStateMachine()


        setContentView(R.layout.activity_main)
        btnBillAcceptorOn.setOnClickListener(this)
        btnBillAcceptorOff.setOnClickListener(this)
        btnBillAcceptorQuestion.setOnClickListener(this)
        btnBillAcceptorReset.setOnClickListener(this)
        btnBillAcceptorStartMachine.setOnClickListener(this)
        btnBillAcceptorStopMachine.setOnClickListener(this)


        btnLogClear.setOnClickListener(this)
        btnLogTag.setOnClickListener(this)

        btn5reais.setOnClickListener(this)
        btn10reais.setOnClickListener(this)
        btn20reais.setOnClickListener(this)
        btn50reais.setOnClickListener(this)


        btnLedOn.setOnClickListener(this)
        btnLedOff.setOnClickListener(this)
        btnLedOnOff.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val btnName = (v as Button).text.toString()
        when (v){
            btnLogClear -> {
                stringTextLog = ""
                stringTextHistory = ""
                textLog.setText(stringTextLog)
                textHistory.setText(stringTextHistory)
            }

            btnLogTag -> {
                mostraNaTela("")
                mostraNaTela("")
                mostraEmHistory("")
                mostraEmHistory("")
            }

            btn5reais -> {
                BillAcceptor.fakeBillAccept(5)
                mostraEmHistory("Nota 5")
            }
            btn10reais -> {
                BillAcceptor.fakeBillAccept(10)
                mostraEmHistory("Nota 10")
            }

            btn20reais -> {
                BillAcceptor.fakeBillAccept(20)
                mostraEmHistory("Nota 20")
            }

            btn50reais -> {
                BillAcceptor.fakeBillAccept(50)
                mostraEmHistory("Nota 50")
            }

            btnLedOn -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_LED, Event.ON)
            }
            btnLedOff -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_LED, Event.OFF)
            }
            btnLedOnOff -> {
                ArduinoSerialDevice.requestToSend(EventType.FW_LED, Event.ON)
                ArduinoSerialDevice.requestToSend(EventType.FW_LED, Event.OFF)
            }

            btnBillAcceptorOn -> BillAcceptor.SendTurnOn()
            btnBillAcceptorOff -> BillAcceptor.SendTurnOff()
            btnBillAcceptorQuestion -> BillAcceptor.SendQuestion()
            btnBillAcceptorReset -> BillAcceptor.SendReset()
            btnBillAcceptorStartMachine -> BillAcceptor.StartStateMachine()
            btnBillAcceptorStopMachine -> BillAcceptor.StopStateMachine()

            else -> {
                println("Outro ${btnName}")
                btnBillAcceptorColor.setBackgroundResource(R.drawable.yellow_bill_acceptor)
            }
        }
    }



    // ------------- textLog --------------
    private var stringTextLog: String = ""
    private var mostraNaTelaHandler = Handler()
    private var updateMostraNaTela = Runnable {
        textLog.setText(stringTextLog)
    }

    fun mostraNaTela(str:String) {
        val strHora1 = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        val newString = "$strHora1 - $str"

        Timber.i(newString)

        stringTextLog = "  $newString\n$stringTextLog"

        mostraNaTelaHandler.removeCallbacks(updateMostraNaTela)
        mostraNaTelaHandler.postDelayed(updateMostraNaTela, 10)
    }


    // ------------- textHistory --------------
    private var stringTextHistory: String = ""
    private var mostraEmHistoryHandler = Handler()
    private var updateEmHistory = Runnable {
        textHistory.setText(stringTextHistory)
    }
    fun mostraEmHistory(str:String) {
        val strHora1 = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        val newString = "$strHora1 - $str"

        Timber.i(newString)

        stringTextHistory = "  $newString\n$stringTextHistory"

        mostraEmHistoryHandler.removeCallbacks(updateEmHistory)
        mostraEmHistoryHandler.postDelayed(updateEmHistory, 10)
    }


}
