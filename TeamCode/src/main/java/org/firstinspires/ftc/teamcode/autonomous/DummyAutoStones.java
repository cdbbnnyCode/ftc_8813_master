package org.firstinspires.ftc.teamcode.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.hardware.configuration.annotations.DigitalIoDeviceType;

import org.firstinspires.ftc.teamcode.autonomous.vision.SkystoneDetector;
import org.firstinspires.ftc.teamcode.common.Robot;
import org.firstinspires.ftc.teamcode.common.actuators.Drivetrain;
import org.firstinspires.ftc.teamcode.common.sensors.vision.WebcamStream;


@Autonomous(name="DummyAutoStones")
public class DummyAutoStones extends BaseAutonomous
{
    @Override
    public void run() throws InterruptedException
    {
        Robot robot = Robot.instance();
        WebcamStream externalCamera = new WebcamStream();
        SkystoneDetector skystone = new SkystoneDetector();
        externalCamera.addListener(skystone);
        externalCamera.addModifier(skystone);

        robot.newarm.resetArm();

        robot.intakelinkage.moveLinkageIn();

        robot.arm.openClaw();
        Thread.sleep(1000);

        robot.drivetrain.move(0.2, 0, 0, 1100);
        Thread.sleep(1000);
        robot.drivetrain.stop();

        robot.drivetrain.move(0, 0.2, 0, tickstoInches(4));
        robot.drivetrain.stop();

        while(!skystone.found()){
            robot.drivetrain.move(0, 0.2, 0, tickstoInches(8));
            robot.drivetrain.stop();
        }

        robot.drivetrain.move(0, 0.2, 0, tickstoInches(7));
        Thread.sleep(100);
        robot.drivetrain.stop();

        robot.slide.raiseLift(0.4);
        Thread.sleep(500);
        robot.slide.raiseLift(0);

        robot.newarm.moveArmEnc(0.4, 700);
        Thread.sleep(1000);
        robot.newarm.moveArm(0);

        robot.arm.closeClaw();
        Thread.sleep(100);

        robot.drivetrain.move(-0.4, 0, 0, tickstoInches(15));

        robot.drivetrain.move(0, -0.6, 0, tickstoInches(60));

        robot.arm.openClaw();

        robot.drivetrain.move(0, 0.6, 0, tickstoInches(30));

        externalCamera.stop();
    }

    public int tickstoInches(double dist){
        return (int) ((dist/(3.14*4))*537.6);
    }
}
