package yxp.cpen502;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.logging.*;
import java.awt.Color;

import robocode.*;

import javax.xml.crypto.Data;

public class CrimsonTyphoon extends AdvancedRobot {

    private String lutFilename = getClass().getSimpleName() + "-lut.txt";

    public enum enumEnergy {zero, dying, low, medium, high}

    public enum enumDistance {veryClose, close, near, far}

    public enum enumAction {circle, retreat, advance, head2Center, fire}

    public enum enumOptionalMode {scan, performanceAction}

    static private StateActionTable5 q = new StateActionTable5(
            enumEnergy.values().length,
            enumDistance.values().length,
            enumEnergy.values().length,
            enumDistance.values().length,
            enumAction.values().length
    );

    static int totalNumRounds = 0;
    static int numRoundsTo100 = 0;
    static int numWins = 0;

    private enumEnergy currentMyEnergy = enumEnergy.high;
    private enumEnergy currentEnemyEnergy = enumEnergy.high;
    private enumDistance currentDistanceToEnemy = enumDistance.near;
    private enumDistance currentDistanceToCenter = enumDistance.near;
    private enumAction currentAction = enumAction.circle;

    private enumEnergy previousMyEnergy = enumEnergy.high;
    private enumEnergy previousEnemyEnergy = enumEnergy.high;
    private enumDistance previousDistanceToEnemy = enumDistance.near;
    private enumDistance previousDistanceToCenter = enumDistance.near;
    private enumAction previousAction = enumAction.circle;

    private enumOptionalMode optionalMode = enumOptionalMode.scan;

    // set RL
    private double gamma = 1.0;
    private double alpha = 0.5;
    private final double epsilon_initial = 0.25;
    private double epsilon = epsilon_initial;
    private boolean decayEpsilon = false;

    //previous and current Q
    private double currentQ = 0.0;
    private double previousQ = 0.0;

    // Rewards
    private final double goodReward = 1.0;
    private final double badReward = -0.25;
    private final double goodTerminalReward = 2.0;
    private final double badTerminalReward = -0.5;

    private double currentReward = 0.0;

    // Initialize states
    double myX = 0.0;
    double myY = 0.0;
    double myEnergy = 0.0;
    double enemyBearing = 0.0;
    double enemyDistance = 0.0;
    double enemyEnergy = 0.0;

    double totalReward = 0.0;

    int circleDirection = 1;


    // Logging
    static String logFilename = "CrimsonTyphoon2.0.log";
    static String logFilename2 = "TotalRewards.log";
    static LogFile log = null;
    static LogFile log2 = null;

    // get center of board
    int xMid = 0;
    int yMid = 0;

    public void run() {
        // set colors
        setBulletColor(Color.red);
        setGunColor(Color.black);
        setBodyColor(Color.red);
        setRadarColor(Color.white);

        // get coordinate of the board center
        int xMid = (int) getBattleFieldWidth() / 2;
        int yMid = (int) getBattleFieldHeight() / 2;

        // Create log file
        if (log == null) {
            System.out.print("!!!*********************!!!");
            System.out.print(logFilename);
            log = new LogFile(getDataFile(logFilename));
            log.stream.printf("Start writing log");
            log.stream.printf("gamma,   %2.2f\n", gamma);
            log.stream.printf("alpha,   %2.2f\n", alpha);
            log.stream.printf("epsilon, %2.2f\n", epsilon);
            log.stream.printf("badInstantReward, %2.2f\n", badTerminalReward);
            log.stream.printf("badTerminalReward, %2.2f\n", badTerminalReward);
            log.stream.printf("goodInstantReward, %2.2f\n", goodTerminalReward);
            log.stream.printf("goodTerminalReward, %2.2f\n\n", goodTerminalReward);
        }

        if (log2 == null){
            log2 = new LogFile((getDataFile(logFilename2)));
        }

        while (true) {

            // set epsilon to 0 after 8000 round
            if (totalNumRounds > 5000) epsilon = 0;

            System.out.println("Flag 1");

            robotMovement();
            radarMovement();

//            q.save(getDataFile("lutFile.dat"));

            if (getGunHeat() == 0)
                execute();

            // Update previous Q
            double[] x = new double[]{
                    previousMyEnergy.ordinal(),
                    previousDistanceToEnemy.ordinal(),
                    previousEnemyEnergy.ordinal(),
                    previousDistanceToCenter.ordinal(),
                    previousAction.ordinal()};

            q.train(x, computeQ(currentReward));

            optionalMode = enumOptionalMode.scan;
            execute();
        }
    }

