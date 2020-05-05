package com.example.lineartest

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import com.example.lineartest.DataModel.Event
import com.example.lineartest.DataModel.EventType
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(MyDebugTree())
//            Timber.plant(Timber.DebugTree())
        }

        Timber.i("AAA")
        Timber.v("BBB")
        Timber.e("CCC")
        Timber.d("DDD")

        //
        // ----- ArduinoDevice
        //
        ArduinoDevice.usbManager =
            applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        ArduinoDevice.myContext = applicationContext
        ArduinoDevice.mainActivity = this
        ArduinoDevice.usbSetFilters()
        ArduinoDevice.usbSerialContinueChecking()

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

        btn15.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onClick(v: View?) {
        val btnName = (v as Button).text.toString()
        when (v){
            btnLogClear -> {
                stringTextLog = ""
                stringTextHistory = ""
                stringTextResult = ""
                textLog.setText(stringTextLog)
                textHistory.setText(stringTextHistory)
                textResult.setText(stringTextResult)
                valorAcumulado = 0
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
                ArduinoDevice.requestToSend(EventType.FW_LED, Event.ON)
            }
            btnLedOff -> {
                ArduinoDevice.requestToSend(EventType.FW_LED, Event.OFF)
            }
            btnLedOnOff -> {
                ArduinoDevice.requestToSend(EventType.FW_LED, Event.ON)
                ArduinoDevice.requestToSend(EventType.FW_LED, Event.OFF)
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


    // ------------- textResult--------------
    private var stringTextResult: String = ""
    private var valorAcumulado: Int = 0
    private var mostraEmResultHandler = Handler()
    private var updateEmResult = Runnable {
        textResult.setText(stringTextResult)
    }
    fun mostraEmResult(valor: Int) {
        valorAcumulado += valor
        stringTextResult = "R$ ${valorAcumulado},00 "
        Timber.i("Total Recebido: ${stringTextResult}")
        mostraEmHistoryHandler.removeCallbacks(updateEmResult)
        mostraEmHistoryHandler.postDelayed(updateEmResult, 10)
    }



}

//class TTT (val ipAddress:String, val port: Int ) : Thread() {
//    lateinit var xxx : Socket
//    lateinit var yyy: DataOutputStream
//
//    override fun run() {
//        try  {
//            xxx = Socket(ipAddress, port)
//            yyy = DataOutputStream(xxx.getOutputStream())
//            yyy.writeUTF("abc\r\n")
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }
//}

