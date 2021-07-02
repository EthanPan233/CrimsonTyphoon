package yxp.cpen502;

import java.io.*;
import java.util.Vector;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import robocode.*;


public class Net {
    private Vector<Layer> layers = new Vector<>();
    private final double learningRate;
    private final double momentum;
    private int numLayers;
    private double[] targetOutput;
    private boolean binary; // Binary or bipolar
    public int numEpoch;
    public ArrayList<String> costList = new ArrayList<>();


    public Net(double argLearningRate, double argMomentum, boolean isBinary) {
        learningRate = argLearningRate;
        momentum = argMomentum;
        numLayers = 0;
        binary = isBinary;
    }

    public void printNet() {
        for (int i = 0; i < numLayers; i++) {
            System.out.print("Layer " + (i + 1) + ": ");
            layers.get(i).printLayer();
            System.out.println(" ");
        }
    }

    public int getLayerNumValue(int idxLayer) {
        return layers.get(idxLayer).getValues().length;
    }

    public Layer getLayer(int idxLayer) {
        return layers.get(idxLayer);
    }


    public void addLayer(int numValues) {
        numLayers++;
        Layer newLayer = new Layer(numValues, binary);
        layers.add(newLayer);
        if (numLayers > 2) {
            for (int i = 1; i < numLayers - 1; i++) {
                layers.get(i).linkLayer(layers.get(i - 1), layers.get(i + 1));
            }
        }
    }

    public void setTargetOutput(double[] argTargetOutput) {
        targetOutput = argTargetOutput;
    }

    public void initNet() {
        for (Layer layer : layers) {
            layer.initWeights();
        }
        costList.clear();
    }

    public void forwardPropagation() {
        for (int i = 0; i < numLayers - 1; i++) {
            layers.get(i).forwardPropagation();
//            System.out.print("Perform forward propagation on layer: ");
//            System.out.print(i);
        }
    }

    public void backwardPropagation() {
        for (int i = numLayers - 1; i > 0; i--)
            layers.get(i).backwardPropagation(momentum, learningRate, targetOutput);
    }

    public void trainNet(double[][] inputs, double[][] targetOutput) {
        initNet();

        int epoch = 0;
        double cost = 99999;
        while (cost > 0.05d && epoch < 20000) {

            cost = 0.0d;
            numEpoch = ++epoch;
            for (int i = 0; i < inputs.length; i++) {
//                System.out.print("input: "); System.out.println(i);
                layers.get(0).setInput(inputs[i]); //set inputs
                setTargetOutput(targetOutput[i]);
//                System.out.print("set input: "); printNet();
                forwardPropagation();
//                System.out.print("forward: "); printNet();
                backwardPropagation();
//                System.out.print("backward: "); printNet();

                for (int j = 0; j < layers.lastElement().getN(); j++) {
                    cost += Math.pow(layers.lastElement().getValues()[j] - targetOutput[i][j], 2)/2;
                }
                cost /= layers.lastElement().getN();
            }
            costList.add(Double.toString(cost));
//            if (epoch % 100 == 0)
//                System.out.println("epoch: " + epoch + "; cost: " + costList.get(epoch-1) + "; output: " + layers.lastElement().getValues()[0]);
        }
    }

    public double[] getOutput(double[] input){
        layers.get(0).setInput(input);
        forwardPropagation();
        return layers.lastElement().getValues();
    }

    public void saveCostList(){
        try {
            Files.write(Paths.get("./error.text"), costList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void backPropagateOnce(double[] input, double[] targetOutput){
        layers.get(0).setInput(input); //set inputs
        setTargetOutput(targetOutput);
        forwardPropagation();
        backwardPropagation();
    }

    public void saveNet(String filename){
        try{
            // write cost
            BufferedWriter outputWriter = null;
            outputWriter = new BufferedWriter(new FileWriter(filename));

            int numLayers = 3;

            for (int i = 0; i < numLayers - 1; i++){
//                System.out.println("Layer " + i + "; Weights: " + layers.get(i).getWeights().length + " " + layers.get(i).getWeights()[0].length);
                int numCurrentNeurons = layers.get(i).getN();
                int numNextNeurons = layers.get(i+1).getN();
                for (int j = 0; j < numCurrentNeurons + 1; j++){
                    for (int k = 0; k < numNextNeurons; k++){
                        outputWriter.write(Double.toString(layers.get(i).getWeights()[j][k]));
                        outputWriter.newLine();
                    }
                }
            }

            outputWriter.flush();
            outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadNet(String fileName){
        BufferedReader reader;
        int numNeurons = 0;
        double[] values = new double[50];
        try {
            reader = new BufferedReader(new FileReader(fileName));

            int numLayers = 3;

            for (int i = 0; i < numLayers - 1; i++){
                int numCurrentNeurons = layers.get(i).getN();
                int numNextNeurons = layers.get(i+1).getN();
                double[][] weights = new double[numCurrentNeurons+1][numNextNeurons];
                for (int j = 0; j < numCurrentNeurons + 1; j++) {
                    for (int k = 0; k < numNextNeurons; k++) {
                        weights[j][k] = Double.parseDouble(reader.readLine());
                    }
                }
                layers.get(i).setWeights(weights);
            }
            reader.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

}