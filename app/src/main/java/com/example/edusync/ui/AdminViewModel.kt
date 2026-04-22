package com.example.edusync.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val excelManager: ExcelManager,
    private val teacherRepository: TeacherRepository,
    private val userRepository: FirebaseUserRepository
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportResult?>(null)
    val importState = _importState.asStateFlow()

    private val _excelPreview = MutableStateFlow<List<ExcelPreviewItem>?>(null)
    val excelPreview = _excelPreview.asStateFlow()

    private var currentUri: Uri? = null

    // --- Existing Flows ---
    val teachers = teacherRepository.getAllTeachers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // PDF Optimization: Use Dispatchers.Default for heavy mapping and lookup
    val verificationCodes = userRepository.getAllVerificationCodes()
        .combine(teacherRepository.getAllTeachers()) { codes, allTeachers ->
            val teacherMap = allTeachers.associateBy { it.id }
            codes.map { code ->
                val teacher = teacherMap[code.teacherId]
                code.copy(createdBy = teacher?.let { "${it.title} ${it.name} ${it.surname}" } ?: "Genel Kod")
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ========== Phase 2: Classroom Management ==========

    val classrooms = teacherRepository.getAllClassrooms().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _classroomError = MutableStateFlow<String?>(null)
    val classroomError = _classroomError.asStateFlow()

    fun addClassroom(roomCode: String, capacity: Int, department: String) {
        viewModelScope.launch {
            try {
                teacherRepository.insertClassroom(
                    Classroom(roomCode = roomCode, capacity = capacity, department = department)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _classroomError.value = "Sınıf eklenemedi: ${e.localizedMessage}"
            }
        }
    }

    fun deleteClassroom(classroomId: String) {
        viewModelScope.launch {
            try {
                teacherRepository.deleteClassroom(classroomId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun clearClassroomError() { _classroomError.value = null }

    // ========== Phase 2: Schedule Entry (Assignment) Management ==========

    val allCourses = teacherRepository.getAllCourses().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val scheduleEntries = teacherRepository.getAllScheduleEntries().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _assignmentError = MutableStateFlow<String?>(null)
    val assignmentError = _assignmentError.asStateFlow()

    private val _assignmentSuccess = MutableStateFlow(false)
    val assignmentSuccess = _assignmentSuccess.asStateFlow()

    fun assignSchedule(
        courseCode: String,
        courseName: String,
        teacherId: Int,
        classroomId: String,
        day: Int,
        timeSlot: Int
    ) {
        viewModelScope.launch {
            try {
                // Phase 2 Madde 5.2: Double-booking prevention
                val conflict = teacherRepository.checkScheduleConflict(day, timeSlot, teacherId, classroomId)
                if (conflict != null) {
                    _assignmentError.value = conflict
                    return@launch
                }

                teacherRepository.insertScheduleEntry(
                    ScheduleEntry(
                        courseCode = courseCode,
                        courseName = courseName,
                        teacherId = teacherId,
                        classroomId = classroomId,
                        day = day,
                        timeSlot = timeSlot
                    )
                )
                _assignmentError.value = null
                _assignmentSuccess.value = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _assignmentError.value = "Atama yapılamadı: ${e.localizedMessage}"
            }
        }
    }

    fun deleteScheduleEntry(entryId: String) {
        viewModelScope.launch {
            try {
                teacherRepository.deleteScheduleEntry(entryId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun clearAssignmentError() { _assignmentError.value = null }
    fun clearAssignmentSuccess() { _assignmentSuccess.value = false }

    // ========== Phase 2 Madde 6: Admin Dashboard Summary Panels ==========

    val unassignedTeachers: StateFlow<List<Teacher>> = combine(teachers, scheduleEntries) { teacherList, entries ->
        val assignedTeacherIds = entries.map { it.teacherId }.toSet()
        teacherList.filter { it.id !in assignedTeacherIds }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unassignedCourses: StateFlow<List<Course>> = combine(allCourses, scheduleEntries) { courseList, entries ->
        val assignedCourseCodes = entries.map { it.courseCode }.toSet()
        courseList.filter { it.code !in assignedCourseCodes }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableClassrooms: StateFlow<List<Pair<Classroom, Int>>> = combine(classrooms, scheduleEntries) { classroomList, entries ->
        val totalSlots = 5 * 9 // 5 gün × 9 saat dilimi = 45 slot
        classroomList.map { classroom ->
            val usedSlots = entries.count { it.classroomId == classroom.roomCode }
            val freeSlots = totalSlots - usedSlots
            classroom to freeSlots
        }.filter { it.second > 0 }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ========== Existing functions ==========

    fun generateCodeForTeacher(teacherId: Int) {
        viewModelScope.launch {
            teacherRepository.generateCodeForTeacher(teacherId)
        }
    }

    fun loadPreview(context: Context, uri: Uri) {
        currentUri = uri
        viewModelScope.launch {
            val result = excelManager.getPreview(context, uri)
            if (result.isSuccess) {
                _excelPreview.value = result.getOrNull()
            } else {
                _importState.value = ImportResult.Error(result.exceptionOrNull()?.message ?: "Önizleme yüklenemedi")
            }
        }
    }

    fun confirmImport(context: Context) {
        val uri = currentUri ?: return
        viewModelScope.launch {
            _importState.value = ImportResult.Loading
            val result = excelManager.importExcel(context, uri)
            _importState.value = if (result.isSuccess) {
                ImportResult.Success(result.getOrNull() ?: 0)
            } else {
                ImportResult.Error(result.exceptionOrNull()?.message ?: "Aktarım hatası")
            }
            _excelPreview.value = null
        }
    }

    fun clearImportState() {
        _importState.value = null
        _excelPreview.value = null
        currentUri = null
    }
}
