/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.monora.uprotocol.client.android.content

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore.Images.Media
import androidx.lifecycle.liveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getBuckets(): List<ImageBucket> {
        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                Media.BUCKET_ID,
                Media.BUCKET_DISPLAY_NAME,
                Media._ID,
                Media.DATE_MODIFIED,
            ),
            "1) GROUP BY 1,(2",
            null,
            "${Media.DATE_MODIFIED} DESC"
        )?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(Media._ID)
                val bucketIdIndex = it.getColumnIndex(Media.BUCKET_ID)
                val bucketDisplayNameIndex = it.getColumnIndex(Media.BUCKET_DISPLAY_NAME)
                val dateModifiedIndex = it.getColumnIndex(Media.DATE_MODIFIED)

                val list = ArrayList<ImageBucket>(it.count)

                do {
                    list.add(
                        ImageBucket(
                            it.getLong(bucketIdIndex),
                            it.getString(bucketDisplayNameIndex),
                            it.getLong(dateModifiedIndex),
                            ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, it.getLong(idIndex))
                        )
                    )
                } while (it.moveToNext())

                list.sortBy { bucket -> bucket.name }

                return list
            }
        }

        return emptyList()
    }

    fun getImages(bucket: ImageBucket): List<Image> {
        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                Media._ID,
                Media.TITLE,
                Media.DISPLAY_NAME,
                Media.SIZE,
                Media.MIME_TYPE,
                Media.DATE_MODIFIED,
                Media.BUCKET_ID
            ),
            "${Media.BUCKET_ID} = ?",
            arrayOf(bucket.id.toString()),
            "${Media.DATE_MODIFIED} DESC"
        )?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(Media._ID)
                val titleIndex = it.getColumnIndex(Media.TITLE)
                val displayNameIndex = it.getColumnIndex(Media.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(Media.SIZE)
                val mimeTypeIndex = it.getColumnIndex(Media.MIME_TYPE)
                val dateModifiedIndex = it.getColumnIndex(Media.DATE_MODIFIED)

                val list = ArrayList<Image>(it.count)

                do {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val displayName = it.getString(displayNameIndex) ?: title

                    list.add(
                        Image(
                            id,
                            title,
                            displayName,
                            it.getLong(sizeIndex),
                            it.getString(mimeTypeIndex),
                            it.getLong(dateModifiedIndex),
                            ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id)
                        )
                    )
                } while (it.moveToNext())

                return list
            }
        }

        return emptyList()
    }
}

@Parcelize
data class ImageBucket(
    val id: Long,
    val name: String,
    val dateModified: Long,
    val thumbnailUri: Uri,
) : Parcelable

@Parcelize
data class Image(
    val id: Long,
    val title: String,
    val displayName: String,
    val size: Long,
    val mimeType: String,
    val dateModified: Long,
    val uri: Uri,
) : Parcelable {
    @IgnoredOnParcel
    var isSelected = false

    override fun equals(other: Any?): Boolean {
        return other is Image && uri == other.uri
    }
}