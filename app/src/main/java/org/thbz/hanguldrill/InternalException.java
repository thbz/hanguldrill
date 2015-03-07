/*
 * Copyright (c) 2015 Thierry Bézecourt
 *
 * This file is part of HangulDrill.
 *
 * HangulDrill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HangulDrill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HangulDrill.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thbz.hanguldrill;

/**
 * Created by Thierry on 20/12/14.
 */
public class InternalException extends Exception {
    InternalException(String message) {
        super(message);
    }

    InternalException(Exception exc) {
        this(exc.getMessage());
    }
}
