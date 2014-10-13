// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

// The values must be alligned with the .js file
public enum ARDroneVideoChannel {
    ZAP_CHANNEL_HORI(1),
    ZAP_CHANNEL_VERT(2),
    ZAP_CHANNEL_LARGE_HORI_SMALL_VERT(3),
    ZAP_CHANNEL_LARGE_VERT_SMALL_HORI(4),
    UNKNOWN(5);

    private final int value;
    private ARDroneVideoChannel(int value) { this.value = value; }
    public int getValue() { return value; }
}
