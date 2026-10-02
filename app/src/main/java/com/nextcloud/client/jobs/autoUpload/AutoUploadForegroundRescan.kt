/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.FilesSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * WorkManager cannot repeat work more often than every 15 minutes, so while the app is visible the auto upload
 * folders are rescanned every minute to pick up files that were deleted on the server.
 */
class AutoUploadForegroundRescan(
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager
) : DefaultLifecycleObserver {

    companion object {
        private const val RESCAN_INTERVAL_MS = 60_000L
    }

    private var job: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        job?.cancel()
        job = owner.lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
                    syncedFolderProvider,
                    backgroundJobManager,
                    false
                )
                delay(RESCAN_INTERVAL_MS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        job?.cancel()
        job = null
    }
}
