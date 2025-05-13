package com.example.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class QueueViewModelFactory(private val repository: YouTubeService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QueueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QueueViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}