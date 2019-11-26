package org.firstinspires.ftc.teamcode.common.actuators;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.common.motor_control.PIDMotor;
import org.firstinspires.ftc.teamcode.common.sensors.IMU;
import org.firstinspires.ftc.teamcode.common.util.Logger;

/**
 * The mecanum drivetrain
 */
public class Drivetrain
{
    public PIDMotor leftFront, rightFront;
    public PIDMotor leftBack,  rightBack;
    
    private Logger log = new Logger("Drivetrain");
    
    private IMU imu;
    
    /**
     * Create a drivetrain. Takes PIDMotors for position control ability
     * @param leftFront  The left front motor
     * @param rightFront The right front motor
     * @param leftBack   The left rear motor
     * @param rightBack  The right rear motor
     */
    public Drivetrain(PIDMotor leftFront, PIDMotor rightFront, PIDMotor leftBack, PIDMotor rightBack, IMU imu)
    {
        this.leftFront  = leftFront;
        this.rightFront = rightFront;
        this.leftBack   = leftBack;
        this.rightBack  = rightBack;
        
        this.rightFront.setDirection(DcMotorSimple.Direction.REVERSE);
        this.rightBack.setDirection(DcMotorSimple.Direction.REVERSE);
        this.imu = imu;
        
        this.leftFront.setDeadband(20);
        this.rightFront.setDeadband(20);
        this.leftBack.setDeadband(20);
        this.rightBack.setDeadband(20);
    }
    
    /**
     * Create a drivetrain. Takes PIDMotors for position control ability
     * @param leftFront  The left front motor
     * @param rightFront The right front motor
     * @param leftBack   The left rear motor
     * @param rightBack  The right rear motor
     */
    public Drivetrain(PIDMotor leftFront, PIDMotor rightFront, PIDMotor leftBack, PIDMotor rightBack)
    {
        this(leftFront, rightFront, leftBack, rightBack, null);
    }
    
    /**
     * Run the drivetrain at a constant power
     * @param forward How fast to drive forward (negative for backwards)
     * @param right   How fast to strafe to the right (negative for left)
     * @param turn    How fast to turn clockwise (negative for counterclockwise)
     */
    public void drive(double forward, double right, double turn)
    {
        leftFront.getMotor().setPower ( forward + right - turn);
        rightFront.getMotor().setPower( forward - right + turn);
        leftBack.getMotor().setPower  ( forward - right - turn);
        rightBack.getMotor().setPower ( forward + right + turn);
    }
    
    /**
     * Drive a certain distance in a certain direction
     * @param forward  How fast to drive forward
     * @param right    How fast to strafe
     * @param turn     How fast to turn
     * @param distance How far to move
     * @throws InterruptedException If an interrupt occurs
     */
    public void move(double forward, double right, double turn, int distance) throws InterruptedException
    {
        double[] powers = {
                forward + right - turn,
                forward - right + turn,
                forward - right - turn,
                forward + right + turn
        };
        
        PIDMotor[] motors = {leftFront, rightFront, leftBack, rightBack};
        
        // Start the motors
        for (int i = 0; i < 4; i++)
        {
            int sign = (motors[i].getMotor().getDirection() == DcMotorSimple.Direction.FORWARD) ? 1 : -1;
            int dist = distance * (int)Math.signum(powers[i]); // (int)(distance * Math.min(Math.abs(powers[i]), 1) * Math.signum(powers[i]));
            int target = (dist + motors[i].getCurrentPosition()); //* sign;
            log.d("Motor %d: target %d (distance %d from %d)", i, target, dist, motors[i].getCurrentPosition());
            if (powers[i] != 0)
            {
                motors[i].setPower(Math.abs(powers[i]));
                motors[i].hold(target);
            }
        }
        
        double angleOrig;
        if (imu != null)
            angleOrig = imu.getHeading();
        else
            angleOrig = 0;
        
        // Wait for the motors to finish
        boolean busy = true;
        double prevPowerOff = 0;
        while (busy)
        {
            busy = false;
            for (int i = 0; i < 4; i++)
            {
                if (motors[i].isBusy())
                {
                    busy = true;
                    break;
                }
            }
            
            // TODO TEST EXPERIMENTAL CODE
            // Adjust speed to correct for any rotation
            if (imu != null && turn == 0)
            {
                double angleError = imu.getHeading() - angleOrig;
                double powerOffset = angleError * 0.02;
                
                if (powerOffset != prevPowerOff)
                {
                    prevPowerOff = powerOffset;
                    log.d("Angle offset: %.2f (add %.3f power)", angleError, powerOffset);
                    motors[0].setPower(Math.abs(powers[0]) + powerOffset);
                    motors[1].setPower(Math.abs(powers[1]) - powerOffset);
                    motors[2].setPower(Math.abs(powers[2]) + powerOffset);
                    motors[3].setPower(Math.abs(powers[3]) - powerOffset);
                }
                
            }
            // ----------------------
            
            
            
            log.d("Encoders: %d %d %d %d",
                    motors[0].getCurrentPosition(),
                    motors[1].getCurrentPosition(),
                    motors[2].getCurrentPosition(),
                    motors[3].getCurrentPosition());
            Thread.sleep(10);
        }
        motors[0].stopHolding();
        motors[1].stopHolding();
        motors[2].stopHolding();
        motors[3].stopHolding();
    }

    public void moveVishnu(double forward, double right, double turn, int dist){
        double[] powers = {
                forward + right - turn,
                forward - right + turn,
                forward - right - turn,
                forward + right + turn
        };

        PIDMotor[] motors = {leftFront, rightFront, leftBack, rightBack};

        for (int i = 0; i<4; i++){
            motors[i].setRunMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }

        for (int i = 0; i<4; i++){
            motors[i].setTargetPosition(dist*(int)Math.signum(powers[i]) + motors[i].getCurrentPosition());

        }
        leftFront.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightFront.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftBack.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightBack.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        for (int i = 0; i<4; i++){
            motors[i].setPower(powers[i]);
        }
    }

    public void stop(){
        leftFront.getMotor().setPower(0);
        leftBack.getMotor().setPower(0);
        rightFront.getMotor().setPower(0);
        rightBack.getMotor().setPower(0);
    }
}
