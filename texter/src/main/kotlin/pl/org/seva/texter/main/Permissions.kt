/*
 * Copyright (C) 2017 Wiktor Nizio
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
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.texter.main

import android.content.pm.PackageManager

import java.util.ArrayList

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

val permissions by instance<Permissions>()

class Permissions {

    private val permissionGrantedSubject = PublishSubject.create<Pair<Int, String>>()
    private val permissionDeniedSubject = PublishSubject.create<Pair<Int, String>>()

    private val rationalesShown = ArrayList<String>()

    fun isRationaleNeeded(permission: String) = !rationalesShown.contains(permission)

    fun onRationaleShown(permission: String) {
        if (isRationaleNeeded(permission)) {
            rationalesShown.add(permission)
        }
    }

    fun permissionGrantedListener(): Observable<Pair<Int, String>> = permissionGrantedSubject.hide()

    fun permissionDeniedListener(): Observable<Pair<Int, String>> = permissionDeniedSubject.hide()

    fun onRequestPermissionsResult(requestCode : Int, permissions: Array<String>, grantResults: IntArray) =
            if (grantResults.isEmpty()) {
                for (permission in permissions) {
                    onPermissionDenied(requestCode, permission)
                }
            }
            else repeat (permissions.size) {
                if (grantResults[it] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted(requestCode, permissions[it])
                } else {
                    onPermissionDenied(requestCode, permissions[it])
                }
            }

    private fun onPermissionGranted(requestCode: Int, permission: String) =
            permissionGrantedSubject.onNext(Pair(requestCode, permission))

    private fun onPermissionDenied(requestCode: Int, permission: String) =
            permissionDeniedSubject.onNext(Pair(requestCode, permission))

    companion object {

        const val LOCATION_PERMISSION_REQUEST_ID = 0
    }
}
