/*
 * Copyright 2018, The Android Open Source Project
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

package net.ucoz.abondarenko.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ucoz.abondarenko.trackmysleepquality.database.SleepDatabaseDao
import net.ucoz.abondarenko.trackmysleepquality.database.SleepNight
import net.ucoz.abondarenko.trackmysleepquality.formatNights


class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application,
) : AndroidViewModel(application) {

    private var viewModelJob = Job()


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private var tonight = MutableLiveData<SleepNight?>()

    val nights = database.getAllNights()


    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    val startButtonVisible = Transformations.map(tonight) {
        null == it
    }

    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }

    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    private val _navigateToSleepDataQuality = MutableLiveData<Long?>()
    val navigateToSleepDataQuality
        get() = _navigateToSleepDataQuality

    private var _showSnackbarEvent = MutableLiveData<Boolean>()

    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()

    val navigateToSleepQuality: MutableLiveData<SleepNight?>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    fun onSleepNightClicked(id: Long){
        _navigateToSleepDataQuality.value = id
    }

    fun onSleepDataQualityNavigated() {
        _navigateToSleepDataQuality.value = null
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    private suspend fun clear() {
        database.clear()
    }

    private suspend fun update(night: SleepNight) {
        database.update(night)
    }

    private suspend fun insert(night: SleepNight) {
        database.insert(night)
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

            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)

            _navigateToSleepQuality.value = oldNight
        }
    }

    fun onClear() {
        viewModelScope.launch {
            clear()

            tonight.value = null
            _showSnackbarEvent.value = true
        }
    }

    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

}

