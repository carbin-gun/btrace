/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package net.java.btrace.wireio.commands;

import net.java.btrace.api.wireio.AbstractCommand;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * This command is sent out when the BTrace engine calls
 * {@linkplain Instrumentation#retransformClasses(java.lang.Class[])} method.
 * It is followed by {@linkplain OkayCommand} command when the retransformation ends.
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
final public class RetransformationStartNotification extends AbstractCommand {
    private int numClasses;

    public RetransformationStartNotification(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
    }

    final public void write(ObjectOutput out) throws IOException {
        out.writeInt(numClasses);
    }

    final public void read(ObjectInput in)
        throws IOException, ClassNotFoundException {
        numClasses = in.readInt();
    }

    final public int getNumClasses() {
        return numClasses;
    }
    
    final public void setNumClasses(int num) {
        numClasses = num;
    }
}
