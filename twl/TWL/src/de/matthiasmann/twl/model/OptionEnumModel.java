/*
 * Copyright (c) 2008-2013, Matthias Mann
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
 * A BooleanModel which is true when the underlying EnumModel has the specified
 * option code. This can be used for radio/option buttons.
 *
 * It is not possible to set this BooleanModel to false. It can only be set to
 * false by setting the underlying EnumModel to another value. Eg by setting
 * another OptionEnumModel working on the same EnumModel to true.
 *
 * @author Matthias Mann
 * @param <T> The enum class
 */
public class OptionEnumModel<T extends Enum<T>> extends AbstractOptionModel {

    protected final EnumModel<T> optionState;
    protected final T optionCode;

    public OptionEnumModel(EnumModel<T> optionState, T optionCode) {
        if(optionState == null) {
            throw new NullPointerException("optionState");
        }
        if(optionCode == null) {
            throw new NullPointerException("optionCode");
        }

        this.optionState = optionState;
        this.optionCode = optionCode;
    }

    public boolean getValue() {
        return optionState.getValue() == optionCode;
    }

    /**
     * If value is true, then the underlying EnumModel is set to the
     * option code of this OptionEnumModel.
     *
     * if value if false then nothing happens.
     *
     * @param value the new value of this BooleanModel
     */
    public void setValue(boolean value) {
        if(value) {
            optionState.setValue(optionCode);
        }
    }

    @Override
    protected void installSrcCallback(Runnable cb) {
        optionState.addCallback(cb);
    }

    @Override
    protected void removeSrcCallback(Runnable cb) {
        optionState.removeCallback(cb);
    }
    
}
