package yxp.cpen502;

import java.util.Random;

public class Layer {
    // Every layer has two pointers pointing to the next and the previous layer
    private Layer prevLayer;
    private Layer nextLayer;

    private final int N; // number of values
    private boolean binary; // Binary or bipolar

    private double[] values;
    private final double[] deltas;
    private double[][] weights;
    private double[][] oldWeights;
    // private double[] deltas;

    public Layer(int numValues, boolean isBinary){
        N = numValues;
        values = new double[N+1];
        values[N] = 1.0d;
        deltas = new double[N];
        binary = isBinary;
    }

    public void linkLayer(Layer prev, Layer next){
        prevLayer = prev;
        nextLayer = next;
        prevLayer.nextLayer = this;
        nextLayer.prevLayer = this;

    }

    // Initialize the weights with random values
    public void initWeights(){
        double lower = -0.5d;
        double upper = 0.5d;
        if (nextLayer == null) {
            weights = null;
            oldWeights = null;
            return;
        } else {
            weights = new double[N + 1][nextLayer.N];
            oldWeights = new double[N + 1][nextLayer.N];
        }
        for (int j = 0; j < nextLayer.N; j++) {
            for (int i = 0; i < N + 1; i++) {
                Random random = new Random();
                weights[i][j] = random.nextDouble() * (upper - lower) + lower;
                oldWeights[i][j] = weights[i][j];
            }
        }
    }

    public double[] getValues() {
        return values;
    }

    public double[][] getWeights() {
        return weights;
    }

    public Layer getPrevLayer(){
        return prevLayer;
    }

    public Layer getNextLayer(){
        return nextLayer;
    }

    public  int getN() { return N; }

    public double sigmoid(double x){
        if (binary)
            return 1 / (1 + Math.exp(-x));
        else
            return 2 / (1 + Math.exp(-x)) - 1;
    }

    public void setValues(double[] argValues){
        if (argValues.length != N)
            throw new NullPointerException("Array size doesn't match.");
        values = argValues;
    }

    public void setWeights(double[][] argWeights){
        if (argWeights.length != N+1 || argWeights[0].length != nextLayer.N)
            throw new NullPointerException("Array size doesn't match.");
        weights = argWeights;
    }

    public void setInput(double[] input){
        for (int i = 0; i < input.length; i++)
            values[i] = input[i];
    }

    // forward propagate will calculate the value of the next layer
    public void forwardPropagation(){
        if (nextLayer == null)
            throw new NullPointerException("Cannot perform a forward-propagation on an output layer.");
        for (int i = 0; i < nextLayer.N; i++){
            nextLayer.values[i] = 0.0d;
            for (int j = 0; j < N + 1; j++){
                nextLayer.values[i] += weights[j][i]*values[j];
            }
            nextLayer.values[i] = sigmoid(nextLayer.values[i]);
        }
    }

    public void backwardPropagation(double momentum, double learningRate, double[] targetOutput){
        if (prevLayer == null)
            throw new NullPointerException("Cannot perform a backward-propagation on an input layer.");
        if (nextLayer == null) {
            if (targetOutput.length != N)
                throw new NullPointerException("The target output length doesn't match!");
            for (int i = 0; i < N; i++) {
                deltas[i] = 0.0d;
                double cost = targetOutput[i] - values[i];
                double derivative;
                if (binary)
                    derivative = values[i] * (1 - values[i]);
                else
                    derivative = 0.5 * (values[i] + 1) * (1 - values[i]);
                deltas[i] = derivative * cost;
            }
        } else {
            for (int i = 0; i < N; i++) {
                deltas[i] = 0.0d;
                double derivative;
                if (binary)
                    derivative = values[i] * (1 - values[i]);
                else
                    derivative = (values[i] + 1) * 0.5 * (1 - values[i]);
                for (int j = 0; j < nextLayer.N; j++)
                    deltas[i] += nextLayer.deltas[j] * weights[i][j];
                deltas[i] = derivative * deltas[i];
            }
        }
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < prevLayer.N + 1; i++){
                double deltaWeight = prevLayer.weights[i][j] - prevLayer.oldWeights[i][j];
                prevLayer.oldWeights[i][j] = prevLayer.weights[i][j];
                prevLayer.weights[i][j] += learningRate * deltas[j] * prevLayer.values[i] + momentum * deltaWeight;
            }
        }
    }

    public void setBinary(boolean isBinary) {
        binary = isBinary;
    }

    public void printLayer(){
        System.out.print("Weights: ");
        if (nextLayer != null) {
            for (int i = 0; i < N + 1; i++){
                for (int j = 0; j < nextLayer.N; j++)
                    System.out.print(weights[i][j] + " ");
            }
        }
        System.out.println(" ");
        System.out.print("Values: ");
        for (int i = 0; i < N + 1; i++)
            System.out.print(values[i] + " ");
    }
}
