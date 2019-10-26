package org.firstinspires.ftc.teamcode.common.actuators;

import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.common.util.DataStorage;

public class Arm
{
    private Servo extension, claw;
    private double extension_min, extension_max;
    private double claw_open, claw_closed;
    public Arm(Servo extension, Servo claw, DataStorage positions)
    {
        this.extension = extension;
        this.claw = claw;
        
        this.extension_min = positions.getDouble("extension_min", 0);
        this.extension_max = positions.getDouble("extension_max", 1);
        this.claw_open = positions.getDouble("claw_open", 1);
        this.claw_closed = positions.getDouble("claw_closed", 0);
        
        this.extension.scaleRange(extension_min, extension_max);
    }
    
    public void extend(double delta)
    {
        extension.setPosition(extension.getPosition() + delta);
    }
    
    public void closeClaw()
    {
        claw.setPosition(claw_closed);
    }
    
    public void openClaw()
    {
        claw.setPosition(claw_open);
    }
}
