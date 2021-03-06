package org.firstinspires.ftc.teamcode.common.actuators;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.common.Robot;
import org.firstinspires.ftc.teamcode.common.motor_control.PIDMotor;
import org.firstinspires.ftc.teamcode.common.sensors.AMSEncoder;
import org.firstinspires.ftc.teamcode.common.sensors.IMU;
import org.firstinspires.ftc.teamcode.common.sensors.Odometry;
import org.firstinspires.ftc.teamcode.common.sensors.OdometryEncoder;
import org.firstinspires.ftc.teamcode.common.util.GlobalDataLogger;
import org.firstinspires.ftc.teamcode.common.util.Logger;
import org.firstinspires.ftc.teamcode.common.util.concurrent.GlobalThreadPool;

import java.util.Date;

/**
 * The mecanum drivetrain
 */
public class Drivetrain
{
    public PIDMotor leftFront, rightFront;
    public PIDMotor leftBack,  rightBack;
    
    private Logger log = new Logger("Drivetrain");
    
    private IMU imu;
    
    private volatile String state = "Idle";
    private volatile double angleOffset = 0;
    
    private SpeedController controller;
    private boolean controllerEnabled;
    private boolean correctAngle;
    
    /**
     * Create a drivetrain. Takes PIDMotors for position control ability
     * @param leftFront  The left front motor
     * @param rightFront The right front motor
     * @param leftBack   The left rear motor
     * @param rightBack  The right rear motor
     */
    public Drivetrain(PIDMotor leftFront, PIDMotor rightFront, PIDMotor leftBack, PIDMotor rightBack, IMU imu, Odometry odometry)
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
        /*
        GlobalDataLogger.instance().addChannel("Left Front position", () -> Integer.toString(this.leftFront.getCurrentPosition()));
        GlobalDataLogger.instance().addChannel("Right Front position", () -> Integer.toString(this.rightFront.getCurrentPosition()));
        GlobalDataLogger.instance().addChannel("Left Rear position", () -> Integer.toString(this.leftBack.getCurrentPosition()));
        GlobalDataLogger.instance().addChannel("Right Rear position", () -> Integer.toString(this.rightBack.getCurrentPosition()));
         */
        GlobalDataLogger.instance().addChannel("Drivetrain Power", () ->
        {
            double power = Math.abs(this.leftFront.getPower()) + Math.abs(this.rightFront.getPower())
                            + Math.abs(this.leftBack.getPower()) + Math.abs(this.rightBack.getPower());
            return String.format("%.4f", power/4);
        });
        
