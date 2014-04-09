/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twl.model;

/**
 * A read only list model containing all enum values of the specified enum.
 * 
 * @author Matthias Mann
 */
public class EnumListModel<T extends Enum<T>> extends SimpleListModel<T> {

    private final Class<T> enumClass;
    private final T[] enumValues;

    public EnumListModel(Class<T> enumClass) {
        if(!enumClass.isEnum()) {
            throw new IllegalArgumentException("not an enum class");
        }
        this.enumClass = enumClass;
        this.enumValues = enumClass.getEnumConstants();
    }

    public Class<T> getEnumClass() {
        return enumClass;
    }

    public T getEntry(int index) {
        return enumValues[index];
    }

    public int getNumEntries() {
        return enumValues.length;
    }

    public int findEntry(T value) {
        for(int i=0,n=enumValues.length ; i<n ; i++) {
            if(enumValues[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
