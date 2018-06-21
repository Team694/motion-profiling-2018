package org.usfirst.frc.team694.robot.commands;

import java.io.File;

import org.usfirst.frc.team694.robot.Robot;
import org.usfirst.frc.team694.robot.RobotMap;
import org.usfirst.frc.team694.util.PathGenerator;

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
	
	double maxVelocity; 
	
	int segmentNumber; 
    public DrivetrainMotionProfileJaciEncoderCommand(String nameOfPath, double maxVelocity) {
    	requires(Robot.drivetrain);
    	leftCSV = new File("/home/lvuser/Paths/" + nameOfPath + "_left_Jaci.csv");
    	rightCSV = new File("/home/lvuser/Paths/" + nameOfPath + "_right_Jaci.csv");
    	leftTraj = Pathfinder.readFromCSV(leftCSV);
        rightTraj = Pathfinder.readFromCSV(rightCSV);
        System.out.println("CSV has been locked and loaded");
    	this.maxVelocity = maxVelocity; 
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    }

    public DrivetrainMotionProfileJaciEncoderCommand(PathGenerator path) {
    	leftTraj = path.modifier.getLeftTrajectory(); 
    	rightTraj = path.modifier.getRightTrajectory();
    	maxVelocity = path.maxVelocity;
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    	leftFollower = new EncoderFollower(leftTraj);
    	rightFollower = new EncoderFollower(rightTraj);
    	leftFollower.reset();
    	rightFollower.reset();
    	//Wheel diameter in feet
    	leftFollower.configureEncoder(Robot.drivetrain.leftBottomMotor.getSensorCollection().getQuadraturePosition(), RobotMap.DRIVETRAIN_ENCODER_TICKS_PER_REVOLUTION, RobotMap.DRIVETRAIN_WHEEL_DIAMETER / 12);
    	rightFollower.configureEncoder(Robot.drivetrain.rightBottomMotor.getSensorCollection().getQuadraturePosition(), RobotMap.DRIVETRAIN_ENCODER_TICKS_PER_REVOLUTION, RobotMap.DRIVETRAIN_WHEEL_DIAMETER / 12);
    	leftFollower.configurePIDVA(SmartDashboard.getNumber("Motion Profile P", 0.0), SmartDashboard.getNumber("Motion Profile I", 0), SmartDashboard.getNumber("Motion Profile D", 0.0), 1 / maxVelocity, SmartDashboard.getNumber("Accel Gain", 0));
    	rightFollower.configurePIDVA(SmartDashboard.getNumber("Motion Profile P", 0.0), SmartDashboard.getNumber("Motion Profile I", 0), SmartDashboard.getNumber("Motion Profile D", 0.0), 1 / maxVelocity, SmartDashboard.getNumber("Accel Gain", 0));
    	segmentNumber = 0; 
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	double leftOutput = leftFollower.calculate(Robot.drivetrain.leftBottomMotor.getSensorCollection().getQuadraturePosition());
    	double rightOutput = rightFollower.calculate(Robot.drivetrain.rightBottomMotor.getSensorCollection().getQuadraturePosition());
    	double gyroHeading = Robot.drivetrain.getGyroAngle();
    	double desiredHeading = Pathfinder.r2d(leftFollower.getHeading());
    	//Pathfinder is counter-clockwise while gyro is clockwise so gyro heading is added
    	double angleDifference = Pathfinder.boundHalfDegrees(desiredHeading + gyroHeading);
    	double turn = 0.8 * (-1.0 / 80.0) * angleDifference;
    	Robot.drivetrain.tankDrive(leftOutput + turn, rightOutput - turn);
    	System.out.println("Left Power: " + (leftOutput + turn) + "Right Power: " + (rightOutput - turn));
    	segmentNumber++; 
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
    	if((leftFollower.isFinished() && rightFollower.isFinished()) || isFinishing()){
    		System.out.println("Path has finished");
    		return true; 
    	}else {
    		return false; 
    	}
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.drivetrain.stop();
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	Robot.drivetrain.stop();
    }
    
    //Checks if there are few points left and if the percent output is low
    public boolean isFinishing() {
    	return (segmentNumber <= leftTraj.length() - 5 && segmentNumber <= rightTraj.length() - 5)
    			&& (Robot.drivetrain.leftBottomMotor.getMotorOutputPercent() <= 0.05 && Robot.drivetrain.rightBottomMotor.getMotorOutputPercent() <= 0.05);
    }
}
