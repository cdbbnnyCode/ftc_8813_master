package org.firstinspires.ftc.teamcode.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.common.Robot;


@Autonomous(name="DummyAutoFoundationBlueTriangle")
public class DummyAutoFoundationBlueTriangle extends BaseAutonomous
{
    public void initialize(){
        Robot robot = Robot.instance();
        robot.intakelinkage.moveLinkageIn();
        robot.imu.setImmediateStart(true);
        robot.imu.initialize();
        robot.foundationhook.moveHookUp();
    }

    @Override
    public void run() throws InterruptedException
    {
        Robot robot = Robot.instance();

        robot.drivetrain.move(-0.5, 0, 0, tickToInches(25));
        robot.drivetrain.stop();

        robot.drivetrain.move(0, 0.3, 0, tickToInches(6));
        robot.drivetrain.stop();

        robot.foundationhook.moveHookDown();
        Thread.sleep(2000);

        robot.drivetrain.move(0.4, 0, 0, tickToInches(30));
        robot.drivetrain.stop();

        robot.foundationhook.moveHookUp();

        robot.drivetrain.move(0, -0.6, 0, tickToInches(35));
        robot.drivetrain.stop();
    }

    public int tickToInches(double dist){
        final double CIRCUMFERENCE = 3.14*(100/25.4);
        double ticks = (dist/CIRCUMFERENCE)*537.6;
        return (int) ticks;
    }
}
