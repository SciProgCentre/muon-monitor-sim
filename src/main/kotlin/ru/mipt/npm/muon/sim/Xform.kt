/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ru.mipt.npm.muon.sim

import javafx.scene.Group
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate

class Xform : Group {

    enum class RotateOrder {
        XYZ, XZY, YXZ, YZX, ZXY, ZYX
    }

    var t = Translate()
    var p = Translate()
    var ip = Translate()
    var rx = Rotate()

    init {
        rx.axis = Rotate.X_AXIS
    }

    var ry = Rotate()

    init {
        ry.axis = Rotate.Y_AXIS
    }

    var rz = Rotate()

    init {
        rz.axis = Rotate.Z_AXIS
    }

    var s = Scale()

    constructor() : super() {
        transforms.addAll(t, rz, ry, rx, s)
    }

    constructor(rotateOrder: RotateOrder) : super() {
        // choose the order of rotations based on the rotateOrder
        when (rotateOrder) {
            Xform.RotateOrder.XYZ -> transforms.addAll(t, p, rz, ry, rx, s, ip)
            Xform.RotateOrder.XZY -> transforms.addAll(t, p, ry, rz, rx, s, ip)
            Xform.RotateOrder.YXZ -> transforms.addAll(t, p, rz, rx, ry, s, ip)
            Xform.RotateOrder.YZX -> transforms.addAll(t, p, rx, rz, ry, s, ip)  // For Camera
            Xform.RotateOrder.ZXY -> transforms.addAll(t, p, ry, rx, rz, s, ip)
            Xform.RotateOrder.ZYX -> transforms.addAll(t, p, rx, ry, rz, s, ip)
        }
    }

    fun setTranslate(x: Double, y: Double, z: Double) {
        t.x = x
        t.y = y
        t.z = z
    }

    fun setTranslate(x: Double, y: Double) {
        t.x = x
        t.y = y
    }

    // Cannot override these methods as they are final:
    // public void setTranslateX(double x) { t.setX(x); }
    // public void setTranslateY(double y) { t.setY(y); }
    // public void setTranslateZ(double z) { t.setZ(z); }
    // Use these methods instead:
    fun setTx(x: Double) {
        t.x = x
    }

    fun setTy(y: Double) {
        t.y = y
    }

    fun setTz(z: Double) {
        t.z = z
    }

    fun setRotate(x: Double, y: Double, z: Double) {
        rx.angle = x
        ry.angle = y
        rz.angle = z
    }

    fun setRotateX(x: Double) {
        rx.angle = x
    }

    fun setRotateY(y: Double) {
        ry.angle = y
    }

    fun setRotateZ(z: Double) {
        rz.angle = z
    }

    fun setRx(x: Double) {
        rx.angle = x
    }

    fun setRy(y: Double) {
        ry.angle = y
    }

    fun setRz(z: Double) {
        rz.angle = z
    }

    fun setScale(scaleFactor: Double) {
        s.x = scaleFactor
        s.y = scaleFactor
        s.z = scaleFactor
    }

    fun setScale(x: Double, y: Double, z: Double) {
        s.x = x
        s.y = y
        s.z = z
    }

    // Cannot override these methods as they are final:
    // public void setScaleX(double x) { s.setX(x); }
    // public void setScaleY(double y) { s.setY(y); }
    // public void setScaleZ(double z) { s.setZ(z); }
    // Use these methods instead:
    fun setSx(x: Double) {
        s.x = x
    }

    fun setSy(y: Double) {
        s.y = y
    }

    fun setSz(z: Double) {
        s.z = z
    }

    fun setPivot(x: Double, y: Double, z: Double) {
        p.x = x
        p.y = y
        p.z = z
        ip.x = -x
        ip.y = -y
        ip.z = -z
    }

    fun reset() {
        t.x = 0.0
        t.y = 0.0
        t.z = 0.0
        rx.angle = 0.0
        ry.angle = 0.0
        rz.angle = 0.0
        s.x = 1.0
        s.y = 1.0
        s.z = 1.0
        p.x = 0.0
        p.y = 0.0
        p.z = 0.0
        ip.x = 0.0
        ip.y = 0.0
        ip.z = 0.0
    }

    fun resetTSP() {
        t.x = 0.0
        t.y = 0.0
        t.z = 0.0
        s.x = 1.0
        s.y = 1.0
        s.z = 1.0
        p.x = 0.0
        p.y = 0.0
        p.z = 0.0
        ip.x = 0.0
        ip.y = 0.0
        ip.z = 0.0
    }

    override fun toString(): String {
        return "Xform[t = (" +
                t.x + ", " +
                t.y + ", " +
                t.z + ")  " +
                "r = (" +
                rx.angle + ", " +
                ry.angle + ", " +
                rz.angle + ")  " +
                "s = (" +
                s.x + ", " +
                s.y + ", " +
                s.z + ")  " +
                "p = (" +
                p.x + ", " +
                p.y + ", " +
                p.z + ")  " +
                "ip = (" +
                ip.x + ", " +
                ip.y + ", " +
                ip.z + ")]"
    }
}
