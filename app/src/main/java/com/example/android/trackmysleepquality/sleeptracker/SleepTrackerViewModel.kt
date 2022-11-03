/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        private val tonight = MutableLiveData<SleepNight?>()

        private val _navigateToSleepQuality = MutableLiveData<SleepNight>()



        val navigateToSleepQuality : LiveData<SleepNight> get() = _navigateToSleepQuality

        val startButtonVisible = Transformations.map(tonight) {
                it == null
        }

        val stopButtonVisible = Transformations.map(tonight) {
                it != null
        }



        private val _showSnackbarEvent = MutableLiveData<Boolean>()
        val showSnackbarEvent get() = _showSnackbarEvent

        init {
            initializeTonight()
        }

        private fun initializeTonight() {
                viewModelScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }

        private suspend fun getTonightFromDatabase() : SleepNight? {
                var tonight = database.getTonight()
                if(tonight?.endTime != tonight?.startTime) {
                        tonight = null
                }
                return tonight
        }

        fun onStartTracking() {
                viewModelScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value = getTonightFromDatabase()
                }
        }

        fun onStopTracking() {
                viewModelScope.launch {
                        val oldNight = tonight.value ?: return@launch
                        oldNight.endTime = System.currentTimeMillis()
                        update(oldNight)
                        _navigateToSleepQuality.value = oldNight
                }
        }

        fun  onClear() {
                viewModelScope.launch {
                        clear()
                        _showSnackbarEvent.value = true
                }
        }

        private suspend fun clear() {
                database.clear()
        }

        private suspend fun update(oldNight: SleepNight) {
               database.update(oldNight)
        }

        private suspend fun insert(night: SleepNight) {
            database.insert(sleepNight = night)
        }

        fun doneNavigate() {
                _navigateToSleepQuality.value = null
        }

        val nights = database.getAllNights()



        val nightsString = Transformations.map(nights) {
                        night -> formatNights(night, application.resources)
        }

        fun doneShowSnackbar() {
                _showSnackbarEvent.value = false
        }

        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

}

