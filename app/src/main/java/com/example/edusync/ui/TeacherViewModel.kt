package com.example.edusync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.Course
import com.example.edusync.data.Teacher
import com.example.edusync.data.TeacherAvailability
import com.example.edusync.data.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val repository: TeacherRepository
) : ViewModel() {

    private val _selectedTeacherId = MutableStateFlow<Int?>(null)
    val selectedTeacherId = _selectedTeacherId.asStateFlow()

    val teachers = repository.getAllTeachers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Seçili hocanın müsaitlik matrisi
    val availabilityMatrix = _selectedTeacherId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList<TeacherAvailability>())
        else repository.getAvailability(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Seçili hocanın dersleri
    val teacherCourses = _selectedTeacherId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList<Course>())
        else repository.getCoursesByTeacher(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTeacher(id: Int) {
        _selectedTeacherId.value = id
    }

    fun addTeacher(name: String, surname: String) {
        viewModelScope.launch {
            val id = repository.insertTeacher(Teacher(name = name, surname = surname))
            selectTeacher(id.toInt())
        }
    }

    fun toggleAvailability(dayIndex: Int, slotIndex: Int) {
        // Öğle arası (slotIndex == 4) değiştirilemez
        if (slotIndex == 4) return

        val teacherId = _selectedTeacherId.value ?: return
        val currentStatus = availabilityMatrix.value.find { 
            it.dayIndex == dayIndex && it.slotIndex == slotIndex 
        }
        
        viewModelScope.launch {
            repository.updateAvailability(
                TeacherAvailability(
                    teacherId = teacherId,
                    dayIndex = dayIndex,
                    slotIndex = slotIndex,
                    isBusy = !(currentStatus?.isBusy ?: false)
                )
            )
        }
    }
}
