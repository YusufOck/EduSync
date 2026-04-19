package com.example.edusync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val repository: TeacherRepository
) : ViewModel() {

    private val _selectedTeacherId = MutableStateFlow<Int?>(null)
    val selectedTeacherId = _selectedTeacherId.asStateFlow()

    private val _isAdminEditing = MutableStateFlow(false)
    val isAdminEditing = _isAdminEditing.asStateFlow()

    private val _conflictError = MutableStateFlow<String?>(null)
    val conflictError = _conflictError.asStateFlow()

    val teachers = repository.getAllTeachers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Admin için global görünüm verisi
    val globalSchedules = repository.getAllAvailabilities().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTeacher = _selectedTeacherId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getAllTeachers().map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val availabilityMatrix = combine(_selectedTeacherId, currentTeacher, _isAdminEditing) { id, teacher, adminEditing ->
        Triple(id, teacher, adminEditing)
    }.flatMapLatest { (id, teacher, editing) ->
        if (id == null) return@flatMapLatest flowOf(emptyList<TeacherAvailability>())
        if (editing || teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL) {
            repository.getProposal(id)
        } else {
            repository.getAvailability(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val teacherCourses = _selectedTeacherId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList<Course>())
        else repository.getCoursesByTeacher(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTeacher(id: Int) {
        _selectedTeacherId.value = id
    }

    // --- ACCOUNT ACTIVATION ---

    suspend fun checkVerificationCode(code: String): VerificationCode? {
        return repository.getVerificationCode(code)
    }

    suspend fun getTeacherById(id: Int): Teacher? {
        return repository.getTeacherById(id)
    }

    suspend fun activateAccount(code: String, username: String, password: String): Boolean {
        val user = User(username = username, password = password)
        return repository.activateTeacherAccount(code, user)
    }

    // --- ADMIN ACTIONS ---

    fun startAdminEdit() {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.copyAvailabilityToProposal(id)
            _isAdminEditing.value = true
        }
    }

    fun cancelAdminEdit() {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.discardProposal(id)
            _isAdminEditing.value = false
        }
    }

    fun assignCourse(dayIndex: Int, slotIndex: Int, course: Course, classroom: String) {
        val teacherId = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            val conflict = repository.checkClassroomConflict(dayIndex, slotIndex, classroom, teacherId)
            if (conflict != null) {
                _conflictError.value = "Çakışma: $classroom dersliğinde o saatte $conflict"
                return@launch
            }
            _conflictError.value = null
            repository.updateProposal(
                TeacherAvailability(
                    teacherId = teacherId,
                    dayIndex = dayIndex,
                    slotIndex = slotIndex,
                    isBusy = true,
                    courseName = course.name,
                    courseCode = course.code,
                    classroom = classroom
                )
            )
        }
    }

    fun clearSlot(dayIndex: Int, slotIndex: Int) {
        val teacherId = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.updateProposal(
                TeacherAvailability(
                    teacherId = teacherId,
                    dayIndex = dayIndex,
                    slotIndex = slotIndex,
                    isBusy = false
                )
            )
        }
    }

    fun clearConflictError() {
        _conflictError.value = null
    }

    fun submitAdminProposal(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.updateScheduleStatus(id, ScheduleStatus.ADMIN_PROPOSAL, adminNote = note, teacherNote = "")
            _isAdminEditing.value = false
        }
    }

    fun addTeacher(name: String, surname: String) {
        viewModelScope.launch {
            repository.insertTeacher(Teacher(name = name, surname = surname))
        }
    }

    fun deleteTeacher(teacher: Teacher) {
        viewModelScope.launch {
            repository.deleteTeacher(teacher)
        }
    }

    // --- TEACHER ACTIONS ---

    fun approveAdminProposal() {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.applyProposal(id)
            repository.updateScheduleStatus(id, ScheduleStatus.APPROVED, adminNote = "", teacherNote = "")
        }
    }

    fun rejectAdminProposal(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.discardProposal(id)
            repository.updateScheduleStatus(id, ScheduleStatus.REJECTED, adminNote = "", teacherNote = note)
        }
    }
}
