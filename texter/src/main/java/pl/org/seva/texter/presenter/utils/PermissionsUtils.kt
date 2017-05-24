/*
 * Copyright (C) 2016 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.org.seva.texter.presenter.utils

import android.content.pm.PackageManager

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

@Singleton
class PermissionsUtils @Inject
internal constructor() {

    private val permissionGrantedSubject = PublishSubject.create<String>()
    private val permissionDeniedSubject = PublishSubject.create<String>()

    private val rationalesShown = ArrayList<String>()

    fun permissionGrantedListener(): Observable<String> {
        return permissionGrantedSubject.hide()
    }

    fun isRationaleNeeded(permission: String): Boolean {
        return !rationalesShown.contains(permission)
    }

    fun onRationaleShown(permission: String) {
        if (isRationaleNeeded(permission)) {
            rationalesShown.add(permission)
        }
    }

    fun permissionDeniedListener(): Observable<String> {
        return permissionDeniedSubject.hide()
    }

    fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            for (permission in permissions) {
                onPermissionDenied(permission)
            }
        }
        else for (i in 0..permissions.size - 1) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permissions[i])
            } else {
                onPermissionDenied(permissions[i])
            }
        }
        closePermissionListeners()
    }

    private fun onPermissionGranted(permission: String) {
        permissionGrantedSubject.onNext(permission)
    }

    private fun onPermissionDenied(permission: String) {
        permissionDeniedSubject.onNext(permission)
    }

    private fun closePermissionListeners() {
        permissionGrantedSubject.onComplete()
        permissionDeniedSubject.onComplete()
    }

    companion object {

        val PERMISSION_ACCESS_FINE_LOCATION_REQUEST = 0
        val PERMISSION_READ_CONTACTS_REQUEST = 1
    }
}
