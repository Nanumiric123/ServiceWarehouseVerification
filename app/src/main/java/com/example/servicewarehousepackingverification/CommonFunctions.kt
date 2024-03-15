package com.example.servicewarehousepackingverification

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class CommonFunctions {
    fun generateTVforRow(ptxt:String,c: Context): View {
        val generateTV = TextView(c)
        with(generateTV) {
            id = View.generateViewId()
            text = ptxt
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(10, 10, 10, 10)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.border)
        }
        return generateTV
    }

    fun generateAlertDialog(msg:String,titleTxt:String,C:Context):AlertDialog.Builder{
        val alertDia = AlertDialog.Builder(C)
        alertDia.setMessage("Duplicate carton scanner \n Carton ni dah scan dah! Nak scan berapa kali ?")
        alertDia.setTitle("Error")
        return alertDia
    }

    fun saveToDatabase(material:String,custPNo:String,lotNum:String,qty:Int,cartonNo:String,bdgeNo:String,plBcode:String):String{
        var link:String = "http://172.16.206.19/REST_API/SERVICEPACKING/recordData?" +
                "material=${material}&custPNo=${custPNo}&lotNo=${lotNum}&qty=${qty}&cartonNo=${cartonNo}&badge=${bdgeNo}&plno=${plBcode}"
        val urlLink: URL = URL(link)
        var result = urlLink.readText()
        var resultTranslate = result.split(':')
        return resultTranslate[0];
    }

    fun saveToDatabasePSBVerify(material:String,custPNo:String,lotNum:String,qty:Int,cartonNo:String,bdgeNo:String,plBcode:String):String{
        var link:String = "http://172.16.206.19/REST_API/SERVICEPACKING/recordData?" +
                "material=${material}&custPNo=${custPNo}&lotNo=${lotNum}&qty=${qty}&cartonNo=${cartonNo}&badge=${bdgeNo}&plno=${plBcode}"
        val urlLink: URL = URL(link)
        var result = urlLink.readText()
        var resultTranslate = result.split(':')

        return resultTranslate[0].trim().replace("^\"|\"$", "")
    }

    fun retrieveScannedData(pl:String): MutableList<SCANNED_DATA>{
        var link:String = "http://172.16.206.19/REST_API/SERVICEPACKING/retrieveScannedData?pickList=${pl}"
        val urlLink: URL = URL(link)
        var fResult:MutableList<SCANNED_DATA> = mutableListOf()
        var result = urlLink.readText()
        var jsonObjarray = JSONTokener(result).nextValue() as JSONArray
        for(i in 0 until  jsonObjarray.length()){
            var temp = SCANNED_DATA(
                MATERIAL = jsonObjarray.getJSONObject(i).getString("MATERIAL"),
                LOT_NO = jsonObjarray.getJSONObject(i).getString("LOT_NO"),
                SCANNED_QUANTITY = jsonObjarray.getJSONObject(i).getInt("SCANNED_QUANTITY"),
                PICKLIST_QUANTITY = jsonObjarray.getJSONObject(i).getInt("PICKLIST_QUANTITY"),
                STATUS = jsonObjarray.getJSONObject(i).getString("STATUS"))
            fResult.add(temp)
        }


        return fResult
    }

    fun translateBarcodePSB(cartonLabel: String): Barcode {
        val link: String =
            "http://172.16.206.19/REST_API/SERVICEPACKING/cartonBarcode?bcode=${cartonLabel}"
        val urlLink: URL = URL(link)
        val jsonResult = urlLink.readText()
        var jsonObj = JSONObject(jsonResult)
        return Barcode(
            MATERIAL_NO = jsonObj.getString("MATERIAL"),
            CUST_PART_NO = jsonObj.getString("CUST_PNO"),
            LOT_NO = jsonObj.getString("LOT_NO"),
            CARTON_QTY = jsonObj.getInt("QUANTITY"),
            CARTON_NO = jsonObj.getString("CARTON_NO")
        )
    }
    fun translateBarcodePCB(LaserMarkingBcode: String): PCBBarcode {
        val link: String = "http://172.16.206.19/REST_API/SERVICEPACKING/PCBLaserMarking?bcode=${LaserMarkingBcode}"
        val urlLink: URL = URL(link)
        var jsonResult = urlLink.readText()
        var jsonObj = JSONObject(jsonResult)

        return PCBBarcode(
            PART_NO = jsonObj.getString("PART_NO"),
            SERIAL_NO = jsonObj.getString("SERIAL_NO")
        )
    }
    fun translateBarcodeRP(cartonLabel: String): PASMYBarcode {
        val link: String =
            "http://172.16.206.19/REST_API/SERVICEPACKING/PASMYBarcode?bcode=${cartonLabel}"
        val urlLink: URL = URL(link)
        var jsonResult = urlLink.readText()
        var jsonObj = JSONObject(jsonResult)

        return PASMYBarcode(
            VENDOR = jsonObj.getString("VENDOR"),
            DATE = jsonObj.getString("DATE"),
            MATERIAL_NO = jsonObj.getString("PART_NO"),
            CARTON_NO = jsonObj.getString("CARTON_NO"),
            LOT = jsonObj.getString("LOT_NO"),
            QTY = jsonObj.getString("QUANTITY")
        )
    }

}