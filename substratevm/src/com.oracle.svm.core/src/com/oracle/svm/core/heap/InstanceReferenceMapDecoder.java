/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.heap;

import org.graalvm.word.Pointer;

import com.oracle.svm.core.annotate.AlwaysInline;
import com.oracle.svm.core.code.CodeInfoQueryResult;
import com.oracle.svm.core.config.ConfigurationValues;
import com.oracle.svm.core.util.ByteArrayReader;

public class InstanceReferenceMapDecoder {
    @AlwaysInline("de-virtualize calls to ObjectReferenceVisitor")
    public static boolean walkOffsetsFromPointer(Pointer baseAddress, byte[] referenceMapEncoding, long referenceMapIndex, ObjectReferenceVisitor visitor) {
        assert referenceMapIndex >= CodeInfoQueryResult.EMPTY_REFERENCE_MAP;
        assert referenceMapEncoding != null;

        int entryCount = ByteArrayReader.getS4(referenceMapEncoding, referenceMapIndex);
        assert entryCount >= 0;

        int referenceSize = ConfigurationValues.getObjectLayout().getReferenceSize();
        boolean compressed = ReferenceAccess.singleton().haveCompressedReferences();

        long entryStart = referenceMapIndex + InstanceReferenceMapEncoder.MAP_HEADER_SIZE;
        for (long idx = entryStart; idx < entryStart + entryCount * InstanceReferenceMapEncoder.MAP_ENTRY_SIZE; idx += InstanceReferenceMapEncoder.MAP_ENTRY_SIZE) {
            int offset = ByteArrayReader.getS4(referenceMapEncoding, idx);
            long count = ByteArrayReader.getU4(referenceMapEncoding, idx + 4);

            Pointer objRef = baseAddress.add(offset);
            for (int c = 0; c < count; c++) {
                final boolean visitResult = visitor.visitObjectReferenceInline(objRef, 0, compressed);
                if (!visitResult) {
                    return false;
                }
                objRef = objRef.add(referenceSize);
            }
        }
        return true;
    }
}