    private void robotMovement() {
        if (Math.random() < epsilon)
            // exploit
            currentAction = selectRandomAction();
        else
            currentAction = selectBestAction(
                    myEnergy,
                    enemyDistance,
                    enemyEnergy,
                    distanceToCenter(myX, myY, xMid, yMid)
            );

        switch (currentAction) {
            case circle: {
                setTurnGunRight(enemyBearing + 90);
                setAhead(50 * circleDirection);
                break;
            }
            case fire: {
                turnGunRight(getHeading() - getGunHeading() + enemyBearing);
                setFire(3);
                break;
            }
            case advance: {
                setTurnGunRight(enemyBearing);
                setAhead(100);
                break;
            }
            case retreat: {
                setTurnRight(enemyBearing + 180);
                setAhead(100);
                break;
            }
            case head2Center: {
                double bearing = getBearingToCenter(getX(), getY(), xMid, yMid, getHeadingRadians());
                setTurnRight(bearing);
                setAhead(100);
                break;
            }
        }
    }

    private void radarMovement() {
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        myX = getX();
        myY = getY();
        enemyBearing = e.getBearing();
        enemyDistance = e.getDistance();
        enemyEnergy = e.getEnergy();
        myEnergy = getEnergy();

        // Update states
        previousMyEnergy = currentMyEnergy;
        previousDistanceToCenter = currentDistanceToCenter;
        previousDistanceToEnemy = currentDistanceToEnemy;
        previousEnemyEnergy = currentEnemyEnergy;
        previousAction = currentAction;

        currentMyEnergy = enumEnergyOf(getEnergy());
        currentDistanceToCenter = enumDistanceOf(distanceToCenter(myX, myY, xMid, yMid));
        currentDistanceToEnemy = enumDistanceOf(e.getDistance());
        currentEnemyEnergy = enumEnergyOf(e.getEnergy());
        optionalMode = enumOptionalMode.performanceAction;
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        currentReward = goodReward; totalReward += currentReward;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        currentReward = badReward; totalReward += currentReward;
    }

    @Override
    public void onDeath(DeathEvent e) {
        currentReward = badTerminalReward;
        totalReward += currentReward;
        log2.stream.printf("%2.1f\n", totalReward);
        log2.stream.flush();
        totalReward = 0;

        // Update Q, otherwise it won't be updated at the last round
        double[] x = new double[]{
                previousMyEnergy.ordinal(),
                previousDistanceToEnemy.ordinal(),
                previousEnemyEnergy.ordinal(),
                previousDistanceToCenter.ordinal(),
                previousAction.ordinal()};

        q.train(x, computeQ(currentReward));

        // stats
        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
            totalNumRounds++;
        } else {
            log.stream.printf("Round: %d - %d  win percentage, %2.1f\n", totalNumRounds - 100, totalNumRounds, 100.0 * numWins / numRoundsTo100);
            log.stream.flush();
            numRoundsTo100 = 0;
            numWins = 0;
        }

