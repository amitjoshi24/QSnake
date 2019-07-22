import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
public class NNBrain{
    private ArrayList<Byte> firstMoves;

    private ArrayList<Double[][]> weightMatList;
    private ArrayList<Double[]> biasMatList;
    private static int[] structure = {10, 5, 4};
    private static String[] activationStructure = {"X", "TANH", "TANH"};

    //private static int[] structure = {6, 5, 4};
    //private static String[] activationStructure = {"X", "SIGMOID", "SIGMOID"};
    //private static int[] structure = {2, 4};
    //private static String[] activationStructure = {"X", "SIGMOID"};
    private int randomSeed = 724;
    private HashMap<Integer, Double> lowerGeneValue; //is used to set the lower gene value for initialization, mutation can cause lower values
    private HashMap<Integer, Double> upperGeneValue; //is used to set the upper gene value for initial population, mutation can cause higher values

    private double constantFactor = 1;
    public NNBrain(ArrayList<Double[][]> weightMatList, ArrayList<Double[]> biasMatList){
        this.weightMatList = weightMatList;
        this.biasMatList = biasMatList;

    }

    public static Double[][] multiplyMatrices(Double[][] firstMatrix, Double[][] secondMatrix) {
        int r1 = firstMatrix.length;
        int c1 = secondMatrix.length;
        int c2 = secondMatrix[0].length;
        Double[][] product = new Double[r1][c2];
        for(int i = 0; i < r1; i++) {
            for (int j = 0; j < c2; j++) {
                for (int k = 0; k < c1; k++) {
                    product[i][j] = firstMatrix[i][k] * secondMatrix[k][j];
                }
            }
        }

        return product;
    }

    public static Double[][] addMatrices(Double[][] firstMatrix, Double[][] secondMatrix){
        Double[][] sum = new Double[firstMatrix.length][firstMatrix[0].length];
        for(int i = 0; i < firstMatrix.length; i++){
            for(int j = 0; j < firstMatrix[0].length; j++){
                sum[i][j] = firstMatrix[i][j] + secondMatrix[i][j];
            }
        }
        return sum;
    }

    private static double sigmoid(double input){
        return 1/(1+Math.exp(-1 * input));
    }

    public static Double[][] applyActivation(Double[][] matrix, int index){
        Double[][] afterActivation = new Double[matrix.length][matrix[0].length];
        for(int i = 0; i < matrix.length; i++){
            for(int j = 0; j < matrix[0].length; j++){
                if(activationStructure[index].equals("SIGMOID")){
                    afterActivation[i][j] = sigmoid(matrix[i][j]);
                }
                else if(activationStructure[index].equals("RELU")){
                    afterActivation[i][j] = Math.max(0, matrix[i][j]);
                }
                else if(activationStructure[index].equals("TANH")){
                    afterActivation[i][j] = Math.tanh(matrix[i][j]);
                }
                else{
                    afterActivation[i][j] = matrix[i][j];
                }
            }
        }
        return afterActivation;
    }

    public Byte nextMove(Snake snake) {
        boolean[][] array = snake.getArray();
        double dir = (int)snake.getDir();
        /*double northDistance = findDistance(snake, (int) dir, array)/snake.getBoardHeight();
        double eastDistance = findDistance(snake, ((int) dir+1)%4, array)/snake.getBoardHeight();
        double southDistance = findDistance(snake, ((int) dir+2)%4, array)/snake.getBoardWidth();
        double westDistance = findDistance(snake, ((int) dir+3)%4, array)/snake.getBoardWidth();*/

        double northDistance = -0.5 + (double) findDistance(snake, 0, array)/(double) snake.getBoardHeight();
        double eastDistance = -0.5 + (double) findDistance(snake, 1, array)/(double) snake.getBoardWidth();
        double southDistance = -0.5 + (double) findDistance(snake, 2, array)/(double) snake.getBoardHeight();
        double westDistance = -0.5 + (double) findDistance(snake, 3, array)/(double) snake.getBoardWidth();
        double horizontalDist = -0.5 + (double) (snake.getPx() - snake.getHeadX())/(double) snake.getBoardWidth();
        double verticalDist = -0.5 + (double) (snake.getPy() - snake.getHeadY())/(double) snake.getBoardHeight();
        //System.out.println(dir + ", " + northDistance + ", " + eastDistance + ", " + southDistance + ", " + westDistance + ", "+ horizontalDist + ", " + verticalDist);
        Double[] numericalInput = {northDistance, eastDistance, southDistance, westDistance, horizontalDist, verticalDist};
        Double[][] input = new Double[1][4+numericalInput.length];
        for(int i = 0; i < 4 + numericalInput.length; i++){
            if(i < 4){
                if(i == dir){
                    input[0][i] = 1.0;
                }
                else{
                    input[0][i] = -1.0;
                }
            }
            else{
                input[0][i] = numericalInput[i-4];
            }
        }

        //Double[][] input = {numericalInput};

        //Double[][] input = {{horizontalDist, verticalDist}};
        //System.out.println(Arrays.toString(input[0]));
        for(int i = 0; i < structure.length-1; i++){
            Double[][] weightLayer = this.weightMatList.get(i);
            input = multiplyMatrices(input, weightLayer);

            Double[][] biasLayer = {this.biasMatList.get(i)};
            input = addMatrices(input, biasLayer);

            input = applyActivation(input, i+1);
        }
        //System.out.println(Arrays.toString(input[0]));
        //System.out.println("---------------------------");
        double max = -Integer.MAX_VALUE;
        int maxIndex = -1;
        for(int i = 0; i < input[0].length; i++){
            if(input[0][i] > max){
                max = input[0][i];
                maxIndex = i;
            }
        }
        return (byte)(maxIndex);
        //return (byte)((snake.getDir() + maxIndex)%4);
        //byte res = (byte)(Math.random()*4);
        //return res;

    }

    private int findDistance(Snake snake, int dir, boolean[][] array){
        if(dir == 0){
            int counter = 0;
            int curX = snake.getHeadX();
            int curY = snake.getHeadY()-1;
            while(curY >= 0 && array[curX][curY] == false){
                curY -= 1;
                counter++;
            }
            return counter;
        }
        else if(dir == 1){
            int counter = 0;
            int curX = snake.getHeadX()+1;
            int curY = snake.getHeadY();
            while(curX < snake.getBoardWidth() && array[curX][curY] == false){
                curX += 1;
                counter++;
            }
            return counter;
        }
        else if(dir == 2) {
            int counter = 0;
            int curX = snake.getHeadX();
            int curY = snake.getHeadY()+1;
            while(curY < snake.getBoardHeight() && array[curX][curY] == false){
                curY += 1;
                counter++;
            }
            return counter;
        }
        else{
            int counter = 0;
            int curX = snake.getHeadX()-1;
            int curY = snake.getHeadY();
            while(curX >= 0 && array[curX][curY] == false){
                curX -= 1;
                counter++;
            }
            return counter;
        }
    }

    public ArrayList<Double[][]> getWeightMatList(){
        return this.weightMatList;
    }

    public ArrayList<Double[]> getBiasMatList(){
        return this.biasMatList;
    }
}




