// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone_pilot;

import android.util.Log;

import java.io.UnsupportedEncodingException;

public class ATCommand implements Comparable<ATCommand> {
    private static final String TAG = "ATCommand";

    private static final String COMMAND_HEADER = "AT*";

    private BaseCommand mCommand;
    private String mCommandType;

    /**
     * Usage:
     *    - New ATCommand:
     *      1. CONFIG -- new ATCommand(new ConfigCommand(STRING_NAME, STRING_VALUE));
     *      2. ANIM -- new ATCommand(new AnimCommand(INT_ANIMATION, INT_DURATION));
     *      3. REF_TAKEOFF -- new ATCommand(new TakeoffCommand());
     *      4. REF_LANDING -- new ATCommand(new LandingCommand());
     *      6. REF_EMERGENCY -- new ATCommand(new EmergencyCommand());
     *      7. FTRIM -- new ATCommand(new FtrimCommand());
     *      8. LED -- new ATCommand(new LedCommand(INT_ANIMATION, FLOAT_FREQUENCY, INT_DURATION));
     *      9. COMWDG -- new ATCommand(new ComwdgCommand());
     *      10. PCWD_HOVER -- new ATCommand(new HoverCommand());
     *      11. PCWD_MOVE -- new ATCommand(new MoveCommand(BOOL_YAW_ENABLE, FLOAT_LEFTRIHTTILT,
     *                                     FLOAT_FRONTBACKTILT, FLOAT_VERTICALSPEED, FLOAT_ANGULARSPEED));
     *      12. QUIT -- new ATCommand(new QuitCommand());
     *
     */
    public ATCommand(BaseCommand baseCommand) {
        if (baseCommand instanceof ConfigCommand) {
            mCommandType = "Config";
        } else if (baseCommand instanceof AnimCommand) {
            mCommandType = "Anim";
        } else if (baseCommand instanceof TakeoffCommand) {
            mCommandType = "Takeoff";
        } else if (baseCommand instanceof LandingCommand) {
            mCommandType = "Landing";
        } else if (baseCommand instanceof EmergencyCommand) {
            mCommandType = "Emergency";
        } else if (baseCommand instanceof FtrimCommand) {
            mCommandType = "Ftrim";
        } else if (baseCommand instanceof LedCommand) {
            mCommandType = "Led";
        } else if (baseCommand instanceof ComwdgCommand) {
            mCommandType = "Comwdg";
        } else if (baseCommand instanceof HoverCommand) {
            mCommandType = "Hover";
        } else if (baseCommand instanceof MoveCommand) {
            mCommandType = "Move";
        } else if (baseCommand instanceof QuitCommand) {
            mCommandType = "Quit";
        } else {
            throw new IllegalArgumentException("Unsupported Type: " + baseCommand.getClass().getName());
        }

        mCommand = baseCommand;
    }

    public String getCommandName() {
        return mCommand.getCommandName();
    }

    public String getCommandType() {
        return mCommandType;
    }

    public int getPriority() {
        return mCommand.getPriority();
    }
    
    private Object[] getParameters() {
        return mCommand.getParameters();
    }

    private String encodeParameter(Object o) {
        if(o instanceof Integer) {
            return o.toString();
        }

        if(o instanceof Float) {
            return Integer.toString(Float.floatToIntBits((Float) o));
        }

        if(o instanceof String) {
            return "\"" + o + "\"";
        }

        throw new IllegalArgumentException("Unsupported Type: " + o.getClass().getName() + " " + o);
    }

    private String buildParametersString() {
        StringBuffer stringBuffer = new StringBuffer();

        for(Object o : getParameters()) {
            stringBuffer.append(',').append(encodeParameter(o));
        }

        return stringBuffer.toString();
    }

