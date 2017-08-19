package pl.org.seva.texter.view.map

import android.support.v4.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment

fun prepareMap(f: GoogleMapHelper.() -> Unit): SupportMapFragment = GoogleMapHelper().apply(f).prepare()

infix fun SupportMapFragment.ready(f: GoogleMap.() -> Unit) {

}

class GoogleMapHelper {
    lateinit var fm: FragmentManager
    lateinit var tag: String
    var container: Int = 0

    fun prepare(): SupportMapFragment {
        var result = fm.findFragmentByTag(tag) as SupportMapFragment?
        result?: let {
            result = SupportMapFragment()
            fm.beginTransaction().add(container, result, tag).commit()
        }

        return result!!
    }


}
