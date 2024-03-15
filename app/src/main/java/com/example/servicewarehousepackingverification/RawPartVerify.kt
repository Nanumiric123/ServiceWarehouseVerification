package com.example.servicewarehousepackingverification

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings.Global
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import androidx.core.view.size
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class RawPartVerify : AppCompatActivity() {
    private lateinit var mainLinear:LinearLayout
    private lateinit var c:Context
    private lateinit var scannedItems:MutableList<SCANNED_DATA>
    private lateinit var badgeString:String
    private lateinit var picklistBarcode: EditText
    private lateinit var pb: ProgressBar
    private lateinit var clearButton:Button
    private lateinit var CF:CommonFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raw_part_verify)
        mainLinear = findViewById(R.id.RMPartVerifyMain)
        c = this
        clearButton = findViewById(R.id.RMBtnClear)
        badgeString = intent.getStringExtra("BadgeNum").toString()
        scannedItems = mutableListOf()
        pb = findViewById(R.id.RMProgressBar)
        picklistBarcode = findViewById(R.id.edRMBarcode)
        picklistBarcode.requestFocus()
        CF = CommonFunctions()

        clearButton.setOnClickListener {
            var greenCount = 0
            for (i in 0 until mainLinear.size ){
                var k = mainLinear[i]
                var table = findViewById<TableLayout>(k.id)

                for (j in 1 until table.size ){
                    var curRow = findViewById<TableRow>(table[j].id)
                    var VerifyLL = findViewById<LinearLayout>(curRow[0].id)
                    var llbackground = VerifyLL.background
                    if (llbackground is ColorDrawable) {
                        val backgroundColor = llbackground.color
                        if(backgroundColor == -259){
                            break;
                        }
                        else if (backgroundColor == -16711936){
                            greenCount++
                        }
                        else{

                        }

                    }
                }
                if (greenCount == (table.size - 1)){
                    clearButton.isEnabled = true
                    picklistBarcode.text.clear()
                }
                else{
                    val clearDialog = AlertDialog.Builder(c)
                    clearDialog.setMessage("Scan all material before clearing the Picklist number\n Scan semua bagi hijau dulu baru tekan clear buat kerja bagi habis")
                    clearDialog.setTitle("Error")
                    clearDialog.setNegativeButton("Back") { dialog, which ->

                    }
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        clearDialog.show()
                    })
                }
            }

        }
        picklistBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                //Perform Code
                var picklistdata: MutableList<PickList> = mutableListOf()
                runBlocking {
                    val job = GlobalScope.launch {
                        runOnUiThread {
                            progressBar()
                        }

                        try{
                            picklistdata = generatePickList(picklistBarcode.text.toString())
                        }
                        catch (ex:Exception){
                            val exceptionDialog = AlertDialog.Builder(c)
                            exceptionDialog.setMessage(ex.message.toString())
                            exceptionDialog.setTitle("Error")
                            exceptionDialog.setNegativeButton("Back") { dialog, which ->

                            }
                            runOnUiThread(kotlinx.coroutines.Runnable {
                                exceptionDialog.show()
                            })
                            picklistBarcode.requestFocus()
                            picklistBarcode.text.clear()
                        }

                    }
                    job.join()
                    try{
                        runOnUiThread {
                            // Stuff that updates the UI
                            if (mainLinear.size > 0) {
                                mainLinear.removeAllViews()
                            }
                            mainLinear.addView(buildTable(picklistdata))

                        }
                    }
                    catch (ex:Exception){
                        val exceptionDialog = AlertDialog.Builder(c)
                        exceptionDialog.setMessage(ex.message.toString())
                        exceptionDialog.setTitle("Error")
                        exceptionDialog.setNegativeButton("Back") { dialog, which ->

                        }
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            exceptionDialog.show()
                        })
                    }
                    runOnUiThread {
                        progressBar()
                    }
                }


                return@OnKeyListener true
            }
            false
        })
        

    }


    private fun buildTable(data: MutableList<PickList>): View {
        var tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.MATCH_PARENT
        )
        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams) {
            weight = 1F
        }
        var mainTable = TableLayout(c)
        mainTable.id = View.generateViewId()
        mainTable.layoutParams = tableParam
        //Add Header
        var headerRow = TableRow(c)
        headerRow.layoutParams = TableRow.LayoutParams(rowParams)
        headerRow.id = View.generateViewId()


        headerRow.addView(CF.generateTVforRow("Verify",c))
        headerRow.addView(CF.generateTVforRow("Material",c))
        headerRow.addView(CF.generateTVforRow("Quantity",c))
        headerRow.addView(CF.generateTVforRow("Batch",c))

        mainTable.addView(headerRow)
        for (i in 0 until data.size) {
            mainTable.addView(generateRowView(data[i],scannedItems))
        }

        return mainTable
    }


    private fun generateRowView(rowLine: PickList,itemsScanned:MutableList<SCANNED_DATA>): View {
        var scannedMat = itemsScanned.filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }

        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams) {
            weight = 1F
        }
        val row = TableRow(c)
        row.layoutParams = rowParams
        row.id = View.generateViewId()

        val edverifyContainerLinearHoriz = LinearLayout(c)
        with(edverifyContainerLinearHoriz) {
            id = View.generateViewId()
            layoutParams = rowParams
            setBackgroundResource(R.drawable.border)
        }
        val edVerify = EditText(c)
        with(edVerify) {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Scanned RM = " + scannedMat[0].SCANNED_QUANTITY.toString()
        }

        if(!scannedMat.isNullOrEmpty()){
            if(scannedMat[0].STATUS == "Y"){
                edverifyContainerLinearHoriz.setBackgroundColor(Color.YELLOW)
            }
            else if (scannedMat[0].STATUS == "NA"){

            }
            else {
                edverifyContainerLinearHoriz.setBackgroundColor(Color.GREEN)
            }
        }

        edVerify.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                //Perform Code
                var lMaterial = ""
                var fResult = ""
                var lqty = 0
                var cartonNum = ""
                var translatedBarcode: PASMYBarcode? = null
                if(edVerify.text.toString().contains('(') && edVerify.text.toString().contains(')')) {
                    runBlocking {
                        val job = GlobalScope.launch {
                            translatedBarcode = CF.translateBarcodeRP(edVerify.text.toString())
                        }
                        job.join()
                        lMaterial = translatedBarcode!!.MATERIAL_NO
                        cartonNum = translatedBarcode!!.CARTON_NO
                        lqty = translatedBarcode!!.QTY.toInt()
                    }

                }
                else if (picklistBarcode.text.contains('$')) {
                    lMaterial = edVerify.text.toString().split('$')[0]
                    lqty = edVerify.text.toString().split('$')[1].toInt()
                }
                else {
                    lMaterial = edVerify.text.toString()
                    lqty = 1
                }

                runBlocking {
                    val job = GlobalScope.launch {
                        runOnUiThread {
                            progressBar()
                        }

                        if (lMaterial == rowLine.MATERIAL_NO) {
                            var scannedMat = CF.retrieveScannedData(picklistBarcode.text.toString()).filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }
                            if((scannedMat[0].SCANNED_QUANTITY + lqty) <=
                                scannedMat[0].PICKLIST_QUANTITY) {

                                fResult = saveToDatabase(lMaterial,"",rowLine.BATCH_NO,lqty,cartonNum,badgeString)

                                var iSplit = fResult.split(':')
                                if(iSplit[0].contains('S')){
                                    val scannedDat = CF.retrieveScannedData(picklistBarcode.text.toString()).filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }

                                    runOnUiThread {
                                        edVerify.hint = "Scanned RM = " + scannedDat[0].SCANNED_QUANTITY.toString()
                                        edVerify.text.clear()
                                        if(!scannedDat.isNullOrEmpty()){
                                            if(scannedDat[0].STATUS == "Y"){
                                                edverifyContainerLinearHoriz.setBackgroundColor(Color.YELLOW)
                                            }
                                            else if (scannedDat[0].STATUS == "NA"){

                                            }
                                            else {
                                                edverifyContainerLinearHoriz.setBackgroundColor(Color.GREEN)
                                            }
                                        }
                                        else{

                                        }
                                    }

                                    edVerify.requestFocus()
                                }
                            }
                            else{
                                val wrongMaterial = AlertDialog.Builder(c)
                                wrongMaterial.setMessage("Carton quantity more than Picklist quantity \n Quantity pun tak betul hang scan benda apa ?")
                                wrongMaterial.setTitle("Error")
                                wrongMaterial.setNegativeButton("Back") { dialog, which ->
                                    edVerify.requestFocus()
                                    edVerify.text.clear()
                                }
                                runOnUiThread(kotlinx.coroutines.Runnable {
                                    wrongMaterial.show()
                                })
                            }
                        } else {
                            val wrongMaterial = AlertDialog.Builder(c)
                            wrongMaterial.setMessage("Wrong Material Scanned \n  Material tu salah, Check la label dengan material nak scan")
                            wrongMaterial.setTitle("Error")
                            wrongMaterial.setNegativeButton("Back") { dialog, which ->
                                edVerify.requestFocus()
                                edVerify.text.clear()
                            }
                            runOnUiThread(kotlinx.coroutines.Runnable {
                                wrongMaterial.show()
                            })
                            var scannedMat = itemsScanned.filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }
                            if(scannedMat[0].SCANNED_QUANTITY<=
                                scannedMat[0].PICKLIST_QUANTITY) {
                                edverifyContainerLinearHoriz.setBackgroundColor(Color.YELLOW)
                            }
                            else{
                                edverifyContainerLinearHoriz.setBackgroundColor(Color.RED)
                            }

                        }

                    }
                    job.join()
                    edVerify.requestFocus()
                    runOnUiThread {
                        edVerify.text.clear()
                        progressBar()
                    }
                }



                return@OnKeyListener true
            }
            false
        })

        edverifyContainerLinearHoriz.addView(edVerify)
        row.addView(edverifyContainerLinearHoriz)
        row.addView(CF.generateTVforRow(rowLine.MATERIAL_NO,c))
        row.addView(CF.generateTVforRow(rowLine.PICK_QTY.toInt().toString(),c))
        row.addView(CF.generateTVforRow(rowLine.BATCH_NO,c))

        return row
    }

    private fun saveToDatabase(material:String,custPNo:String,lotNum:String,qty:Int,cartonNo:String,bdgeNo:String):String{
        return CF.saveToDatabase(material,custPNo,lotNum,qty,cartonNo,bdgeNo,picklistBarcode.text.toString())
    }

    private fun generatePickList(doNum:String):MutableList<PickList>{
        var pickList = mutableListOf<PickList>()
        val link: String = "http://172.16.206.19/REST_API/SERVICEPACKING/RetrieveData?p_pl=${doNum}"
        val urlLink: URL = URL(link)
        var jsonText = urlLink.readText()
        var dataArr = JSONTokener(jsonText).nextValue() as JSONArray
        for (i in 0 until dataArr.length()) {
            var tempPL = PickList(
                BATCH_NO = dataArr.getJSONObject(i).getString("LOT_NO"),
                MATERIAL_NO = dataArr.getJSONObject(i).getString("MATERIAL"),
                PICK_QTY = dataArr.getJSONObject(i).getDouble("QUANTITY")
            )
            pickList.add(tempPL)
        }

        scannedItems = CF.retrieveScannedData(picklistBarcode.text.toString())

        return pickList
    }


    private fun progressBar(){
        if(pb.visibility == View.INVISIBLE || pb.visibility == View.GONE){
            pb.visibility = View.VISIBLE
        }
        else{
            pb.visibility = View.INVISIBLE
        }
    }


}

data class PASMYBarcode(
    var VENDOR:String,
    var DATE:String,
    var MATERIAL_NO: String,
    var CARTON_NO:String,
    var LOT:String,
    var QTY:String
)