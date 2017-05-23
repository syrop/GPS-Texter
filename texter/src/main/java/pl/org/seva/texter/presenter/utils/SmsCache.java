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

package pl.org.seva.texter.presenter.utils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.texter.model.SmsLocation;

@Singleton
public class SmsCache {

    private boolean mock = true;

    private final List<SmsLocation> list;

    @Inject
    SmsCache() {
        list = new ArrayList<>();
        list.add(new SmsLocation());
    }

    public List<SmsLocation> getList() {
        return list;
    }

    public void add(SmsLocation model) {
        if (mock) {
            list.clear();
            mock = false;
        }
        list.add(model);
    }
}