        GlobalDataLogger.instance().addChannel("Drivetrain State", () -> state);
        controller = new SpeedController(imu, odometry);
    }
    
    /**
     * Create a drivetrain. Takes PIDMotors for position control ability
     * @param leftFront  The left front motor
     * @param rightFront The right frontI2cDeviceSynch motor
     * @param leftBack   The left rear motor
     * @param rightBack  The right rear motor
     */
    public Drivetrain(PIDMotor leftFront, PIDMotor rightFront, PIDMotor leftBack, PIDMotor rightBack)
    {
        this(leftFront, rightFront, leftBack, rightBack, null, null);
    }
    
    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior)
    {
        leftFront.getMotor().setZeroPowerBehavior(behavior);
        rightFront.getMotor().setZeroPowerBehavior(behavior);
        leftBack.getMotor().setZeroPowerBehavior(behavior);
        rightBack.getMotor().setZeroPowerBehavior(behavior);
    }
    
    /**
     * Run the drivetrain at a constant power
     * @param forward How fast to drive forward (negative for backwards)
     * @param right   How fast to strafe to the right (negative for left)
     * @param turn    How fast to turn counterclockwise (negative for clockwise)
     */
    public void drive(double forward, double right, double turn)
    {
        if (correctAngle)
        {
            if (turn == 0 && controller.getAngleInfluence() == 0) internalEnableCorrection();
            else if (turn != 0) controller.setAngleInfluence(0);
        }
        controller.drive(forward, right, turn);
    }
    
    /**
     * Drive a certain distance in a certain direction
     * @param forward  How fast to drive forward
     * @param right    How fast to strafe
     * @param turn     How fast to turn
     * @param distance How far to move
     * @throws InterruptedException If an interrupt occurs
     */
    public void move(double forward, double right, double turn, double distance) throws InterruptedException
    {
        state = "Move";
        if (turn != 0 && (forward != 0 || right != 0))
        {
            log.e("Arc turns are not supported");
            return;
        }
        if (forward == 0 && right == 0 && (turn == 0 || imu == null))
        {
            return;
        }
        
        controller.move(distance * Math.signum(forward), forward, distance * Math.signum(right), right);
        Thread.sleep(1);
        while (controller.busy)
        {
            Thread.sleep(50);
        }
        
        state = "Idle";
    }

    public void moveTimeout(double forward, double right, double turn, double distance, long timeout) throws InterruptedException
    {
        state = "Move";
        if (turn != 0 && (forward != 0 || right != 0))
        {
            log.e("Arc turns are not supported");
            return;
        }
        if (forward == 0 && right == 0 && (turn == 0 || imu == null))
        {
            return;
        }

        controller.move(distance * Math.signum(forward), forward, distance * Math.signum(right), right);
        Thread.sleep(1);
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        while (controller.busy && elapsedTime < timeout)
        {
            elapsedTime = System.currentTimeMillis()-startTime;
            Thread.sleep(50);
        }

        state = "Idle";
    }
    
    

    public void oldMove(double forward, double right, double turn, int distance) throws InterruptedException
    {
        state = "Move";
        if (turn != 0 && (forward != 0 || right != 0))
        {
            log.e("Arc turns are not supported");
            return;
        }
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
            if (powers[i] != 0)
            {
                motors[i].getMotor().setPower(powers[i] * Math.signum(distance));
            }
            Thread.sleep(6);
        }
    
        double angleOrig;
        if (imu != null)
            angleOrig = imu.getHeading();
        else
            angleOrig = 0;
        /*
        if (fwdEnc != null)
        {
            fwdEnc.resetEncoder();
            strafeEnc.resetEncoder();
        }
         */
        PIDMotor encMotor = rightBack;
        int origPos = encMotor.getCurrentPosition();
    
        // Wait for the motors to finish
        boolean busy = true;
        double prevPowerOff = 0;
        while (busy)
        {
            if (forward != 0 && Math.abs(encMotor.getCurrentPosition() - origPos) >= Math.abs(distance))
                busy = false;
            else if (right != 0 && Math.abs(encMotor.getCurrentPosition() - origPos) >= Math.abs(distance))
                busy = false;
            else if (turn != 0 && imu != null && Math.abs(imu.getHeading() - angleOrig) >= Math.abs(distance))
                busy = false;
            else
                busy = true;
    
    
            // TODO TEST EXPERIMENTAL CODE
            // Adjust speed to correct for any rotation
            /*
            if (imu != null && turn == 0)
            {
                double angleError = imu.getHeading() - angleOrig;
                double powerOffset = angleError * 0.005 * Math.signum(distance);

                if (powerOffset != prevPowerOff)
                {
                    prevPowerOff = powerOffset;
                    log.d("Angle offset: %.2f (add %.3f power)", angleError, powerOffset);
                    motors[0].getMotor().setPower(Math.abs(powers[0]) + powerOffset);
                    motors[1].getMotor().setPower(Math.abs(powers[1]) - powerOffset);
                    motors[2].getMotor().setPower(Math.abs(powers[2]) + powerOffset);
                    motors[3].getMotor().setPower(Math.abs(powers[3]) - powerOffset);
                }

                angleOffset = angleError; // For logging
            }
             */
            // ----------------------

//            log.d("Encoders: %d %d %d %d",
//                    motors[0].getCurrentPosition(),
//                    motors[1].getCurrentPosition(),
//                    motors[2].getCurrentPosition(),
//                    motors[3].getCurrentPosition());
            Thread.sleep(10);
        }
        motors[0].getMotor().setPower(0);
        motors[1].getMotor().setPower(0);
        motors[2].getMotor().setPower(0);
        motors[3].getMotor().setPower(0);
        angleOffset = 0;
        state = "Idle";
    }

    public void stop(){
        drive(0, 0, 0);
    }
    
    
    ////////////////////////////////////
    // Angle Correction
    
    private class SpeedController implements Runnable
    {
        // private IMU imu;
        // private OdometryEncoder fwdEnc, strafeEnc;
        private Odometry odometry;
        private volatile double targetAngle = 0;
        // private volatile double targetPos;
        private volatile double forward, strafe, turn;
        
        private double prevFwd, prevStrafe, prevTurn;
        
        private volatile double fwdTarget, strafeTarget;
        private volatile boolean initTarget = true;
        private volatile boolean holdPosition;
        private volatile boolean busy;
        
        private volatile double angleInfluence = 0;
        
        private long lastTick;
        private double acceleration;
        private volatile double fwdSpeed, strafeSpeed; // Speed factor for acceleration
        
        private volatile double fieldCentrAngle = 0;
        private volatile boolean useFieldCentric = false;
        
        private final double efficiency = 0.9023; // How much slower the drivetrain strafes
        
        private int updateCount;
        private long lastLog;
        
        public SpeedController(IMU imu, Odometry odometry)
        {
            // this.imu = imu;
            this.targetAngle = 0;
            
//            this.fwdEnc = fwdEnc;
//            this.strafeEnc = strafeEnc;
//
//            this.imu.setImmediateStart(true);
//            this.imu.initialize();
            
            this.odometry = odometry;
            acceleration = 1;
            
            GlobalDataLogger.instance().addChannel("Target Angle", () -> "" + targetAngle);
            GlobalDataLogger.instance().addChannel("Forward Target", () -> "" + fwdTarget);
            GlobalDataLogger.instance().addChannel("Strafe Target", () -> "" + strafeTarget);
        }
        
        public synchronized void setAngle(double angle)
        {
            this.targetAngle = angle;
        }
        
        public synchronized void setAngleInfluence(double power)
        {
            this.angleInfluence = Math.abs(power);
        }
        
        public double getAngle()
        {
            return targetAngle;
        }
        
        public double getAngleError()
        {
            return imu.getHeading() - targetAngle;
        }
        
        public double getAngleInfluence()
        {
            return angleInfluence;
        }
        
        public synchronized void drive(double forward, double strafe, double turn)
        {
            this.holdPosition = false;
            this.busy = false;
            this.forward = forward;
            this.strafe = strafe;
            this.turn = turn;
        }
        
        public synchronized void move(double fwdDist, double fwdPower, double strafeDist, double strafePower)
        {
            if (initTarget)
            {
                fwdTarget = odometry.getForwardDistance();
                strafeTarget = odometry.getStrafeDistance();
                log.d("move fwdDist=%.0f strfDist=%.0f", fwdTarget, strafeTarget);
                initTarget = false;
            }
            moveTo(fwdDist + fwdTarget, fwdPower, strafeDist + strafeTarget, strafePower);
        }
        
        public synchronized void moveTo(double fwdPos, double fwdPower, double strafePos, double strafePower)
        {
            fwdTarget = fwdPos;
            strafeTarget = strafePos;
            fwdSpeed = 0;
            strafeSpeed = 0;
            holdPosition = true;
            busy = true;
            forward = Math.abs(fwdPower);
            strafe = Math.abs(strafePower);
            log.d("moveTo fwd=%.3f strafe=%.3f power=%.3f,%.3f", fwdPos, strafePos, fwdPower, strafePower);
        }
        
        public synchronized double[] updateTarget()
        {
            double fwdOff = odometry.getForwardDistance() - fwdTarget;
            double strafeOff = odometry.getStrafeDistance() - strafeTarget;
            fwdTarget = odometry.getForwardDistance();
            strafeTarget = odometry.getStrafeDistance();
            return new double[] {fwdOff, strafeOff};
        }
    
        @Override
        public void run()
        {
            lastTick = System.nanoTime();
            while (true)
            {
                loop();
                try
                {
                    Thread.sleep(10);
                } catch (InterruptedException e)
                {
                    log.d("Interrupted");
                    break;
                }
            }
        }
        
        public synchronized void loop()
        {
            double forward = this.forward;
            double strafe = this.strafe;
            double turn = this.turn;
        
            if (angleInfluence > 0)
            {
                turn -= getAngleError() / 50 * angleInfluence;
            }
        
            if (holdPosition)
            {
                double fwdError = odometry.getForwardDistance() - fwdTarget;
                double strafeError = odometry.getStrafeDistance() - strafeTarget;
            
                double fwdGain = 1.0 / 40;
                double strafeGain = 1.0 / 40;
                
                if (forward >= 0.5)
                {
                    fwdGain = 1.0 / 70;
                }
                
                if (strafe >= 0.5)
                {
                    strafeGain = 1.0 / 70;
                }
                
                forward *= -Range.clip(fwdError * fwdGain, -1, 1);
                strafe *= -Range.clip(strafeError * strafeGain, -1, 1);
                
                if (Math.abs(forward) < 0.05 && Math.abs(strafe) < 0.05 && busy)
                {
                    log.d("Done (error=<%.0f, %.0f> from target <%.0f, %.0f>)", fwdError, strafeError, fwdTarget, strafeTarget);
                    busy = false;
                }
                
                double realFwd = fwdSpeed * Math.signum(forward);
                double realStrafe = strafeSpeed * Math.signum(strafe);
                if (Math.abs(realFwd) > Math.abs(forward)) realFwd = forward;
                if (Math.abs(realStrafe) > Math.abs(strafe)) realStrafe = strafe;
                
                /*log.d("Forward: in power=%.3f, cs power=%.3f, ramp power=%.3f, output=%.3f",
                        this.forward, forward, fwdSpeed, realFwd);*/
                /* log.d("Strafe: in power=%.3f, cs power=%.3f, ramp power=%.3f, output=%.3f",
                        this.strafe, strafe, strafeSpeed, realStrafe);*/
    
                forward = realFwd;
                strafe = realStrafe;
                
                double deltaTime = (System.nanoTime() - lastTick) / 1000000000.0;
                if (fwdSpeed < Math.abs(this.forward)) fwdSpeed += acceleration * deltaTime;
                else fwdSpeed = Math.abs(this.forward);
                
                if (strafeSpeed < Math.abs(this.strafe)) strafeSpeed += acceleration * deltaTime;
                else strafeSpeed = Math.abs(this.strafe);
                
            }
            lastTick = System.nanoTime();
            
            if (useFieldCentric)
            {
                double angle = Math.toRadians(imu.getHeading() - fieldCentrAngle);
                double realFwd = forward * Math.cos(angle) - strafe * Math.sin(angle);
                double realStrafe = strafe * Math.cos(angle) + forward * Math.sin(angle);
                forward = realFwd;
                strafe = realStrafe;
            }
            
            if (prevFwd != forward || prevStrafe != strafe || prevTurn != turn)
            {
                strafe /= efficiency; // Scale up the strafe speed to account for inefficiency
                
                prevFwd = forward;
                prevStrafe = strafe;
                prevTurn = turn;
            
                leftFront.getMotor().setPower ( forward + strafe - turn);
                rightBack.getMotor().setPower ( forward + strafe + turn);
                rightFront.getMotor().setPower( forward - strafe + turn);
                leftBack.getMotor().setPower  ( forward - strafe - turn);
            }
            
            /*
            updateCount++;
            if (System.currentTimeMillis() - lastLog > 1000)
            {
                log.d("FPS: %d", updateCount);
                updateCount = 0;
                lastLog = System.currentTimeMillis();
            }
             */
        }
    
    }
    
    /**
     * Manually update the speed controller. This should be used instead of enableAsyncLoop() when a
     * reasonably tight loop is available (i.e. in TeleOp). Should take about 1-2 ms to execute unless
     * USB communication is blocked up
     */
    public void manualLoop()
    {
        if (controllerEnabled) return;
        controller.loop();
    }
    
    /**
     * Enable the asynchronous speed controller. If this is not called, the drivetrain must be updated
     * using manualLoop()
     */
    public void enableAsyncLoop()
    {
        if (controllerEnabled) return;
        GlobalThreadPool.instance().start(controller);
        controllerEnabled = true;
    }
    
    public void enableAngleCorrection()
    {
        internalEnableCorrection();
        correctAngle = true;
    }
    
    private void internalEnableCorrection()
    {
        controller.setAngleInfluence(0.65);
        controller.setAngle(imu.getHeading());
    }
    
    public void setTargetAngle(double angle)
    {
        controller.setAngle(angle);
    }
    
    public void setAngleInfluence(double power)
    {
        controller.setAngleInfluence(power);
    }
    
    public double getAngleInfluence()
    {
        return controller.angleInfluence;
    }
    
    public void disableAngleCorrection()
    {
        correctAngle = false;
        controller.setAngleInfluence(0);
    }
    
    public void enableFieldCentric()
    {
        controller.fieldCentrAngle = imu.getHeading();
        controller.useFieldCentric = true;
    }
    
    public void disableFieldCentric()
    {
        controller.useFieldCentric = false;
    }
    
    public boolean isFieldCentric()
    {
        return controller.useFieldCentric;
    }
    
    public double[] updateTarget()
    {
        return controller.updateTarget();
    }

    public void moveDiag(double fwdDist, double fwdPow, double strafeDist, double strafePow) throws InterruptedException
    {
        controller.move(fwdDist,fwdPow,strafeDist,strafePow);
        Thread.sleep(1);
        while (controller.busy)
        {
            Thread.sleep(50);
        }
    }
}
