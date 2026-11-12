package com.commonwealthrobotics.controls;


/**
 * Regression tests for the FX-thread flood fix in Manipulation: setGlobal()
 * must coalesce pending affine writes so a drag storm does not enqueue one
 * FX task per mouse event, while the latest pose still wins.
 */
class ManipulationCoalesceTest {

}
