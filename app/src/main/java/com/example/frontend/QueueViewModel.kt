package com.example.frontend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Collections

class QueueViewModel(private val repository: YouTubeService) : ViewModel() {
    // Use MutableLiveData to hold the list of tracks
    private val _queue = MutableLiveData<List<Track>>()
    val queue: LiveData<List<Track>> = _queue

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadInitialQueue()
    }

    private fun loadInitialQueue() {
        viewModelScope.launch {
            _isLoading.value = true
            val popularSongs = repository.searchSongs("popolar songs")
            _queue.value = popularSongs.take(10) // Initialize with 5 popular songs
            _isLoading.value = false
        }
    }

    fun addToQueue(track: Track) {
        val current = _queue.value?.toMutableList() ?: mutableListOf()
        current.add(track)
        _queue.value = current
    }

    fun removeFromQueue(position: Int) {
        val current = _queue.value?.toMutableList() ?: return
        if (position in current.indices) {
            current.removeAt(position)
            _queue.value = current
        }
    }

    fun moveItem(from: Int, to: Int) {
        val current = _queue.value?.toMutableList() ?: return
        if (from in current.indices && to in current.indices) {
            Collections.swap(current, from, to)
            _queue.value = current
        }
    }
}