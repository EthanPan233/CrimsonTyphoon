package yxp.cpen502;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.logging.*;
import java.awt.Color;

import robocode.*;

import javax.xml.crypto.Data;

public class CrimsonTyphoon2_0 extends AdvancedRobot {

    static boolean netInitialized = false;

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

    static private Net MyNet = new Net(0.2, 0.9, false);

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
    private double gamma = 0.75;
    private double alpha = 0.5;
    private final double epsilon_initial = 2.5;
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

    int circleDirection = 1;


    // Logging
    static String logFilename = "CrimsonTyphoon2.0_20_0.48.log";
    static LogFile log = null;

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

        if (netInitialized == false) {
            log.stream.printf("If initialized: %b\n", netInitialized);
            netInitialized = true;
//            // load lut
//            log.stream.printf("Start to load LUT\n");
//            try {
//                q.load("/media/ethan/Yuxuan/Academy/Cources/CPEN_502_Architecture_of_learning_syetem/Project/out/production/CPEN502/yxp/cpen502/CrimsonTyphoon2_0.data/lutFile.dat");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            log.stream.printf("LUT loaded\n");

            // Initialize NN
            log.stream.printf("Initialize NN\n");
            MyNet.addLayer(5);
            MyNet.addLayer(20);
            MyNet.addLayer(1);
            MyNet.initNet();
            String nnFile = "/media/ethan/Yuxuan/Academy/Cources/CPEN_502_Architecture_of_learning_syetem/Project/out/production/CPEN502/yxp/cpen502/CrimsonTyphoon2_0.data/NN_20.0.48.dat";
            MyNet.loadNet(nnFile);
            log.stream.printf("NN initialized\n");
        }
        //

        while (true) {
            // set epsilon to 0 after 8000 round
            if (totalNumRounds > 10000) epsilon = 0;

            robotMovement();
            radarMovement();

            if (getGunHeat() == 0)
                execute();

//            // Update previous Q
//            double[] x = new double[]{
//                    previousMyEnergy.ordinal(),
//                    previousDistanceToEnemy.ordinal(),
//                    previousEnemyEnergy.ordinal(),
//                    previousDistanceToCenter.ordinal(),
//                    previousAction.ordinal()};
//
//            q.train(x, computeQ(currentReward));

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
        currentReward = goodReward;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        currentReward = badReward;
    }

    @Override
    public void onDeath(DeathEvent e) {
        currentReward = badTerminalReward;

        // Update Q, otherwise it won't be updated at the last round
        double[] x = new double[]{
                previousMyEnergy.ordinal(),
                previousDistanceToEnemy.ordinal(),
                previousEnemyEnergy.ordinal(),
                previousDistanceToCenter.ordinal(),
                previousAction.ordinal()};

        MyNet.backPropagateOnce(x, new double[] {computeQ(currentReward)});

        // stats
        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
            totalNumRounds++;
        } else {
//            log.stream.printf("win percentage, %2.1f\n", 100.0 * numWins / numRoundsTo100);
            log.stream.printf("Round: %d - %d  win percentage, %2.1f\n", totalNumRounds - 100, totalNumRounds, 100.0 * numWins / numRoundsTo100);

            log.stream.flush();
            numRoundsTo100 = 0;
            numWins = 0;
        }
    }

    @Override
    public void onWin(WinEvent e) {
        currentReward = goodTerminalReward;

        // Update Q, otherwise it won't be updated at the last round
//        double[] x = new double[]{
//                previousMyEnergy.ordinal(),
//                previousDistanceToEnemy.ordinal(),
//                previousEnemyEnergy.ordinal(),
//                previousDistanceToCenter.ordinal(),
//                previousAction.ordinal()};
//
//        q.train(x, computeQ(currentReward));

        // stats
        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
            totalNumRounds++;
            numWins++;
        } else {
            System.out.printf("win percentage, %2.1f\n", 100.0 * numWins / numRoundsTo100);
            log.stream.printf("Round: %d - %d  win percentage, %2.1f\n", totalNumRounds - 100, totalNumRounds, 100.0 * numWins / numRoundsTo100);
            log.stream.flush();
            numRoundsTo100 = 0;
            numWins = 0;
        }
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

        double prevQ = MyNet.getOutput(prevStateAction)[0];
        double currentQ = MyNet.getOutput(currentStateAction)[0];

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

        for (int a = CrimsonTyphoon.enumAction.circle.ordinal(); a < CrimsonTyphoon.enumAction.values().length; a++) {
            double[] x = new double[]{energy, distance, enemyEnergy, distanceToCenter, a};
            if (MyNet.getOutput(x)[0] > bestQ) {
                bestQ = MyNet.getOutput(x)[0];
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

    private double[][] loadInputs(StateActionTable5 q){
        int numDim1 = enumEnergy.values().length;
        int numDim2 = enumDistance.values().length;
        int numDim3 = enumEnergy.values().length;
        int numDim4 = enumDistance.values().length;
        int numDim5 = enumAction.values().length;

        int numStates = numDim1 * numDim2 * numDim3 * numDim4 * numDim5;
        double[][] inputs = new double[numStates][5];

        for (int a = 0; a < numDim1; a ++){
            for (int b = 0; b < numDim2; b ++){
                for (int c = 0; c < numDim3; c ++){
                    for (int d = 0; d < numDim4; d ++){
                        for (int e = 0; e < numDim5; e ++){
                            for (int i = 0; i < 5; i++) {
                                int j = a * b * c * d * e + b * c * d * e + c * d * e + d * e + e;
                                switch (i){
                                    case 1: inputs[j][i] = a;
                                    case 2: inputs[j][i] = b;
                                    case 3: inputs[j][i] = c;
                                    case 4: inputs[j][i] = d;
                                    case 5: inputs[j][i] = e;
                                }
                            }
                        }
                    }
                }
            }
        }
        return inputs;
    }

    private double[][] loadOutputs(StateActionTable5 q) {
        int numDim1 = enumEnergy.values().length;
        int numDim2 = enumDistance.values().length;
        int numDim3 = enumEnergy.values().length;
        int numDim4 = enumDistance.values().length;
        int numDim5 = enumAction.values().length;

        int numStates = numDim1 * numDim2 * numDim3 * numDim4 * numDim5;
        double[][] outputs = new double[numStates][1];

        for (int a = 0; a < numDim1; a ++){
            for (int b = 0; b < numDim2; b ++){
                for (int c = 0; c < numDim3; c ++){
                    for (int d = 0; d < numDim4; d ++){
                        for (int e = 0; e < numDim5; e ++) {
                            int j = a * b * c * d * e + b * c * d * e + c * d * e + d * e + e;
                            double[] stateArray = {Double.valueOf(a), Double.valueOf(b), Double.valueOf(c), Double.valueOf(d), Double.valueOf(e)};
                            outputs[j][0] = q.outputFor(stateArray);
                        }
                    }
                }
            }
        }
        return outputs;
    }
}
