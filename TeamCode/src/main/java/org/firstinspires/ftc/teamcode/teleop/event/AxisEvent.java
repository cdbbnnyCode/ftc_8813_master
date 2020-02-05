package org.firstinspires.ftc.teamcode.teleop.event;

public class AxisEvent extends GamepadEvent
{
    public static final int left_stick_x  = 0,
                            left_stick_y  = 1,
                            right_stick_x = 2,
                            right_stick_y = 3,
                            left_trigger  = 4,
                            right_trigger = 5,
                            dpad_x        = 6,
                            dpad_y        = 7;
    
    public final int axis;
    public final double value;
    
    public AxisEvent(int gamepad, int axis, double value)
    {
        super("AxisEvent", gamepad);
        this.axis = axis;
        this.value = value;
    }
}
