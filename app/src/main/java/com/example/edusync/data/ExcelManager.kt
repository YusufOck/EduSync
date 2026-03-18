package com.example.edusync.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ExcelPreviewItem(
    val courseCode: String,
    val courseName: String,
    val lecturer: String
)

@Singleton
class ExcelManager @Inject constructor(
    private val teacherRepository: TeacherRepository
) {
    private val TAG = "ExcelManager"

    fun getPreview(context: Context, uri: Uri): Result<List<ExcelPreviewItem>> {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val previewList = mutableListOf<ExcelPreviewItem>()

            val rowLimit = if (sheet.lastRowNum > 5) 5 else sheet.lastRowNum
            for (i in 1..rowLimit) {
                val row = sheet.getRow(i) ?: continue
                val code = row.getCell(0)?.toString()?.trim() ?: ""
                val name = row.getCell(1)?.toString()?.trim() ?: ""
                val lecturer = row.getCell(2)?.toString()?.trim() ?: ""
                
                if (code.isNotEmpty() || name.isNotEmpty() || lecturer.isNotEmpty()) {
                    previewList.add(ExcelPreviewItem(code, name, lecturer))
                }
            }
            workbook.close()
            Result.success(previewList)
        } catch (e: Throwable) {
            Log.e(TAG, "Önizleme hatası", e)
            Result.failure(e)
        } finally {
            inputStream?.close()
        }
    }

    suspend fun importExcel(context: Context, uri: Uri): Result<Int> {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            var count = 0

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                
                val courseCode = row.getCell(0)?.toString()?.trim() ?: ""
                val courseName = row.getCell(1)?.toString()?.trim() ?: ""
                val lecturerFull = row.getCell(2)?.toString()?.trim() ?: ""

                if (courseCode.isNotEmpty() && lecturerFull.isNotEmpty()) {
                    val parts = lecturerFull.split(" ").filter { it.isNotBlank() }
                    val titleParts = parts.filter { it.contains(".") || it.equals("Dr", ignoreCase = true) }
                    val title = titleParts.joinToString(" ")
                    val nameParts = parts.filter { !titleParts.contains(it) }
                    
                    val name = if (nameParts.isNotEmpty()) nameParts.first().trim() else ""
                    val surname = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ").trim() else ""

                    if (name.isEmpty()) continue

                    // Veritabanına kaydederken ve ararken hep normalize (küçük harf + i/ı fix) kullanıyoruz
                    val teacherId = teacherRepository.insertTeacher(
                        Teacher(name = name, surname = surname, title = title)
                    ).toInt()

                    teacherRepository.insertCourse(
                        Course(code = courseCode, name = courseName, teacherId = teacherId)
                    )
                    count++
                }
            }
            workbook.close()
            Result.success(count)
        } catch (e: Throwable) {
            Log.e(TAG, "Aktarım hatası", e)
            Result.failure(e)
        } finally {
            inputStream?.close()
        }
    }
}
