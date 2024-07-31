package com.example.kidneyclassification

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.bangkit.gogym.helper.uriToFile
import org.jetbrains.annotations.Nullable
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

class ClassificationFragment : Fragment() {
    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_classification, container, false)
    }

    val inputSize = 224
    val batchSize = 1
    val numChannels = 3
    val dataTypeSize = 4 // FLOAT32 is 4 bytes in size
    val requiredSize = batchSize * inputSize * inputSize * numChannels * dataTypeSize
    private var getFile: File? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnUnggah = view.findViewById<Button>(R.id.btn_unggah)
        btnUnggah.setOnClickListener {
            startGallery()
        }
    }

    private fun startGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Choose a Picture")
        Log.d("LOGGING", "startGallery: ")
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val selectedImg = result.data?.data as Uri
            selectedImg.let { uri ->
                val myFile = uriToFile(uri, requireActivity())
                getFile = myFile
                val ivPreview = view?.findViewById<ImageView>(R.id.imageView)
                if (ivPreview != null) {
                    ivPreview.setImageURI(uri)
                }
                encodeImageAndSend(myFile)
            }
        }
    }


    private fun encodeImageAndSend(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

        // Call your API to send the image
        sendImageToApi(encodedImage)
    }

    private fun sendImageToApi(encodedImage: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaTypeOrNull()
            val body = """{"image":"${encodedImage.replace("\n", "")}"}""".toRequestBody(mediaType)
            Log.d("BODYREQUEST", body.toString())
            val request = Request.Builder()
                .url("http://192.168.1.8:5000/api/classify") // Replace with your API endpoint
                .post(body)
                .build()

            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    if (jsonResponse != null) {

                        withContext(Dispatchers.Main) {
                            val tvHasil = view?.findViewById<TextView>(R.id.txt_hasil)
                            val tvScore = view?.findViewById<TextView>(R.id.txt_probabilitas)
                            tvHasil?.text = jsonResponse.getString("result")
                            tvScore?.text = jsonResponse.getString("confidence")
                        }

                        Log.d("TESTSUCCESS", jsonResponse.getString("result"))
                    }
                } else {
                    // Handle unsuccessful response
                    Log.d("TESTFAILED", "FAILED SENDING IMAGE " + response.message)
                }
            } catch (e: Exception) {
                // Handle exception
                Log.d("TESTEXCEPTION", "EXCEPTION SENDING IMAGE " + e)
            }
        }
    }

}