        q.save(getDataFile("lutFile.dat"));
    }

    @Override
    public void onWin(WinEvent e) {
        currentReward = goodTerminalReward;
        totalReward += currentReward;
        log2.stream.printf("%2.1f\n", totalReward);
        log2.stream.flush();
        totalReward = 0;

        // Update Q, otherwise it won't be updated at the last round
        double[] x = new double[]{
                previousMyEnergy.ordinal(),
                previousDistanceToEnemy.ordinal(),
                previousEnemyEnergy.ordinal(),
                previousDistanceToCenter.ordinal(),
                previousAction.ordinal()};

        q.train(x, computeQ(currentReward));

        // stats
        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
            totalNumRounds++;
            numWins++;
        } else {
            log.stream.printf("Round: %d - %d  win percentage, %2.1f\n", totalNumRounds - 100, totalNumRounds, 100.0 * numWins / numRoundsTo100);
            log.stream.flush();
            numRoundsTo100 = 0;
            numWins = 0;
        }

        q.save(getDataFile("lutFile.dat"));
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        super.onHitWall(e);
        avoidObstacle();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        super.onHitRobot(e);
        avoidObstacle();
    }

    public void avoidObstacle() {
        switch (currentAction) {
            case circle: {
                circleDirection = circleDirection * -1;
                break;
            }
            case advance:
            case retreat: {
                setTurnRight(30);
                setBack(50);
                execute();
                break;
            }

        }
    }

    public double computeQ(double r) {
        enumAction maxA = selectBestAction(
                currentMyEnergy.ordinal(),
                currentDistanceToEnemy.ordinal(),
                currentEnemyEnergy.ordinal(),
                currentDistanceToCenter.ordinal());

        double[] prevStateAction = new double[]{
                previousMyEnergy.ordinal(),
                previousDistanceToEnemy.ordinal(),
                previousEnemyEnergy.ordinal(),
                previousDistanceToCenter.ordinal(),
                previousAction.ordinal()};

        double[] currentStateAction = new double[]{
                currentMyEnergy.ordinal(),
                currentDistanceToEnemy.ordinal(),
                currentEnemyEnergy.ordinal(),
                currentDistanceToCenter.ordinal(),
                maxA.ordinal()};

        double prevQ = q.outputFor(prevStateAction);
        double currentQ = q.outputFor(currentStateAction);

        double updatedQ = prevQ + alpha * (r + gamma * currentQ - prevQ);
        return updatedQ;
    }

    public enumAction selectRandomAction() {
        Random rand = new Random();
        int r = rand.nextInt(enumAction.values().length);
        return enumAction.values()[r];
    }

    public enumAction selectBestAction(double e, double d, double e2, double d2) {
        int energy = enumEnergyOf(e).ordinal();
        int distance = enumDistanceOf(d).ordinal();
        int enemyEnergy = enumEnergyOf(e2).ordinal();
        int distanceToCenter = enumDistanceOf(e2).ordinal();
        double bestQ = -Double.MAX_VALUE;
        enumAction bestAction = null;

        for (int a = enumAction.circle.ordinal(); a < enumAction.values().length; a++) {
            double[] x = new double[]{energy, distance, enemyEnergy, distanceToCenter, a};
            if (q.outputFor(x) > bestQ) {
                bestQ = q.outputFor(x);
                bestAction = enumAction.values()[a];
            }
        }
        return bestAction;
    }

    public enumDistance enumDistanceOf(double distance) {
        enumDistance d = null;
        if (distance < 50) d = enumDistance.veryClose;
        else if (distance >= 50 && distance < 250) d = enumDistance.close;
        else if (distance >= 250 && distance < 500) d = enumDistance.near;
        else if (distance >= 500) d = enumDistance.far;
        return d;
    }

    public enumEnergy enumEnergyOf(double energy) {
        enumEnergy e = null;
        if (energy == 0) e = enumEnergy.zero;
        else if (energy > 0 && energy < 15) e = enumEnergy.dying;
        else if (energy >= 15 && energy < 40) e = enumEnergy.low;
        else if (energy >= 40 && energy < 60) e = enumEnergy.medium;
        else if (energy >= 60) e = enumEnergy.high;
        return e;
    }

    public double distanceToCenter(double fromX, double fromY, double toX, double toY) {
        double distance = Math.sqrt(Math.pow((fromX - toX), 2) + Math.pow((fromY - toY), 2));
        return distance;
    }

    // convert an angle to [-Pi, Pi]
    public double norm(double a) {
        while (a <= -Math.PI) a += 2 * Math.PI;
        while (a > Math.PI) a -= 2 * Math.PI;
        return a;
    }

    public double getBearingToCenter(double fromX, double fromY, double toX, double toY, double currentHeadingRadians) {
        double b = Math.PI / 2 - Math.atan2(toY - fromY, toX - fromX);
        return norm(b - currentHeadingRadians);
    }


}
