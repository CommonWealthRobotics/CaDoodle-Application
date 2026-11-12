package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import javafx.application.Platform;
import javafx.scene.transform.Affine;

/**
 * Regression tests for the FX-thread flood fix in Manipulation: setGlobal()
 * must coalesce pending affine writes so a drag storm does not enqueue one
 * FX task per mouse event, while the latest pose still wins.
 */
class ManipulationCoalesceTest {

}
