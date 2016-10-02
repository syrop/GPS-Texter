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

package pl.org.seva.texter.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;

import pl.org.seva.texter.R;
import pl.org.seva.texter.databinding.ActivityHomeLocationBinding;

/**
 * Created by wiktor on 04.09.16.
 */

public class HomeLocationActivity extends AppCompatActivity {

    private GoogleMap map;
    private Button useCurrentButton;

    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHomeLocationBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_home_location);

        MapsInitializer.initialize(this);
        mapFragment = binding.map;
    }

    public void onUseCurrentLocation(View v) {

    }
}
