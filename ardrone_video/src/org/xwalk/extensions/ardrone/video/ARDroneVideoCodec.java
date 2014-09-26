// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

// The values must be alligned with the .js file
public enum ARDroneVideoCodec {
    // A MJPG-like codec, which was the default one until 1.6.4.
    VLIB(1),
    // A h264-like codec, which should be the default one after 1.6.4.
    P264(2),
    UNKNOWN(3);

    private final int value;
    private ARDroneVideoCodec(int value) { this.value = value; }
    public int getValue() { return value; }
}
