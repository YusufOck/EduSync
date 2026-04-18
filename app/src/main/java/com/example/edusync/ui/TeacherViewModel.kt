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

    private val _isTeacherEditing = MutableStateFlow(false)
    val isTeacherEditing = _isTeacherEditing.asStateFlow()

    val teachers = repository.getAllTeachers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTeacher = _selectedTeacherId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getAllTeachers().map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val availabilityMatrix = combine(_selectedTeacherId, currentTeacher, _isAdminEditing, _isTeacherEditing) { id, teacher, adminEditing, teacherEditing ->
        DataQueryState(id, teacher, adminEditing || teacherEditing)
    }.flatMapLatest { state ->
        val id = state.id ?: return@flatMapLatest flowOf(emptyList<TeacherAvailability>())
        
        if (state.isEditing || 
            state.teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL || 
            state.teacher?.scheduleStatus == ScheduleStatus.PENDING) {
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

    // --- Admin Actions ---

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

    fun submitAdminProposal(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            // Clear teacher's old note, set new admin note
            repository.updateScheduleStatus(id, ScheduleStatus.ADMIN_PROPOSAL, adminNote = note, teacherNote = "")
            _isAdminEditing.value = false
        }
    }

    fun approveTeacherRequest(note: String = "") {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.applyProposal(id)
            repository.updateScheduleStatus(id, ScheduleStatus.APPROVED, adminNote = note, teacherNote = "")
        }
    }

    fun rejectTeacherRequest(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.discardProposal(id)
            repository.updateScheduleStatus(id, ScheduleStatus.REJECTED, adminNote = note, teacherNote = "")
        }
    }

    // --- Teacher Actions ---

    fun startTeacherEdit() {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.copyAvailabilityToProposal(id)
            _isTeacherEditing.value = true
        }
    }

    fun cancelTeacherEdit() {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.discardProposal(id)
            _isTeacherEditing.value = false
        }
    }

    fun sendTeacherRequest(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            // When teacher sends request, clear old admin note
            repository.updateScheduleStatus(id, ScheduleStatus.PENDING, adminNote = "", teacherNote = note)
            _isTeacherEditing.value = false
        }
    }

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
            // Clear old admin note, set new teacher rejection note
            repository.updateScheduleStatus(id, ScheduleStatus.REJECTED, adminNote = "", teacherNote = note)
        }
    }

    // --- Common ---

    fun toggleAvailability(dayIndex: Int, slotIndex: Int) {
        if (slotIndex == 4) return
        val teacherId = _selectedTeacherId.value ?: return
        val currentStatus = availabilityMatrix.value.find { 
            it.dayIndex == dayIndex && it.slotIndex == slotIndex 
        }
        val newBusy = !(currentStatus?.isBusy ?: false)
        
        viewModelScope.launch {
            val availability = TeacherAvailability(
                teacherId = teacherId,
                dayIndex = dayIndex,
                slotIndex = slotIndex,
                isBusy = newBusy
            )
            
            if (_isAdminEditing.value || _isTeacherEditing.value) {
                repository.updateProposal(availability)
            }
        }
    }

    fun addTeacher(name: String, surname: String) {
        viewModelScope.launch {
            val id = repository.insertTeacher(Teacher(name = name, surname = surname))
            selectTeacher(id.toInt())
        }
    }

    fun deleteTeacher(teacher: Teacher) {
        viewModelScope.launch {
            repository.deleteTeacher(teacher)
        }
    }

    private data class DataQueryState(
        val id: Int?,
        val teacher: Teacher?,
        val isEditing: Boolean
    )
}
