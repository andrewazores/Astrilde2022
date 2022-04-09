package frc.robot.subsystems.BallPath;

import java.util.concurrent.TimeUnit;

import javax.swing.text.DefaultStyledDocument.ElementSpec;

import ca.team3161.lib.robot.LifecycleEvent;
import ca.team3161.lib.robot.subsystem.RepeatingPooledSubsystem;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.subsystems.BallPath.Elevator.Elevator;
import frc.robot.subsystems.BallPath.Elevator.ElevatorImpl;
import frc.robot.subsystems.BallPath.Elevator.Elevator.ElevatorAction;
import frc.robot.subsystems.BallPath.Intake.Intake;
import frc.robot.subsystems.BallPath.Intake.Intake.IntakeAction;
import frc.robot.subsystems.BallPath.Shooter.PIDShooterTrackingImpl;
import frc.robot.subsystems.BallPath.Shooter.Shooter;
import frc.robot.subsystems.BallPath.Shooter.Shooter.ShotPosition;

public class BallPathImpl extends RepeatingPooledSubsystem implements BallPath {

    private final Intake intake;
    private final Elevator elevator;
    private final Shooter shooter;
    private Spark blinkenController;
    boolean checkBall = false;
    boolean noShoot = false;
    private boolean flipped = false;
    private boolean ballSent = false;
    int startBall = 1;
    int readyShootDelay = 0;

    private volatile BallAction action = BallAction.NONE;

    public BallPathImpl(Intake intake, Elevator elevator, Shooter shooter, Spark blinkenController) {
        super(20, TimeUnit.MILLISECONDS);
        this.intake = intake;
        this.elevator = elevator;
        this.shooter = shooter;
        this.blinkenController = blinkenController;
    }

    @Override
    public void defineResources(){
        // require(intake);
        // require(elevator);
        // require(shooter);
    }

    @Override
    public void setAction(BallAction inputAction) {
        this.action = inputAction;
    }

    @Override
    public void task() {
        int ballNumber = startBall + intake.getBallsIntake() - shooter.getBallsShooter();
        SmartDashboard.putNumber("BALL NUMBER", ballNumber);


        switch (action) {
            case YES_SHOOT:
                this.shooter.setShotPosition(ShotPosition.STARTAIM);
                noShoot = false;
                break;
            case NO_SHOOT:
                this.shooter.setShotPosition(ShotPosition.STOPAIM);
                noShoot = true;
                break;
            case SHOOTGENERAL:
                this.shooter.setShotPosition(ShotPosition.GENERAL);
                if(shooter.readyToShoot()){
                    if (!elevator.ballPrimed()){
                        elevator.setAction(ElevatorAction.INDEX);
                    }
                    else if (readyShootDelay == 5){
                        elevator.setAction(ElevatorAction.PRIME);
                    }
                    else{
                        readyShootDelay++;
                    }
                }else{
                    elevator.setAction(ElevatorAction.INDEX);
                    readyShootDelay=0;
                }
                break;
            case SHOOTFENDER:
                
                
                this.shooter.setShotPosition(ShotPosition.FENDER);
                // if(shooter.readyToShoot()){
                //     if (!elevator.ballPrimed()){
                //         elevator.setAction(ElevatorAction.INDEX);
                //     }else{
                //         elevator.setAction(ElevatorAction.PRIME);
                //     }
                // }else{
                //     elevator.setAction(ElevatorAction.INDEX);
                // }
                
                if(elevator.ballPrimed() && ballNumber != 0){
                    if(ballNumber == 2 && !ballSent){
                        if(shooter.readyToShoot()){
                            elevator.setAction(ElevatorAction.PRIME);
                        }
                    }else if(ballNumber == 2 && ballSent){
                        elevator.setAction(ElevatorAction.INDEX);
                    }else if(ballNumber == 1 && shooter.readyToShoot()){
                        elevator.setAction(ElevatorAction.PRIME);
                    }

                }else if(ballNumber != 0){
                    ballSent = true;
                    elevator.setAction(ElevatorAction.INDEX);
                }else{
                    ballSent = false;
                }
                
                break;
            case NONE:
                this.intake.setAction(IntakeAction.STOP);
                this.elevator.setAction(ElevatorAction.STOP);
                this.shooter.setShotPosition(ShotPosition.NONE);
                checkBall = false;
                ballSent = false;
                flipped = false;
                break;
            case INDEX:
                this.elevator.setAction(ElevatorAction.INDEX);
                if(elevator.ballPrimed()){
                    this.intake.setAction(IntakeAction.IN);
                }else{
                    this.intake.setAction(IntakeAction.TESTIN);
                }
                checkBall = true;
                break;
            case OUT:
                this.elevator.setAction(ElevatorAction.OUT);
                this.intake.setAction(IntakeAction.TESTOUT);
                break;
            case AUTO:
            case MANUAL:
                break;
            
            default:
                intake.setAction(IntakeAction.NONE);
                elevator.setAction(ElevatorAction.NONE);
                shooter.setShotPosition(ShotPosition.NONE);
                ballSent = false;
                flipped = false;
                break;
        }

        if(noShoot){
            blinkenController.set(-0.89);
        }else if(checkBall){
            if(elevator.ballPrimed()){
                blinkenController.set(.83);
            }else{
                blinkenController.set(.61);
            }
        }else{
            if(PIDShooterTrackingImpl.canSeeTarget() == 1.0){
                blinkenController.set(0.77);
            }else{
                blinkenController.set(0.61);
            }
        }
    }

    @Override
    public void lifecycleStatusChanged(LifecycleEvent previous, LifecycleEvent current) {
        switch (current) {
            case ON_INIT:
            case ON_AUTO:
            case ON_TELEOP:
            case ON_TEST:
                this.start();
                break;
            case ON_DISABLED:
            case NONE:
            default:
                this.cancel();
                break;
        }
    }

    @Override
    public Intake getIntake(){
        return this.intake;
    }

    @Override
    public Elevator getElevator(){
        return this.elevator;
    }

    @Override
    public Shooter getShooter(){
        return this.shooter;
    }
}
