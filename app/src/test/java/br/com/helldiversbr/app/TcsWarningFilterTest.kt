package br.com.helldiversbr.app

import br.com.helldiversbr.app.ui.screens.tcsWarningMatrix
import org.junit.Assert.*
import org.junit.Test

class TcsWarningFilterTest {
    @Test fun zeroWarningRestoresExactOriginalArtwork() {
        assertArrayEquals(floatArrayOf(1f,0f,0f,0f,0f,0f,1f,0f,0f,0f,0f,0f,1f,0f,0f,0f,0f,0f,1f,0f),
            tcsWarningMatrix(1f,.7f,.1f,0f), .00001f)
    }
    @Test fun warningDoesNotMakeTransparentPixelsOpaque() {
        for (step in 0..15) {
            val matrix = tcsWarningMatrix(1f,.7f,.1f,step/15f*.72f)
            assertArrayEquals(floatArrayOf(0f,0f,0f,1f,0f),matrix.sliceArray(15..19),0f)
            assertEquals(0f,matrix[4],0f);assertEquals(0f,matrix[9],0f);assertEquals(0f,matrix[14],0f)
        }
    }
    @Test fun lostFilterRetainsTextureInsteadOfFlatSilhouette() {
        val matrix=tcsWarningMatrix(1f,.3f,.3f,1f)
        assertTrue(matrix[0] >= .28f);assertTrue(matrix[6] >= .28f);assertTrue(matrix[12] >= .28f)
        val dark=matrix[0]*20f+matrix[1]*20f+matrix[2]*20f
        val light=matrix[0]*200f+matrix[1]*200f+matrix[2]*200f
        assertTrue(light > dark)
    }
}
