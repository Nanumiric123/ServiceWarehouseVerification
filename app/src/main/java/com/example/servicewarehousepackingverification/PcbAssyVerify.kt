package com.example.servicewarehousepackingverification

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class PcbAssyVerify : AppCompatActivity() {

    private lateinit var picklistBarcode:EditText
    private lateinit var mainLayout:LinearLayout
    private lateinit var c: Context
    private lateinit var badgeString:String
    private lateinit var pb:ProgressBar
    private lateinit var clearButton:Button
    private lateinit var scannedItems:MutableList<SCANNED_DATA>
    private lateinit var CF:CommonFunctions

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pcbassy_verify)
        picklistBarcode = findViewById(R.id.edPicklistPCBA)
        mainLayout = findViewById(R.id.PCBAssyHoriz)
        c = this
        scannedItems = mutableListOf()
        picklistBarcode.requestFocus()
        badgeString = intent.getStringExtra("BadgeNum").toString()
        pb = findViewById(R.id.pcbAssyProgressBar)
        clearButton = findViewById(R.id.btnPCBClear)
        CF = CommonFunctions()

        if (mainLayout.size > 0) {
            clearButton.isEnabled = false
        }

        clearButton.setOnClickListener {
            var greenCount = 0
            for (i in 0 until mainLayout.size ){
                var k = mainLayout[i]
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
                    clearDialog.setMessage("Scan all material before clearing the Picklist number !\n Scan semua bagi hijau dulu baru tekan clear, buat kerja bagi habis !")
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
                                picklistBarcode.requestFocus()
                                picklistBarcode.text.clear()
                            })

                        }

                    }
                    job.join()
                    try{
                        runOnUiThread {
                            // Stuff that updates the UI
                            if (mainLayout.size > 0) {
                                mainLayout.removeAllViews()
                            }
                            mainLayout.addView(buildTable(picklistdata))


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
        val tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.MATCH_PARENT
        )
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams) {
            weight = 1F
        }
        val mainTable = TableLayout(c)
        mainTable.id = View.generateViewId()
        mainTable.layoutParams = tableParam
        //Add Header
        val headerRow = TableRow(c)
        headerRow.layoutParams = TableRow.LayoutParams(rowParams)
        headerRow.id = View.generateViewId()

        headerRow.addView(CF.generateTVforRow(c.getString(R.string.headerVerify),c))
        headerRow.addView(CF.generateTVforRow(c.getString(R.string.headerPickQty),c))
        headerRow.addView(CF.generateTVforRow(c.getString(R.string.headerMaterialNo),c))
        headerRow.addView(CF.generateTVforRow(c.getString(R.string.headerBatchNo),c))

        mainTable.addView(headerRow)
        for (i in 0 until data.size) {
            mainTable.addView(generateRowView(data[i],scannedItems))
        }
        return mainTable
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun generateRowView(rowLine: PickList, itemsScanned:MutableList<SCANNED_DATA>): View {
        val scannedMat = itemsScanned.filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }

        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
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
            hint = "Scanned PCBA = " + scannedMat[0].SCANNED_QUANTITY.toString()
        }

        if(scannedMat.isNotEmpty()){
            if(scannedMat[0].STATUS == "Y"){
                edverifyContainerLinearHoriz.setBackgroundColor(Color.YELLOW)
            }
            else if (scannedMat[0].STATUS == "NA"){
                edverifyContainerLinearHoriz.setBackgroundColor(Color.YELLOW)
            }
            else {
                edverifyContainerLinearHoriz.setBackgroundColor(Color.GREEN)
            }
        }

        edVerify.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                runBlocking {
                    val job = GlobalScope.launch {
                        runOnUiThread {
                            progressBar()
                        }
                        var translatedBarcode = CF.translateBarcodePCB(edVerify.text.toString())

                        if (translatedBarcode.PART_NO == rowLine.MATERIAL_NO) {
                            if((scannedMat[0].SCANNED_QUANTITY + 1) <=
                                scannedMat[0].PICKLIST_QUANTITY){
                                if(!checkForDuplicate(translatedBarcode.SERIAL_NO)){
                                    val fResult = saveToDatabase(translatedBarcode.PART_NO,"",rowLine.BATCH_NO,1,translatedBarcode.SERIAL_NO,badgeString)
                                    val iSplit = fResult.split(':')
                                    if(iSplit[0].contains('S')){
                                        val scannedDat = CF.retrieveScannedData(picklistBarcode.text.toString()).filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }
                                        runOnUiThread {
                                            edVerify.hint = "Scanned PCBA = " + scannedDat[0].SCANNED_QUANTITY.toString()
                                            edVerify.text.clear()
                                            if(scannedDat.isNotEmpty()){
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
                                    }
                                }
                                else{
                                    val duplicatePallet = AlertDialog.Builder(c)
                                    duplicatePallet.setMessage("Duplicate carton scanner \n Carton ni dah scan dah! Nak scan berapa kali ?")
                                    duplicatePallet.setTitle("Error")
                                    duplicatePallet.setNegativeButton("Back") { dialog, which ->
                                        edVerify.requestFocus()
                                        edVerify.text.clear()
                                    }
                                    runOnUiThread(kotlinx.coroutines.Runnable {
                                        duplicatePallet.show()
                                    })
                                }

                                edVerify.requestFocus()
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
                            edverifyContainerLinearHoriz.setBackgroundColor(Color.RED)
                        }
                    }
                    job.join()
                    runOnUiThread {
                        progressBar()
                    }
                }



                return@OnKeyListener true
            }
            false
        })

        edverifyContainerLinearHoriz.addView(edVerify)
        row.addView(edverifyContainerLinearHoriz)
        row.addView(CF.generateTVforRow(rowLine.PICK_QTY.toInt().toString(),c))
        row.addView(CF.generateTVforRow(rowLine.MATERIAL_NO,c))
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



    private fun checkForDuplicate(cartonNumber:String):Boolean {
        val link = "http://172.16.206.19/REST_API/SERVICEPACKING/checkDuplicateCarton?cartonNum=${cartonNumber}"
        val urlLink: URL = URL(link)
        val jsonResult = urlLink.readText().replace("\"","")
        val rowCount = jsonResult.toInt()
        return (rowCount >= 1)

    }

    private fun progressBar(){
        if(pb.visibility == View.INVISIBLE || pb.visibility == View.GONE){
            pb.visibility = View.VISIBLE
        }
        else{
            pb.visibility = View.INVISIBLE
        }
    }

    private fun translateBarcode(LaserMarkingBcode: String): PCBBarcode {
        val link: String = "http://172.16.206.19/REST_API/SERVICEPACKING/PCBLaserMarking?bcode=${LaserMarkingBcode}"
        val urlLink: URL = URL(link)
        var jsonResult = urlLink.readText()
        var jsonObj = JSONObject(jsonResult)

        return PCBBarcode(
            PART_NO = jsonObj.getString("PART_NO"),
            SERIAL_NO = jsonObj.getString("SERIAL_NO")
        )
    }

}

data class PCBBarcode(
    var PART_NO: String,
    var SERIAL_NO:String
)