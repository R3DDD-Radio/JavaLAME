package net.sourceforge.lame.mp3;

/**
 * Global Type Definitions
 *
 * @author Ken
 */
public interface IterationLoop {
	void iterationLoop(final LameGlobalFlags gfp, float[][] pe, float[] ms_ratio, III_psy_ratio[][] ratio);
}
