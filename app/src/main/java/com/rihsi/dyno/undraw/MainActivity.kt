package com.rihsi.dyno.undraw

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rihsi.dyno.undraw.databinding.ActivityMainBinding
import com.rihsi.dyno.undraw.databinding.DialogBrushSizeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawingView = binding.drawingView

        drawingView.setSizeForBrush(10f)

        binding.ibBrush.setOnClickListener { showBrushSizeDialog(drawingView) }
        binding.ibUndo.setOnClickListener { drawingView.undo() }
        binding.ibRedo.setOnClickListener { drawingView.redo() }
        binding.ibClear.setOnClickListener { drawingView.clear() }

        binding.ibGallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                openGallery()
            } else {
                requestReadStoragePermission()
            }
        }
        binding.ibSave.setOnClickListener {
            requestWriteStoragePermission()
            if(isReadStorageAllowed()){
                lifecycleScope.launch {
                  val flDrawingView: FrameLayout = binding.flDrawingViewContainer
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }

    private fun requestWriteStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            Snackbar.make(
                binding.root,
                "Storage permission is required to save image",
                Snackbar.LENGTH_LONG
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CompanionObject.REQUEST_WRITE_STORAGE_PERMISSION
            )
        }
    }



    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        //If permission is granted returning true
        return (result == PackageManager.PERMISSION_GRANTED)
    }

    private fun openGallery() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickPhoto, CompanionObject.GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == CompanionObject.GALLERY_REQUEST_CODE) {
            try {
                if (data != null) {
                    if (data.data != null) {
                        val contentURI = data.data
                        binding.ivBackground.visibility = View.VISIBLE
                        binding.ivBackground.setImageURI(contentURI)
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            CompanionObject.REQUEST_WRITE_STORAGE_PERMISSION
        )
    }

    private fun showBrushSizeDialog(drawingView: DrawingView) {
        val brushDialog = Dialog(this)
        var brushSizeBinding = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(brushSizeBinding.root)
        brushDialog.setTitle("Brush Size: ")

        brushSizeBinding.brushSizeMinus.setOnClickListener {
            drawingView.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        brushSizeBinding.brushSizePlus.setOnClickListener {
            drawingView.setSizeForBrush(20f)
            brushDialog.dismiss()
        }
        brushSizeBinding.brushSizeLarge.setOnClickListener {
            drawingView.setSizeForBrush(30f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
    private fun getBitmapFromView(view: View): Bitmap{
        val returnedBitmap  = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
        var toSave = ""
        withContext(Dispatchers.IO){
            if(mBitmap !=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                    + File.separator + "UndrawApp_" + System.currentTimeMillis() /1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    toSave = f.absolutePath

                    runOnUiThread {
                        if(toSave.isNotEmpty()){
                            Toast.makeText(
                                this@MainActivity,
                                "File Saved Successfully : $toSave",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else{
                            Toast.makeText(
                                this@MainActivity,
                                "Some thing went wrong in saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                catch(e: Exception){
                    toSave = ""
                    e.printStackTrace()
                }
            }
        }
        return toSave
    }
}
