package org.usfirst.frc.team694.robot.commands;

import java.io.File;

import org.usfirst.frc.team694.robot.Robot;
import org.usfirst.frc.team694.robot.RobotMap;

import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import jaci.pathfinder.Pathfinder;
import jaci.pathfinder.Trajectory;
import jaci.pathfinder.followers.EncoderFollower;

/**
 *
 */
public class DrivetrainMotionProfileJaciEncoderCommand extends Command {
	EncoderFollower leftFollower; 
	EncoderFollower rightFollower; 
	
	File leftCSV; 
	File rightCSV; 
	
	Trajectory leftTraj; 
	Trajectory rightTraj;

	Notifier profileProcessor; 
	double dt; 
	
    public DrivetrainMotionProfileJaciEncoderCommand(String nameOfPath) {
    	requires(Robot.drivetrain);
    	leftCSV = new File("/home/lvuser/Paths/" + nameOfPath + "_left_Jaci.csv");
    	rightCSV = new File("/home/lvuser/Paths/" + nameOfPath + "_right_Jaci.csv");
    	leftTraj = Pathfinder.readFromCSV(leftCSV);
        rightTraj = Pathfinder.readFromCSV(rightCSV);
        System.out.println("CSV has been locked and loaded"); 
    	profileProcessor = new Notifier(new RunProfile());
    	this.dt = leftTraj.get(0).dt; 
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    	leftFollower = new EncoderFollower(leftTraj);
    	rightFollower = new EncoderFollower(rightTraj);
    	leftFollower.reset();
    	rightFollower.reset();
    	//Wheel diameter in feet
    	leftFollower.configureEncoder(Robot.drivetrain.getLeftEncoderTicks(), RobotMap.DRIVETRAIN_ENCODER_TICKS_PER_REVOLUTION, RobotMap.DRIVETRAIN_WHEEL_DIAMETER / 12);
    	rightFollower.configureEncoder(Robot.drivetrain.getRightEncoderTicks(), RobotMap.DRIVETRAIN_ENCODER_TICKS_PER_REVOLUTION, RobotMap.DRIVETRAIN_WHEEL_DIAMETER / 12);
    	leftFollower.configurePIDVA(SmartDashboard.getNumber("kp", 0.0), SmartDashboard.getNumber("ki", 0), SmartDashboard.getNumber("kd", 0.0), RobotMap.kv, SmartDashboard.getNumber("ka", 0));
    	rightFollower.configurePIDVA(SmartDashboard.getNumber("kp", 0.0), SmartDashboard.getNumber("ki", 0), SmartDashboard.getNumber("kd", 0.0), RobotMap.kv, SmartDashboard.getNumber("ka", 0));
    	profileProcessor.startPeriodic(dt);
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	//Code in Runnable 
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
    	if(leftFollower.isFinished() && rightFollower.isFinished()){
    		System.out.println("Path has finished");
    		return true; 
    	}else {
    		return false; 
    	}
    }

    // Called once after isFinished returns true
    protected void end() {
    	profileProcessor.stop();
    	Robot.drivetrain.stop();
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	profileProcessor.stop();
    	Robot.drivetrain.stop();
    }
    
    class RunProfile implements java.lang.Runnable {
    	int segmentNumber = 0; 
		@Override
		public void run() {
			double leftOutput = leftFollower.calculate(Robot.drivetrain.getLeftEncoderTicks());
	    	double rightOutput = rightFollower.calculate(Robot.drivetrain.getRightEncoderTicks());
	    	double gyroHeading = Robot.drivetrain.getGyroAngle();
	    	double desiredHeading = Pathfinder.r2d(leftFollower.getHeading());
	    	//Pathfinder is counter-clockwise while gyro is clockwise so gyro heading is added
	    	double angleDifference = Pathfinder.boundHalfDegrees(desiredHeading + gyroHeading);
	    	//kg is the turn gain and is the p for the angle loop
	    	double turn = SmartDashboard.getNumber("kg", 0.08) * (-1.0 / 80.0) * angleDifference;
	    	Robot.drivetrain.tankDrive(leftOutput + turn, rightOutput - turn);
	    	System.out.println(segmentNumber + "-Left Power: " + (leftOutput + turn) + " Right Power: " + (rightOutput - turn));
	    	segmentNumber++; 
		}
    }
}
