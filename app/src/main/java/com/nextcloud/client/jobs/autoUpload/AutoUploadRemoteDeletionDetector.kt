/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.nextcloud.client.database.entity.FilesystemEntity
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Queues auto uploaded files again when the user deleted them on the server while they still exist locally.
 */
class AutoUploadRemoteDeletionDetector(private val repository: FileSystemRepository) {

    companion object {
        private const val TAG = "AutoUploadRemoteDeletion"
        private const val MIN_CHECK_INTERVAL_MS = 60_000L
        private val lastCheckByFolder = ConcurrentHashMap<Long, Long>()
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun requeueFilesMissingOnServer(syncedFolder: SyncedFolder, client: OwnCloudClient) {
        val now = System.currentTimeMillis()
        val lastCheck = lastCheckByFolder[syncedFolder.id] ?: 0L
        if (now - lastCheck < MIN_CHECK_INTERVAL_MS) {
            return
        }
        lastCheckByFolder[syncedFolder.id] = now

        try {
            // files uploaded before the remote folder setting changed belong to the old target, leave them alone
            val remoteFolder = syncedFolder.remotePath.trimEnd('/') + "/"
            val entitiesByRemoteDir = repository.getUploadedEntities(syncedFolder)
                .filter { File(it.localPath!!).exists() }
                .map { it to repository.getRemotePath(it, syncedFolder) }
                .filter { (_, remotePath) -> remotePath.startsWith(remoteFolder) }
                .groupBy { (_, remotePath) -> remotePath.substringBeforeLast('/') + '/' }

            var requeued = 0
            for ((remoteDir, entries) in entitiesByRemoteDir) {
                val existingPaths = readRemoteFolder(remoteDir, client) ?: continue
                entries
                    .filter { (_, remotePath) -> remotePath !in existingPaths }
                    .forEach { (entity: FilesystemEntity, remotePath) ->
                        repository.requeueFile(entity, syncedFolder, remotePath)
                        requeued++
                    }
            }

            if (requeued > 0) {
                Log_OC.w(TAG, "$requeued file(s) deleted on server, uploading again: ${syncedFolder.remotePath}")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "remote deletion check failed: ${e.message}", e)
        }
    }

    /**
     * @return remote paths inside [remoteDir], an empty set when the folder itself was deleted,
     * or null when the server could not be asked so that nothing is uploaded twice
     */
    private fun readRemoteFolder(remoteDir: String, client: OwnCloudClient): Set<String>? {
        val result = ReadFolderRemoteOperation(remoteDir).execute(client)

        if (result.isSuccess) {
            return result.data.filterIsInstance<RemoteFile>().mapNotNull { it.remotePath }.toSet()
        }

        return if (result.code == ResultCode.FILE_NOT_FOUND) emptySet() else null
    }
}
