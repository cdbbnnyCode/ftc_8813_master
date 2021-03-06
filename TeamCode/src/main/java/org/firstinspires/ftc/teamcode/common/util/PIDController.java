package org.firstinspires.ftc.teamcode.common.util;

public class PIDController
{
    private volatile double target;
    private volatile double error;
    private volatile double derivative;
    private volatile double lastOutput;
    private volatile double kP, kI, kD;
    private final double integratorCutoff;
    private boolean autoIntegrate;

    private volatile double integral, prevError;

    public PIDController(double kP, double kI, double kD)
    {
        this(kP, kI, kD, true);
    }

    public PIDController(double kP, double kI, double kD, boolean autoIntegrate)
    {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        integratorCutoff = .8;
        this.autoIntegrate = autoIntegrate;
    }

    public synchronized double process(double input)
    {
        error = target - input;

        derivative = error - prevError;
        prevError = error;
        if (autoIntegrate)
        {
            integral += error;

            if (integral * kI > integratorCutoff)
            {
                integral = integratorCutoff / kI;
            } else if (integral * kI < -integratorCutoff)
            {
                integral = -integratorCutoff / kI;
            }
        }

        lastOutput = error * kP + integral * kI + derivative * kD;
        return lastOutput;
    }

    public void integrate(double input)
    {
        integral += input;
        if (integral * kI > integratorCutoff)
        {
            integral = integratorCutoff / kI;
        } else if (integral * kI < -integratorCutoff)
        {
            integral = -integratorCutoff / kI;
        }
    }

    public double getOutput()
    {
        return lastOutput;
    }

    public synchronized void setPIDConstants(double kP, double kI, double kD)
    {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
    }

    public double[] getPIDConstants()
    {
        return new double[] {kP, kI, kD};
    }

    public synchronized void setTarget(double target)
    {
        this.target = target;
    }

    public double getTarget()
    {
        return target;
    }

    public double getError()
    {
        return error;
    }

    public double getIntegral()
    {
        return integral;
    }

    public double getDerivative()
    {
        return derivative;
    }

    public synchronized void resetIntegrator()
    {
        integral = 0;
    }
}
