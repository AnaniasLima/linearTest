package com.example.lineartest

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lineartest.DataModel.Event
import com.example.lineartest.DataModel.EventType
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val MAX_LOG_LINES=300

    var myList = ArrayList<String>()
    var myBackgroundList = ArrayList<String>()

    var historyList = ArrayList<String>()
    var historyBackgroundList = ArrayList<String>()

    val logAdapter = LogAdapter(this, myList)
    val historyAdapter = HistoryAdapter(this, historyList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        if (BuildConfig.DEBUG) {
//            Timber.plant(MyDebugTree())
            Timber.plant(Timber.DebugTree())
        }

        setContentView(R.layout.activity_main)


        spinnerDelayQuestion.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , BillAcceptor.questionDelayList)
        BillAcceptor.setDelayForQuestion(spinnerDelayQuestion.selectedItem.toString())


//        spinnerDelayQuestion.setSelection(BillAcceptor.defaultQuestionDelayOption) // 1000 ms


        spinnerDelayQuestion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Timber.i("Nada foi selecionado")
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                BillAcceptor.setDelayForQuestion(parent!!.getItemAtPosition(pos).toString())
            }
        }




        //
        // ----- ArduinoDevice
        //
        ArduinoDevice.usbManager =
            applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        ArduinoDevice.myContext = applicationContext
        ArduinoDevice.mainActivity = this
        ArduinoDevice.usbSetFilters()
//        ArduinoDevice.usbSerialContinueChecking()
        ArduinoDevice.usbSerialImediateChecking(200)



        //
        // ----- BillAcceptor
        //
        BillAcceptor.context = applicationContext
        BillAcceptor.mainActivity = this
//        BillAcceptor.StartStateMachine()



        log_recycler_view.layoutManager = LinearLayoutManager(this)
        log_recycler_view.adapter = logAdapter

        history_recycler_view.layoutManager = LinearLayoutManager(this)
        history_recycler_view.adapter = historyAdapter


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

//        btn16.setOnClickListener {
//            Thread {
//                for ( contaLinha in  1..500) {
//                    ArduinoDevice.requestToSend(EventType.FW_LED, Event.ON)
//                    ArduinoDevice.requestToSend(EventType.FW_LED, Event.OFF)
//                    Thread.sleep(40)
//                }
//            }.start()
//        }


    }

    override fun onClick(v: View?) {
        val btnName = (v as Button).text.toString()
        when (v){
            btnLogClear -> {
                myList.clear()
                myBackgroundList.clear()
                logAdapter.notifyDataSetChanged()


                historyList.clear()
                historyBackgroundList.clear()
                historyAdapter.notifyDataSetChanged()


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
    private var mostraNaTelaHandler = Handler()
    private var updateMostraNaTela = Runnable {
        val linesInBackgroudList = myBackgroundList.size
        val cota=3

        Thread.currentThread().priority = 1


        if ( myList.size >= MAX_LOG_LINES) {
            val linesToBeDeleted : Int = MAX_LOG_LINES/4
            Timber.i("Deletando $linesToBeDeleted de myList (size atual: ${myList.size})")
            for (line in 0 until linesToBeDeleted) {
                myList.removeAt(0)
            }
            Timber.i("Novo tamanho de myList (${myList.size})")
        }

        if ( linesInBackgroudList > cota) {
            Timber.i("Copiando $linesInBackgroudList de myBackgroundList para myList")
        }

        // Copy lines from myBackgroundList to myList
        for (line in 0 until linesInBackgroudList ) {
            myList.add(myBackgroundList[line])
        }
        logAdapter.notifyDataSetChanged()

        // Remove lines from myBackgroundList to myList
        for (line in 0 until linesInBackgroudList) {
            myBackgroundList.removeAt(0)
        }

        if ( linesInBackgroudList > cota) {
            Timber.i("Removidas $linesInBackgroudList de myBackgroundList")
            Timber.i("Novo tamanho de myBackgroundList = ${myBackgroundList.size}")
        }

//        Thread.currentThread().priority = 1
        log_recycler_view.smoothScrollToPosition(logAdapter.getItemCount() - 1)
    }

    fun mostraNaTela(str:String) {
        val strHora1 = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        val newString = "$strHora1 - $str"

        Timber.i(newString)

        myBackgroundList.add(newString)

        mostraNaTelaHandler.removeCallbacks(updateMostraNaTela)
        mostraNaTelaHandler.postDelayed(updateMostraNaTela, 10)
    }


    // ------------- textHistory --------------
    private var mostraEmHistoryHandler = Handler()

    private var updateEmHistory = Runnable {
        val linesInBackgroudList = historyBackgroundList.size
        val cota=3

        Thread.currentThread().priority = 1

        if ( historyList.size >= MAX_LOG_LINES) {
            val linesToBeDeleted : Int = MAX_LOG_LINES/4
            Timber.i("Deletando $linesToBeDeleted de historyList (size atual: ${historyList.size})")
            for (line in 0 until linesToBeDeleted) {
                historyList.removeAt(0)
            }
            Timber.i("Novo tamanho de historyList (${historyList.size})")
        }

        if ( linesInBackgroudList > cota) {
            Timber.i("Copiando $linesInBackgroudList de historyBackgroundList para historyList")
        }

        // Copy lines from historyBackgroundList to historyList
        for (line in 0 until linesInBackgroudList ) {
            historyList.add(historyBackgroundList[line])
        }
        historyAdapter.notifyDataSetChanged()

        // Remove lines from historyBackgroundList to historyList
        for (line in 0 until linesInBackgroudList) {
            historyBackgroundList.removeAt(0)
        }

        if ( linesInBackgroudList > cota) {
            Timber.i("Removidas $linesInBackgroudList de historyBackgroundList")
            Timber.i("Novo tamanho de historyBackgroundList = ${historyBackgroundList.size}")
        }

//        Thread.currentThread().priority = 1
        history_recycler_view.smoothScrollToPosition(historyAdapter.getItemCount() - 1)
    }

    fun mostraEmHistory(str:String) {
        val strHora1 = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        val newString = "$strHora1 - $str"

        Timber.i(newString)

        historyBackgroundList.add(newString)

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

