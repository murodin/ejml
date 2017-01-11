/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.sparse;

import org.ejml.data.DMatrixRow_F64;
import org.ejml.data.Matrix_F64;
import org.ejml.data.SMatrixCC_F64;
import org.ejml.data.SMatrixTriplet_F64;

import java.util.Arrays;

import static org.ejml.sparse.cmpcol.misc.ImplCommonOps_O64.colsum;

/**
 * Contains functions for converting between row and sparse matrix formats as well as sparse to sparse.
 *
 * @author Peter Abeles
 */
public class ConvertSparseMatrix_F64 {
    public static SMatrixTriplet_F64 convert(Matrix_F64 src , SMatrixTriplet_F64 dst ) {
        if( dst == null )
            dst = new SMatrixTriplet_F64(src.getNumRows(), src.getNumCols(), 1);
        else
            dst.reshape(src.getNumRows(), src.getNumCols());

        for (int row = 0; row < src.getNumRows(); row++) {
            for (int col = 0; col < src.getNumCols(); col++) {
                double value = src.unsafe_get(row,col);
                if( value != 0.0 )
                    dst.addItem(row,col,value);
            }
        }

        return dst;
    }

    public static SMatrixTriplet_F64 convert(DMatrixRow_F64 src , SMatrixTriplet_F64 dst ) {
        if( dst == null )
            dst = new SMatrixTriplet_F64(src.numRows, src.numCols,src.numRows*src.numCols);
        else
            dst.reshape(src.numRows, src.numCols);

        int index = 0;
        for (int row = 0; row < src.numRows; row++) {
            for (int col = 0; col < src.numCols; col++) {
                double value = src.data[index++];
                if( value != 0.0 )
                    dst.addItem(row,col,value);
            }
        }

        return dst;
    }

    public static DMatrixRow_F64 convert(SMatrixTriplet_F64 src , DMatrixRow_F64 dst ) {
        if( dst == null )
            dst = new DMatrixRow_F64(src.numRows, src.numCols);
        else {
            dst.reshape(src.numRows, src.numCols);
            dst.zero();
        }

        for (int i = 0; i < src.length; i++) {
            SMatrixTriplet_F64.Element e = src.data[i];

            dst.unsafe_set(e.row, e.col, e.value);
        }

        return dst;
    }

    /**
     * Converts SMatrixTriplet_64 into a SMatrixCC_64.
     *
     * @param src Original matrix which is to be copied.  Not modified.
     * @param dst Destination. Will be a copy.  Modified.
     * @param hist Workspace.  Should be at least as long as the number of columns.  Can be null.
     */
    public static SMatrixCC_F64 convert(SMatrixTriplet_F64 src , SMatrixCC_F64 dst , int hist[] ) {
        if( dst == null )
            dst = new SMatrixCC_F64(src.numRows, src.numCols , src.length);
        else
            dst.reshape(src.numRows, src.numCols, src.length);

        if( hist == null )
            hist = new int[ src.numCols ];
        else if( hist.length >= src.numCols )
            Arrays.fill(hist,0,src.numCols, 0);
        else
            throw new IllegalArgumentException("Length of hist must be at least numCols");

        // compute the number of elements in each columns
        for (int i = 0; i < src.length; i++) {
            hist[src.data[i].col]++;
        }

        // define col_idx
        colsum(dst,hist);

        // now write the row indexes and the values
        for (int i = 0; i < src.length; i++) {
            SMatrixTriplet_F64.Element e = src.data[i];

            int index = hist[e.col]++;
            dst.row_idx[index] = e.row;
            dst.data[index] = e.value;
        }
        dst.length = src.length;

        return dst;
    }

    public static SMatrixTriplet_F64 convert(SMatrixCC_F64 src , SMatrixTriplet_F64 dst ) {
        if( dst == null )
            dst = new SMatrixTriplet_F64(src.numRows, src.numCols, src.length );
        else
            dst.reshape( src.numRows , src.numCols );

        int i0 = src.col_idx[0];
        for (int col = 0; col < src.numCols; col++) {
            int i1 = src.col_idx[col+1];

            for (int i = i0; i < i1; i++) {
                int row = src.row_idx[i];
                dst.addItem(row,col, src.data[i]);
            }
            i0 = i1;
        }

        return dst;
    }

}
