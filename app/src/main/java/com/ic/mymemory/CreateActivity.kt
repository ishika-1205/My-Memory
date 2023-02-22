package com.ic.mymemory

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.util.Log.i
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ic.mymemory.models.BoardSize
import com.ic.mymemory.utils.BitmapScaler
import com.ic.mymemory.utils.EXTRA_BOARD_SIZE
import com.ic.mymemory.utils.isPermissionGranted
import com.ic.mymemory.utils.requestPermission
import java.io.ByteArrayOutputStream
import java.util.jar.Manifest

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_LENGTH = 3
        private const val MAX_GAME_LENGTH = 14

    }

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button



    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>() //uniform resource identifier, kind of like a string which unambigously identifies where does a particular resource live
    private val storage = Firebase.storage
    private val db = Firebase.firestore



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave =  findViewById(R.id.btnSave)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Chose pics (0 / $numImagesRequired)"


        btnSave.setOnClickListener{
            saveDataToFirebase()
        }
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

        })




        val adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                // implicit intents
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                launchIntentForPhotos()}
                else{
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }

        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())



    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                launchIntentForPhotos()
        }else{
            Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK){
            Log.w(TAG, "Did not get data back from the launched activity, user likely cancelled the selection flow")
        return}
        val selectedUri = data?.data
        val clipData = data?.clipData
        if(clipData != null){
            Log.i(TAG, "clipData number of images ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < numImagesRequired){ //
                    chosenImageUris.add(clipItem.uri)
                } else if(selectedUri != null){
                    Log.i(TAG, "data $selectedUri")
                    chosenImageUris.add(selectedUri)
                }
                adapter.notifyDataSetChanged()
                supportActionBar?.title = "Chose pics (${chosenImageUris.size}/ ${numImagesRequired})"
            }
        }
    }

    private fun launchIntentForPhotos() {
       val intent = Intent(Intent.ACTION_PICK)
        intent.type = "Image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
startActivityForResult(Intent.createChooser(intent, "Chose pics"), PICK_PHOTO_CODE)

    }

    private fun saveDataToFirebase() {
        val customGameName = etGameName.text.toString()
        //DOWNSCALE THE IMAGE st it takes less place in the firebase storage and speeds up the upload
Log.i(TAG, "saveDataToFirebase")
        var didEncounterError = false
        val uploadImageUrls = mutableListOf<String>()
        for((index, photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri) // this method will take care of all the downgrading of image that we want
        //define a path where this image should live in the firebase storage
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            //actually moving data to the firebase storage
            val photoReference = storage.reference.child(filePath)
            //val success =
                photoReference.putBytes(imageByteArray)
            // unlike above instead of getting a return value we will get get a task in return which we have to wait for until it succeeds or fails
                    // continueWithTask is an API defined by firebase coz above is a long operation once it concludes execute the code and execute 1 more task defined at the end
                    //result of putBytes is a photoUpload task
                    .continueWithTask{ photoUploadTask ->
                     Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}" )
                        //task we are going to continue with is after pic uploaded we get the downloaded URL
                    photoReference.downloadUrl
                        //above returns a task and we have to wait for the completion of that task
                    }.addOnCompleteListener{downloadUrlTask ->
                        if(!downloadUrlTask.isSuccessful){
                            Log.e(TAG, "Exception with firebase storage", downloadUrlTask.exception)
                            Toast.makeText(this,"Failed to upload image" , Toast.LENGTH_SHORT).show()
                            didEncounterError =  true
                            return@addOnCompleteListener // since no point in continuing
                        }
                        if (didEncounterError){
                            return@addOnCompleteListener
                        }
                        val downloadUrl = downloadUrlTask.result.toString()
                        uploadImageUrls.add(downloadUrl)
                        Log.i(TAG, "finished uploading $photoUri, num uploaded ${uploadImageUrls.size}" )
                        if(uploadImageUrls.size == chosenImageUris.size){
                            handleAllImagesUploaded(customGameName, uploadImageUrls)
                        }

                    }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        // TODO : UPLOAD ALL THE INFO WE HAVE I.E THE IMAGE URLS AND THE CUSTOM GAME NAME TO UPLOAD IT TO THE FIRESTORE
        db.
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
val originalBitmap =  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)  {// P here is the original bitmap android code pie
    val source = ImageDecoder.createSource(contentResolver, photoUri)
    ImageDecoder.decodeBitmap(source)
}  else {
    MediaStore.Images.Media.getBitmap(contentResolver, photoUri) // for older version android that does not support android pie
}

        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun shouldEnableSaveButton(): Boolean {
//decides if we should enable the save button or not
        if(chosenImageUris.size != numImagesRequired){return false}
        if(etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_LENGTH){return false}
        return true
    }
}