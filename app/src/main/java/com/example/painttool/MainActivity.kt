package com.example.painttool

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.painttool.PaintView.Companion.colorList
import com.example.painttool.PaintView.Companion.currentBrush
import com.example.painttool.PaintView.Companion.pathList
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity() {

    companion object{
        var path = Path()
        var paintBrush = Paint()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val blueBtn = findViewById<ImageButton>(R.id.blueColorImgButton)
        val blackBtn = findViewById<ImageButton>(R.id.blackColorImgButton)
        val eraserBtn = findViewById<ImageButton>(R.id.eraserButton)
        val saveBtn = findViewById<ImageButton>(R.id.saveButton)
        val paintCanvas = findViewById<RelativeLayout>(R.id.paintCanvas)

        blueBtn.setOnClickListener {
            Toast.makeText(this, "Classic", Toast.LENGTH_SHORT).show()
            paintBrush.setColor(Color.BLUE)
            currentColor(paintBrush.color)
        }

        blackBtn.setOnClickListener {
            Toast.makeText(this, "Чёрный бумер", Toast.LENGTH_SHORT).show()
            paintBrush.setColor(Color.BLACK)
            currentColor(paintBrush.color)
        }

        eraserBtn.setOnClickListener {
            Toast.makeText(this, "Стёр", Toast.LENGTH_SHORT).show()
            pathList.clear()
            colorList.clear()
            path.reset()
        }

        saveBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Save?")

            builder.setPositiveButton("Yes") { dialog, which ->
                // Получаем битмапу из нашей функции передавая в неё холст
                val bitmap = getBitmapFromUiView(paintCanvas)

                // Получаем время на телефоне (чтобы картинка не улетела к херам в галерее)
                val timestamp = System.currentTimeMillis()

                // Данная структура будет хранить в себе всю нужную информацию для сохранения картинки
                val values = ContentValues()

                // Задаём MIME_TYPE
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                // Задаём дату
                values.put(MediaStore.Images.Media.DATE_ADDED, timestamp)
                values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                // Задаём название папки конкретного сохранения (Pictures (когда ставлю другую - у меня крашит)) / Название подпапки (вот тут уже можно чё хочешь)
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + getString(R.string.app_name))
                // Кидаем значения в ожидание
                values.put(MediaStore.Images.Media.IS_PENDING, true)

                // Внесение значений в конечную модель данных
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    // Если модель данных создалась, то выполняем этот блок кода
                    try {
                        // Получение конечного пути и значений для сохранения
                        val outputStream = contentResolver.openOutputStream(uri)
                        if (outputStream != null) {
                            try {
                                // Конвертит мапу в пнг
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                // Закрывает модель данных для использования
                                outputStream.close()
                            } catch (e: Exception) {
                                Log.e(TAG, "saveBitmapImage: ", e)
                            }
                        }
                        // Убираем значения из ожидания
                        values.put(MediaStore.Images.Media.IS_PENDING, false)
                        // Очищаем модель данных
                        contentResolver.update(uri, values, null, null)

                        Toast.makeText(this, "Saved1...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "saveBitmapImage: ", e)
                    }
                } else {
                    // Если же модель данных не создалась, то выполняем этот блок кода

                    // Задаём путь сохранения в общий (внешний) каталог хранилища / наша подпапка
                    val imageFileFolder = File(Environment.getExternalStorageDirectory().toString() + '/' + getString(R.string.app_name))
                    if (!imageFileFolder.exists()) {
                        // Если даже так нет пути сохранение, то используем метод mkdirs()
                        // Создание нового каталога, обозначенного абстрактным путем что бы это не значило
                        imageFileFolder.mkdirs()
                    }
                    // Переменная названия картинки текущей датой.png
                    val mImageName = "$timestamp.png"
                    // Переменная конечного результата сохранения нашей картинки
                    val imageFile = File(imageFileFolder, mImageName)
                    try {
                        // Получение конечного пути и значений для сохранения
                        val outputStream: OutputStream = FileOutputStream(imageFile)
                        try {
                            // Конвертит мапу в пнг
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            // Закрывает модель данных для использования
                            outputStream.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "saveBitmapImage: ", e)
                        }
                        // Помещаем в значения абсолютный путь нашей imageFile
                        // An absolute path is a path that starts at a root of the file system. On Android, there is only one root: /.
                        values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                        // В модель данных помещаем значения
                        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                        Toast.makeText(this, "Saved2...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "saveBitmapImage: ", e)
                    }
                }
            }

            builder.setNegativeButton("No") { dialog, which ->
                Toast.makeText(this, "Ну и ладно", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        }
    }

    private fun getBitmapFromUiView(view: View?): Bitmap {
        // Определение битмапы по размерам оригинального холста
        val returnedBitmap = Bitmap.createBitmap(view!!.width, view.height, Bitmap.Config.ARGB_8888)
        // Привязываем canvas к нашей мапе
        val canvas = Canvas(returnedBitmap)

        /*
        // Получаем background нашего холста (view)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            // Если бэкграунд есть, то отрисовываем его на canvas
            //bgDrawable.draw(canvas)
        } else {
            // Если же нет - просто закрашиваем его белым, а то хули он
            //canvas.drawColor(Color.WHITE)
        }
        */

        // Делаем задний фон прозрачным
        canvas.drawColor(Color.alpha(0))

        // Отрисовываем view на canvas
        view.draw(canvas)

        // Возвращаем битмапу
        return returnedBitmap
    }

    private fun currentColor(color: Int) {
        currentBrush = color

        path = Path()
    }
}