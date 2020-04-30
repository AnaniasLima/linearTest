package com.example.lineartest

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
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
        btnBillAcceptorStartMachine.setOnClickListener(this)


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
                stringGiganteMostraNaTela = ""
                textView.setText(stringGiganteMostraNaTela)
            }
            btnLogTag -> {
                mostraNaTela("")
                mostraNaTela("")
            }

            btn5reais -> BillAcceptor.fakeBillAccept(5)
            btn10reais -> BillAcceptor.fakeBillAccept(10)
            btn50reais -> BillAcceptor.fakeBillAccept(50)

            btnBillAcceptorOn -> BillAcceptor.SendTurnOn()
            btnBillAcceptorOff -> BillAcceptor.SendTurnOff()
            btnBillAcceptorQuestion -> BillAcceptor.SendQuestion()
            btnBillAcceptorStartMachine -> BillAcceptor.StartStateMachine()

            else -> {
                println("Outro ${btnName}")
                btnBillAcceptorColor.setBackgroundResource(R.drawable.yellow_bill_acceptor)
            }

        }
    }



    private var stringGiganteMostraNaTela: String = ""
    private var mostraNaTelaHandler = Handler()
    private var updateMostraNaTela = Runnable {
        textView.setText(stringGiganteMostraNaTela)
    }
    fun mostraNaTela(str:String) {
        val strHora1 = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        val newString = "$strHora1 - $str"

        Timber.i(newString)

        stringGiganteMostraNaTela = "  $newString\n$stringGiganteMostraNaTela"

        mostraNaTelaHandler.removeCallbacks(updateMostraNaTela)
        mostraNaTelaHandler.postDelayed(updateMostraNaTela, 10)
    }

}
