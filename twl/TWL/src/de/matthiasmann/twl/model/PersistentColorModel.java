/*
 * Copyright (c) 2008-2011, Matthias Mann
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

import de.matthiasmann.twl.Color;
import java.util.prefs.Preferences;

/**
 * A Persistent color model.
 * 
 * @author Matthias Mann
 */
public class PersistentColorModel extends HasCallback implements ColorModel {
    
    private final Preferences prefs;
    private final String prefKey;

    private Color value;
    private IllegalArgumentException initialError;
    
    public PersistentColorModel(Preferences prefs, String prefKey, Color defaultValue) {
        if(prefs == null) {
            throw new NullPointerException("prefs");
        }
        if(prefKey == null) {
            throw new NullPointerException("prefKey");
        }
        if(defaultValue == null) {
            throw new NullPointerException("defaultValue");
        }
        
        this.prefs = prefs;
        this.prefKey = prefKey;
        this.value = defaultValue;
        
        try {
            String text = prefs.get(prefKey, null);
            if(text != null) {
                Color aValue = Color.parserColor(text);
                if(aValue != null) {
                    value = aValue;
                } else {
                    initialError = new IllegalArgumentException("Unknown color name: " + text);
                }
            }
        } catch (IllegalArgumentException ex) {
            initialError = ex;
        }
    }

    public IllegalArgumentException getInitialError() {
        return initialError;
    }
    
    public void clearInitialError() {
        initialError = null;
    }

    public Color getValue() {
        return value;
    }

    public void setValue(Color value) {
        if(this.value != value) {
            this.value = value;
            storeSettings();
            doCallback();
        }
    }

    private void storeSettings() {
        prefs.put(prefKey, value.toString());
    }
}
