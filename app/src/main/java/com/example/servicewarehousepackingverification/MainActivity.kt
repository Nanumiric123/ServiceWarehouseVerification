package com.example.servicewarehousepackingverification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URL


class MainActivity : AppCompatActivity() {
    private lateinit var PSBbtn:Button
    private lateinit var PCBAbtn:Button
    private lateinit var RMPNbtn:Button
    private lateinit var badgeNumber:EditText
    private lateinit var c:Context
    private lateinit var pb:ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PSBbtn = findViewById(R.id.btnPSB)
        PCBAbtn = findViewById(R.id.btnPCBA)
        RMPNbtn = findViewById(R.id.btnRMPN)
        badgeNumber = findViewById(R.id.edBadgeNum)
        pb = findViewById(R.id.PBMain)
        badgeNumber.requestFocus()
        c = this

        RMPNbtn.setOnClickListener {
            val _user = badgeNumber.text.toString()

            if(_user.isNotBlank()){
                val alertDialogPSBBuilder = AlertDialog.Builder(this)
                alertDialogPSBBuilder.setMessage("Input Password")
                // Set up the input
                val password = EditText(this)
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                password.hint = "Password"
                alertDialogPSBBuilder.setView(password)

                alertDialogPSBBuilder.setPositiveButton("OK") {dialog, which ->
                    var loginResult:Boolean = false
                    val _pass = password.text.toString()
                    progressBar()
                    runBlocking {
                        val job = GlobalScope.launch {

                            loginResult = getLoginInfo(_user,_pass)

                        }
                        job.join()
                        if(loginResult){
                            val pcbAVerify = Intent(applicationContext,RawPartVerify::class.java)
                            pcbAVerify.putExtra("BadgeNum",_user)
                            startActivity(pcbAVerify)
                        }
                        else{
                            val wrongPassDialogBuilder = AlertDialog.Builder(c)
                            wrongPassDialogBuilder.setMessage("Wrong Password")
                            wrongPassDialogBuilder.setTitle("Error")
                            wrongPassDialogBuilder.setNegativeButton("Cancel") {dialog, which ->
                                badgeNumber.requestFocus()
                            }
                            wrongPassDialogBuilder.show()
                        }
                        progressBar()
                    }


                }
                alertDialogPSBBuilder.setNegativeButton("Cancel") {dialog, which ->

                }
                alertDialogPSBBuilder.show()

            }
            else{
                val BlankDialogBuilder = AlertDialog.Builder(this)
                BlankDialogBuilder.setMessage("Enter Badge Number")
                BlankDialogBuilder.setTitle("Error")
                BlankDialogBuilder.setNegativeButton("Cancel") {dialog, which ->
                    badgeNumber.requestFocus()
                }
            }

        }

        PCBAbtn.setOnClickListener {
            val _user = badgeNumber.text.toString()

            if(_user.isNotBlank()){
                val alertDialogPSBBuilder = AlertDialog.Builder(this)
                alertDialogPSBBuilder.setMessage("Input Password")
                // Set up the input
                val password = EditText(this)
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                password.hint = "Password"
                alertDialogPSBBuilder.setView(password)

                alertDialogPSBBuilder.setPositiveButton("OK") {dialog, which ->
                    var loginResult:Boolean = false
                    val _pass = password.text.toString()
                    progressBar()
                    runBlocking {
                        val job = GlobalScope.launch {

                            loginResult = getLoginInfo(_user,_pass)

                        }
                        job.join()
                        if(loginResult){
                            val pcbAVerify = Intent(applicationContext,PcbAssyVerify::class.java)
                            pcbAVerify.putExtra("BadgeNum",_user)
                            startActivity(pcbAVerify)
                        }
                        else{
                            val wrongPassDialogBuilder = AlertDialog.Builder(c)
                            wrongPassDialogBuilder.setMessage("Wrong Password")
                            wrongPassDialogBuilder.setTitle("Error")
                            wrongPassDialogBuilder.setNegativeButton("Cancel") {dialog, which ->
                                badgeNumber.requestFocus()
                            }
                            wrongPassDialogBuilder.show()
                        }
                        progressBar()
                    }


                }
                alertDialogPSBBuilder.setNegativeButton("Cancel") {dialog, which ->

                }
                alertDialogPSBBuilder.show()

            }
            else{
                val BlankDialogBuilder = AlertDialog.Builder(this)
                BlankDialogBuilder.setMessage("Enter Badge Number")
                BlankDialogBuilder.setTitle("Error")
                BlankDialogBuilder.setNegativeButton("Cancel") {dialog, which ->
                    badgeNumber.requestFocus()
                }
            }

        }

        PSBbtn.setOnClickListener {
            val _user = badgeNumber.text.toString()
            if(_user.toString().isNotBlank() || _user.isNotBlank()){
                val alertDialogPSBBuilder = AlertDialog.Builder(this)
                alertDialogPSBBuilder.setMessage("Input Password")
                // Set up the input
                val password = EditText(this)
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                password.hint = "Password"
                alertDialogPSBBuilder.setView(password)
                progressBar()
                alertDialogPSBBuilder.setPositiveButton("OK") {dialog, which ->
                    var loginResult:Boolean = false
                    val _pass = password.text.toString()

                    runBlocking {
                        val job = GlobalScope.launch {
                            runOnUiThread {
                                progressBar()
                            }
                            loginResult = getLoginInfo(_user,_pass)
                            runOnUiThread {
                                progressBar()
                            }
                        }
                        job.join()
                        if(loginResult){
                            val PSBVerifyintent = Intent(applicationContext,PSBVerify::class.java)
                            PSBVerifyintent.putExtra("BadgeNum",_user)
                            startActivity(PSBVerifyintent)
                        }
                        else{
                            val wrongPassDialogBuilder = AlertDialog.Builder(c)
                            wrongPassDialogBuilder.setMessage("Wrong Password")
                            wrongPassDialogBuilder.setTitle("Error")
                            wrongPassDialogBuilder.setNegativeButton("Cancel") {dialog, which ->
                                badgeNumber.requestFocus()
                            }
                            wrongPassDialogBuilder.show()
                        }



                    }


                }
                alertDialogPSBBuilder.setNegativeButton("Cancel") {dialog, which ->

                }
                alertDialogPSBBuilder.show()
            }
            else{
                val BlankDialogBuilder = AlertDialog.Builder(this)
                BlankDialogBuilder.setMessage("Enter Badge Number")
                BlankDialogBuilder.setTitle("Error")
                BlankDialogBuilder.setNegativeButton("Cancel") {dialog, which ->
                    badgeNumber.requestFocus()
                }
            }

        }

    }

    private fun progressBar(){
        if(pb.visibility == View.INVISIBLE || pb.visibility == View.GONE){
            pb.visibility == View.VISIBLE
        }
        else{
            pb.visibility == View.INVISIBLE
        }
    }

    private fun getLoginInfo(user:String,pass:String):Boolean {



        val link:String = "http://172.16.206.19/REST_API/Home/Barcode_loginSimple?user_name=${user}&pwd=${pass}"
        var urlLink:URL = URL(link)
        var result = urlLink.readText()

        return result == "true"


    }
}