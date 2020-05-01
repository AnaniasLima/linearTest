package com.example.lineartest

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
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

        btnEchoSend.setOnClickListener(this)
        btnEchoReceive.setOnClickListener(this)
        btnLogClear.setOnClickListener(this)
        btnLogTag.setOnClickListener(this)

        btn5reais.setOnClickListener(this)
        btn10reais.setOnClickListener(this)
        btn50reais.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        val btnName = (v as Button).text.toString()
        when (v){
            btnEchoSend -> {
                if ( ArduinoSerialDevice.getLogLevel(FunctionType.FX_TX) == 0) {
                    btnEchoSend.text = getString(R.string.sendOff)
                    ArduinoSerialDevice.setLogLevel(FunctionType.FX_TX, 1)
                }  else {
                    btnEchoSend.text = getString(R.string.sendOn)
                    ArduinoSerialDevice.setLogLevel(FunctionType.FX_TX, 0)
                }
            }
            btnEchoReceive -> {
                if ( ArduinoSerialDevice.getLogLevel(FunctionType.FX_RX) == 0) {
                    btnEchoReceive.text = getString(R.string.receiveOff)
                    ArduinoSerialDevice.setLogLevel(FunctionType.FX_RX, 1)
                }  else {
                    btnEchoReceive.text = getString(R.string.receiveOn)
                    ArduinoSerialDevice.setLogLevel(FunctionType.FX_RX, 0)
                }
            }
            btnLogClear -> {
                stringTextLog = ""
                textLog.setText(stringTextLog)
            }
            btnLogTag -> {
                mostraNaTela("")
                mostraNaTela("")
            }

            btn5reais -> {
                if ( BillAcceptor.isEnabled() ) {
                    BillAcceptor.fakeBillAccept(5)
                } else {
                    Toast.makeText(this, "Noteiro desabilitado", Toast.LENGTH_SHORT).show()
                }
            }
            btn10reais -> {
                if ( BillAcceptor.isEnabled() ) {
                    BillAcceptor.fakeBillAccept(10)
                } else {
                    Toast.makeText(this, "Noteiro desabilitado", Toast.LENGTH_SHORT).show()
                }
            }

            btn50reais -> {
                if ( BillAcceptor.isEnabled() ) {
                    BillAcceptor.fakeBillAccept(50)
                } else {
                    Toast.makeText(this, "Noteiro desabilitado", Toast.LENGTH_SHORT).show()
                }
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
