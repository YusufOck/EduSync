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

    val teachers = repository.getAllTeachers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
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
        
        // Admin düzenleme yaparken veya hoca öneriyi incelerken proposal oku
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

    fun submitAdminProposal(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            // Admin öneri sunarken hoca notunu temizler, kendi notunu yazar.
            repository.updateScheduleStatus(id, ScheduleStatus.ADMIN_PROPOSAL, adminNote = note, teacherNote = "")
            _isAdminEditing.value = false
        }
    }

    fun approveTeacherRequest(note: String = "") {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.updateScheduleStatus(id, ScheduleStatus.APPROVED, adminNote = note, teacherNote = "")
        }
    }

    fun rejectTeacherRequest(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.updateScheduleStatus(id, ScheduleStatus.REJECTED, adminNote = note, teacherNote = "")
        }
    }

    // --- TEACHER ACTIONS ---

    fun approveAdminProposal() {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.applyProposal(id) // Öneriyi kalıcı hale getir
            repository.updateScheduleStatus(id, ScheduleStatus.APPROVED, adminNote = "", teacherNote = "")
        }
    }

    fun rejectAdminProposal(note: String) {
        val id = _selectedTeacherId.value ?: return
        viewModelScope.launch {
            repository.discardProposal(id) // Öneriyi sil
            // Admin notunu sil, hocanın neden reddettiğini teacherNote'a yaz (Admin görsün)
            repository.updateScheduleStatus(id, ScheduleStatus.REJECTED, adminNote = "", teacherNote = note)
        }
    }

    // --- COMMON ---

    fun toggleAvailability(dayIndex: Int, slotIndex: Int) {
        if (slotIndex == 4) return
        val teacherId = _selectedTeacherId.value ?: return
        if (!_isAdminEditing.value) return // Sadece admin düzenleyebilir
        
        val currentStatus = availabilityMatrix.value.find { 
            it.dayIndex == dayIndex && it.slotIndex == slotIndex 
        }
        val newBusy = !(currentStatus?.isBusy ?: false)
        
        viewModelScope.launch {
            repository.updateProposal(
                TeacherAvailability(
                    teacherId = teacherId,
                    dayIndex = dayIndex,
                    slotIndex = slotIndex,
                    isBusy = newBusy
                )
            )
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
}