    public String buildATCommandString(int sequence) {
        return COMMAND_HEADER + getCommandName() + "=" + sequence + buildParametersString() + "\r";
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof ATCommand)) {
            return false;
        }

        ATCommand atCommand = (ATCommand) o;
        return atCommand.buildATCommandString(0).equals(buildATCommandString(0));
    }

    public byte[] buildPacketBytes(int sequence) {
        try {
            return buildATCommandString(sequence).getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public int compareTo(ATCommand o) {
        if (o instanceof ATCommand) {
           return getPriority() > o.getPriority() ? 1 : -1;
        }

        return 0;
    }
}

class BaseCommand {
    static final int PRIORITY_LOW = 0;
    static final int PRIORITY_MEDIUM = 50;
    static final int PRIORITY_HIGH = 80;
    static final int PRIORITY_VERY_HIGH = 100;

    String mCommandName;

    BaseCommand(String commandName) {
        mCommandName = commandName;
    }

    String getCommandName() {
        return mCommandName;
    }

    int getPriority() {
        return PRIORITY_LOW;
    }

    Object[] getParameters() {
        return new Object[] {};
    }
}

class ConfigCommand extends BaseCommand {
    String mName;
    String mValue;

    ConfigCommand(String name, String value) {
        super("CONFIG");
        mName = name;
        mValue = value;
    }

    @Override
    Object[] getParameters() {
        return new Object[] {mName, mValue};
    }
}

class AnimCommand extends BaseCommand {
    int mAnimation;
    int mDuration;

    AnimCommand(int animation, int duration) {
        super("ANIM");
        mAnimation = animation;
        mDuration = duration;
    }

    @Override
    protected Object[] getParameters() {
        return new Object[] {mAnimation, mDuration};
    }
}

class RefCommand extends BaseCommand {
    int mValue = (1 << 18) | (1 << 20) | (1 << 22) | (1 << 24) | (1 << 28);

    RefCommand() {
        super("REF");
    }

    @Override
    protected Object[] getParameters() {
        return new Object[] {mValue};
    }

    @Override
    int getPriority() {
        return PRIORITY_MEDIUM;
    }
}

class TakeoffCommand extends RefCommand {
    TakeoffCommand() {
        mValue |= (1 << 9);
    }
}

class LandingCommand extends RefCommand {
    LandingCommand() {
    }
}

class EmergencyCommand extends RefCommand {
    EmergencyCommand() {
        mValue |= (1 << 8);
    }

    @Override
    int getPriority() {
        return PRIORITY_HIGH;
    }
}

class QuitCommand extends LandingCommand {
    QuitCommand() {
    }

    @Override
    int getPriority() {
        return PRIORITY_VERY_HIGH;
    }
}

class FtrimCommand extends BaseCommand {
    FtrimCommand() {
        super("FTRIM");
    }
}

class LedCommand extends BaseCommand {
    float mFrequence;
    int mAnimation; 
    int mDuration;

    LedCommand(int animation, float frequence, int duration) {
        super("LED");
        mAnimation = animation;
        mFrequence = frequence;
        mDuration = duration;
    }

    @Override
    protected Object[] getParameters() {
        return new Object[] {mAnimation, mFrequence, mDuration};
    }
}

class ComwdgCommand extends BaseCommand {
    ComwdgCommand() {
        super("COMWDG");
    }

    @Override
    int getPriority() {
        return PRIORITY_VERY_HIGH;
    }
}

class PcwdCommand extends BaseCommand {
    boolean mCombinedYawEnable;
    boolean mHover;

    float mAngularSpeed;
    float mFrontBackTilt;
    float mLeftRrightTilt;
    float mVerticalSpeed;

    PcwdCommand(boolean hover) {
        super("PCWD");
        mHover = hover;
    }

    @Override
    protected Object[] getParameters() {
        if (mHover) {
            return new Object[] { 0, 0f, 0f, 0f, 0f };
        }

        int mode = 1;
        if (mCombinedYawEnable) {
            mode |= (1 << 1);
        }

        return new Object[] {mode, mLeftRrightTilt, mFrontBackTilt,
                mVerticalSpeed, mAngularSpeed};
    }
}

class HoverCommand extends PcwdCommand {
    HoverCommand() {
        super(true);
    }
}

class MoveCommand extends PcwdCommand {
    MoveCommand(boolean combinedYawEnable, float leftRightTilt, float frontBackTilt,
            float verticalSpeed, float angularSpeed) {
        super(false);
        mCombinedYawEnable = combinedYawEnable;
        mLeftRrightTilt = leftRightTilt;
        mFrontBackTilt = frontBackTilt;
        mVerticalSpeed = verticalSpeed;
        mAngularSpeed = angularSpeed;
    }
}
