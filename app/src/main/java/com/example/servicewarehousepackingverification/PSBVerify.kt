package com.example.servicewarehousepackingverification

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.size
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class PSBVerify : AppCompatActivity() {
    private lateinit var picklistBcode: EditText
    private lateinit var mainLayout: LinearLayout
    private lateinit var c: Context
    private lateinit var badgeString:String
    private lateinit var scannedItems:MutableList<SCANNED_DATA>
    private lateinit var pb:ProgressBar
    private lateinit var clearButton:Button
    private lateinit var CF:CommonFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_psbverify)
        picklistBcode = findViewById(R.id.edPicklistPCBA)
        picklistBcode.requestFocus()
        mainLayout = findViewById(R.id.PCBAssyHoriz)
        c = this
        pb = findViewById(R.id.PBpsbVerify)
        clearButton = findViewById(R.id.btnPSBVClearButton)
        scannedItems = mutableListOf()
        badgeString = intent.getStringExtra("BadgeNum").toString()
        CF = CommonFunctions()

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
                    picklistBcode.text.clear()
                }
                else{
                    val clearDialog = CF.generateAlertDialog("Scan all material before clearing the Picklist number !\n" +
                            " Scan semua bagi hijau dulu baru tekan clear, buat kerja bagi habis !"
                        ,"Error",c)
                    clearDialog.setNegativeButton("Back") { dialog, which ->

                    }
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        clearDialog.show()
                    })
                }
            }

        }

        picklistBcode.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
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
                            picklistdata = generatePickList(picklistBcode.text.toString())
                            scannedItems = CF.retrieveScannedData(picklistBcode.text.toString())
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
                            runOnUiThread {
                                picklistBcode.requestFocus()
                                picklistBcode.text.clear()
                            }

                        }

                    }
                    job.join()
                    try{
                        runOnUiThread {
                            // Stuff that updates the UI
                            if (mainLayout.size > 0) {
                                mainLayout.removeAllViews()
                            }
                            mainLayout.addView(generateTableView(picklistdata))

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


    private fun progressBar(){
        if(pb.visibility == View.INVISIBLE || pb.visibility == View.GONE){
            pb.visibility = View.VISIBLE
        }
        else{
            pb.visibility = View.INVISIBLE
        }
    }

    private fun generateTableView(data: MutableList<PickList>): View {
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
            hint = "Scanned items = " + scannedMat[0].SCANNED_QUANTITY.toString()
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
        else{

        }

        edVerify.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                //Perform Code
                runBlocking {
                    val job = GlobalScope.launch {
                        runOnUiThread {
                            progressBar()
                        }
                        var result = CF.translateBarcodePSB(edVerify.text.toString())
                        if (result.MATERIAL_NO == rowLine.MATERIAL_NO && result.LOT_NO == rowLine.BATCH_NO) {
                            var scannedMat = CF.retrieveScannedData(picklistBcode.text.toString()).filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }
                            if((scannedMat[0].SCANNED_QUANTITY + result.CARTON_QTY) <=
                                scannedMat[0].PICKLIST_QUANTITY) {
                                if(!checkForDuplicate(result.CARTON_NO)){
                                    var i = saveToDatabase(result.MATERIAL_NO,result.CUST_PART_NO,result.LOT_NO,result.CARTON_QTY,result.CARTON_NO,badgeString)
                                    var iSplit = i.split(':')
                                    if(iSplit[0].contains('S')){
                                        var scannedDat = CF.retrieveScannedData(picklistBcode.text.toString()).filter { it.MATERIAL == rowLine.MATERIAL_NO && it.LOT_NO == rowLine.BATCH_NO }

                                        runOnUiThread {
                                            edVerify.hint = "Scanned items = " + scannedDat[0].SCANNED_QUANTITY.toString()
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
                                    val duplicatePallet = CF.generateAlertDialog("Duplicate carton scanner \n Carton ni dah scan dah! Nak scan berapa kali ?",
                                        "Error",c)
                                    duplicatePallet.setNegativeButton("Back") { dialog, which ->
                                        edVerify.requestFocus()
                                        edVerify.text.clear()
                                    }
                                    runOnUiThread(kotlinx.coroutines.Runnable {
                                        duplicatePallet.show()
                                    })
                                }

                            }
                            else{
                                val wrongMaterial = CF.generateAlertDialog("Carton quantity more than Picklist quantity \n Quantity pun tak betul hang scan benda apa ?",
                                    "Error",c)
                                wrongMaterial.setNegativeButton("Back") { dialog, which ->
                                    edVerify.requestFocus()
                                    edVerify.text.clear()
                                }
                                runOnUiThread(kotlinx.coroutines.Runnable {
                                    wrongMaterial.show()
                                })
                            }
                        } else {
                            val wrongMaterial = CF.generateAlertDialog("Wrong Material Scanned \n Material tu salah, Check la label dengan material nak scan",
                                "Error",c)
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
        row.addView(CF.generateTVforRow(rowLine.MATERIAL_NO,c))
        row.addView(CF.generateTVforRow(rowLine.PICK_QTY.toInt().toString(),c))
        row.addView(CF.generateTVforRow(rowLine.BATCH_NO,c))
        return row
    }

    private fun generatePickList(doNum: String): MutableList<PickList> {

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



        return pickList
    }

    private fun checkForDuplicate(cartonNumber:String):Boolean {
        val link = "http://172.16.206.19/REST_API/SERVICEPACKING/checkDuplicateCarton?cartonNum=${cartonNumber}"
        val urlLink: URL = URL(link)
        val jsonResult = urlLink.readText().replace("\"","")
        val rowCount = jsonResult.toInt()
        return (rowCount >= 1)

    }


    private fun saveToDatabase(material:String,custPNo:String,lotNum:String,qty:Int,cartonNo:String,bdgeNo:String):String{
        var result = CF.saveToDatabasePSBVerify(material,custPNo,lotNum,qty,cartonNo,bdgeNo,picklistBcode.text.toString())
        scannedItems.clear()
        scannedItems = CF.retrieveScannedData(picklistBcode.text.toString())
        return result
    }



}

data class SCANNED_DATA(
    var MATERIAL:String,
    var LOT_NO:String,
    var SCANNED_QUANTITY:Int,
    var PICKLIST_QUANTITY:Int,
    var STATUS:String
)

data class Barcode(
    var MATERIAL_NO: String,
    var CUST_PART_NO:String,
    var LOT_NO:String,
    var CARTON_QTY:Int,
    var CARTON_NO:String
)

data class PickList(
    var BATCH_NO:String,
    var MATERIAL_NO:String,
    var PICK_QTY:Double
)