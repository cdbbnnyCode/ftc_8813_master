package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.autonomous.util.Config;
import org.firstinspires.ftc.teamcode.teleop.util.ArmDriver;
import org.firstinspires.ftc.teamcode.teleop.util.ServoAngleFinder;

/**
 * Main TeleOp control to control the {@link ArmDriver}.
 */
@TeleOp(name="Driver Control")
public class MainTeleOp extends OpMode {

    //The arm controller
    private ArmDriver driver;
    //Claw servo
    private Servo claw;
    //Drive motors
    private DcMotor base, extend;
    //Limit switch
    private DigitalChannel limit;
    //Extend motor minimum position
    private Integer extMin = null;
    //Range of extend motor
    private int extRange;
    //Configuration
    private Config conf = new Config(Config.configFile);
    //Maximum speed of arm servos (servopos per 200ms)
    private double maxMove = conf.getDouble("max_move", 0.02);
    //Maximum speed of waist servo (radians per 200ms)
    private double maxRotate = conf.getDouble("max_rotate_speed", 0.1);
    //Whether the claw open/close button is currently being held down
    private boolean aHeld;
    //Whether the claw is open or closed
    private boolean claw_closed;
    //The amount to close the claw
    private double clawCloseAmount = conf.getDouble("claw_closed", 0);
    //The amount to open the claw
    private double clawOpenAmount = conf.getDouble("claw_open", 0);

    @Override
    public void init() {
        //Get motors and servos from hardware map
        Servo waist = hardwareMap.servo.get("s0");
        Servo shoulder = hardwareMap.servo.get("s1");
        Servo elbow = hardwareMap.servo.get("s2");
        claw = hardwareMap.servo.get("s3");
        base = hardwareMap.dcMotor.get("base");
        extend = hardwareMap.dcMotor.get("extend");
        limit = hardwareMap.digitalChannel.get("limit");
        //Initialize arm controller
        driver = new ArmDriver(waist, shoulder, elbow);

        //Get extend motor range
        extRange = conf.getInt("ext_range", 0);

        //Set up
        setInitialPositions();
        ServoAngleFinder.create(hardwareMap);
    }

    private void setInitialPositions() {
        driver.moveTo(conf.getDouble("waist_init", 0),
                      conf.getDouble("shoulder_init", 0),
                      conf.getDouble("elbow_init", 0));
    }

    @Override
    public void loop() {
        driver.setArmX(driver.getClawX()-(gamepad1.left_stick_y * maxMove));
        driver.setArmY(driver.getClawY()-(gamepad1.right_stick_y * maxMove));
        base.setPower(gamepad1.left_stick_x * 0.5);
        driver.setWaistAngle(driver.getWaistAngle()+(gamepad1.right_stick_x * maxRotate));
        //getState same as isPressed, except for DigitalChannels (which are needed for REV sensors)
        if (!limit.getState()) {
            //Only allows user to go forward if the minimum switch has been triggered.
            if (gamepad1.dpad_down) {
                extend.setPower(-1);
            } else {
                extend.setPower(0);
            }
        } else {
            extend.setPower(0);
            extMin = extend.getCurrentPosition();
        }
        if (gamepad1.dpad_up && extMin != null && extend.getCurrentPosition() < extMin + extRange) {
            extend.setPower(1);
        } else {
            extend.setPower(0);
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {}

        //Used to be A, but that would trigger the claw when Start+A was pressed to connect gamepad1
        if (gamepad1.x) {
            if (!aHeld) {
                aHeld = true;
                claw_closed = !claw_closed;
                if (claw_closed)
                    claw.setPosition(clawCloseAmount);
                else
                    claw.setPosition(clawOpenAmount);
            }
        } else {
            aHeld = false;
        }

        if (gamepad1.left_bumper) {
            setInitialPositions();
        }

        telemetry.addData("Claw", claw_closed ? "CLOSED" : "OPEN");
        telemetry.addData("Limit Switch", limit.getState() ? "PRESSED" : "RELEASED");
        telemetry.addData("Claw X", driver.getClawX());
        telemetry.addData("Claw Y", driver.getClawY());
        telemetry.addData("Waist Position", driver.getWaistPos());
        telemetry.addData("Waist Angle", driver.getWaistAngle());
        telemetry.addData("Shoulder Position", driver.getShoulderPos());
        telemetry.addData("Shoulder Angle", driver.getShoulderAngle());
        telemetry.addData("Elbow Position", driver.getElbowPos());
        telemetry.addData("Elbow Angle", driver.getElbowAngle());
    }
}
