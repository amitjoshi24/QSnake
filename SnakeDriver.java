import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;
import java.awt.image.*;
import java.util.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;


public class SnakeDriver{


    public SnakeDriver(){

    }

    public static void main(String[] args){
        JFrame frame = new JFrame();
        frame.setContentPane(new SnakePanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("The Snake Game");
        frame.setVisible(true);
    }
}
class SnakePanel extends JPanel{

    private javax.swing.Timer moveTimer;
    private Snake snake;
    private Thread thread;
    private boolean[][] array;
    private BufferedImage myImage = new BufferedImage(1218, 850, BufferedImage.TYPE_INT_RGB);
    private Graphics2D myBuffer = (Graphics2D)myImage.getGraphics();
    private Timer timer;
    private int delay = 10;
    public void newGame(){
        array = new boolean[120][80];
        setFocusable(true);
        myBuffer.setBackground(Color.RED);
        myBuffer.clearRect(0, 0, myImage.getWidth(), myImage.getHeight());

        setPreferredSize(new Dimension(1218, 855));
        snake.reset(array);

    }
    public void restart(){
        this.newGame();
    }
    public SnakePanel(){


        setPreferredSize(new Dimension(1218, 855));
        array = new boolean[120][80];
        snake = new Snake(myBuffer, array, delay);
        thread = new Thread(snake);


        setFocusable(true);
        myBuffer.setBackground(Color.RED);
        myBuffer.clearRect(0, 0, myImage.getWidth(), myImage.getHeight());

        timer = new Timer(delay, new TimerListener());
        //moveTimer.start();
        timer.start();
        thread.start();
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                runGenetic();
            }
        });
        t1.start();
    }
    private class TimerListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            synchronized (myBuffer) {
                drawBoard();
                myBuffer.clearRect(100, 0, 620, 10);
                myBuffer.setColor(Color.BLACK);
                myBuffer.drawString("Length: " + snake.length(), 100, 10);
                repaint();
            }
        }
    }
    private void drawBoard(){
        int x = 9;
        int y = 9;
        for(int i = 0; i < array.length; i++, x+=10, y = 9){
            for(int j = 0; j < array[0].length; j++, y += 10){
                if(array[i][j]){
                    myBuffer.setColor(new Color(20,240, 1));
                    myBuffer.fillRect(x, y, 9, 9);
                }
                else{
                    myBuffer.setColor(Color.BLUE);
                    myBuffer.fillRect(x, y, 10, 10);
                }

            }
        }
        myBuffer.setColor(Color.RED);
        myBuffer.fillRect(9 + (snake.getPx() * 10), 9 + (snake.getPy() * 10), 10, 10);
    }

    public void paintComponent(Graphics g){
        g.drawImage(myImage, 0, 0, getWidth(), getHeight(), null);
    }

    private void pressEnter(){
        synchronized (myBuffer) {
            restart();
        }
    }


    public void runGenetic(){
        int generation = 1;
        while(true){
            double length = 0;
            pressEnter();
            long startTime = System.currentTimeMillis();
            long timeSinceGrowth = startTime;
            int oldLength = snake.getLength();
            while (snake.getAlive()){
                System.out.print("");
                if(snake.length() != oldLength){
                    oldLength = snake.length();
                    timeSinceGrowth = System.currentTimeMillis();
                    continue;
                }
                if(System.currentTimeMillis() - timeSinceGrowth > 20000){
                    break;
                }
            }
            length += snake.getLength();

            double score = length + ((float)(System.currentTimeMillis() - startTime)/(float) (1000*6));
            System.out.println("Generation " + generation + ": " + score);
            System.out.println("Weights: " + Arrays.toString(snake.getWeights()));
            
            generation += 1;
        }
    }

}
class Snake implements Runnable{
    private boolean[][] array;
    private int length = 6;
    private int pX , pY;
    private int headX, headY;
    private Queue<Point> queue = new LinkedList<Point>();
    private byte dir = 0; //0 = up, 1 = right, 2 = down, 3 = left
    private LinkedList<Byte> moveQueue;
    private boolean movedSinceLast = true;
    private Timer snakeTimer;
    private int boardWidth;
    private int boardHeight;
    private double rewardToGive;
    private boolean alive;
    Brain curBrain;
    public Double[] getWeights(){
        return curBrain.getWeights();
    }
    public Snake(Graphics2D g, boolean[][] a, int delay){
        this.moveQueue = new LinkedList<Byte>();
        this.array = a;
        this.snakeTimer = new Timer(delay, new SnakeListener());
        this.boardWidth = this.array.length;
        this.boardHeight = this.array[0].length;
        pX = (int)(Math.random() * array.length);
        pY = (int)(Math.random() * array[0].length);
        this.alive = false;
        add(60, 60);
        add(60, 59);
        add(60, 58);
        add(60, 57);
        add(60, 56);
        add(60, 55);
        makePrize();
        rewardToGive = -1;
        curBrain = new Brain();
    }

    public void reset(boolean[][] a){
        this.dir = 0;
        this.moveQueue = new LinkedList<Byte>();
        this.queue.clear();
        this.array = a;
        pX = (int)(Math.random() * array.length);
        pY = (int)(Math.random() * array[0].length);
        add(60, 60);
        add(60, 59);
        add(60, 58);
        add(60, 57);
        add(60, 56);
        add(60, 55);
        makePrize();
        this.length = 6;

        this.run();
    }
    public int length(){
        return length;
    }
    public int getPx(){
        return pX;
    }
    public int getPy(){
        return pY;
    }
    private void makePrize(){
        pX = (int)(Math.random() * array.length);
        pY = (int)(Math.random() * array[0].length);

    }
    public byte getDir(){
        return dir;
    }
    private void add(int x, int y){
        this.add(new Point(x, y));
    }
    private void add(Point p){

        queue.add(p);
        int x = p.x;
        int y = p.y;
        if(x < 0 || y < 0 || x >= this.boardWidth || y >= this.boardHeight){
            snakeTimer.stop();
            this.alive = false;
            rewardToGive = -1000;
            return;
        }
        headX = x;
        headY = y;
        if(array[x][y] && !queue.peek().equals(new Point(x, y))){
            snakeTimer.stop();
            this.alive = false;
            rewardToGive = -1000;
            return;
        }
        array[x][y] = true;
        rewardToGive = -1;
    }
    private void remove(Point p){
        int x = p.x;
        int y = p.y;
        array[x][y] = false;
    }
    public void updateWeights(double rewardToGive, double oldQsa, Double[] oldFeatureVector){
        curBrain.updateWeights(this, rewardToGive, oldQsa, oldFeatureVector);
    }
    private class SnakeListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            double oldQsa = 0;
            Double[] oldFeatureVector = null;
            if(Snake.this.getAlive() && curBrain != null) {
                    Tuple tup = curBrain.nextMove(Snake.this);
                    dir = tup.action;
                    oldQsa = tup.value;
                    oldFeatureVector = tup.featureVector;
            }
            else{
                return;
            }
            if(oldFeatureVector == null){
                System.out.println("THIS IS A PROBLEM!");
            }
            if(dir == 0){
                headY--;
                add(new Point(headX, headY));
            }
            else if(dir == 1){
                headX++;
                add(new Point(headX, headY));
            }
            else if(dir == 2){
                headY++;
                add(new Point(headX, headY));
            }
            else{
                headX--;
                add(new Point(headX, headY));
            }
            if(headX == pX && headY == pY){
                length += 6;
                rewardToGive = 50;
                makePrize();
            }
            if(queue.size() > (length)){
                remove(queue.poll());
                try {
                    array[headX][headY] = true;
                }
                catch (ArrayIndexOutOfBoundsException fldsj){

                }
            }
            Snake.this.updateWeights(rewardToGive, oldQsa, oldFeatureVector);
            rewardToGive = -1;
        }
    }

    public void run(){
        snakeTimer.start();
        this.alive = true;
    }
    public boolean[][] getArray(){
        return this.array;
    }
    public int getHeadX(){
        return this.headX;
    }
    public int getHeadY(){
        return this.headY;
    }
    public int getBoardWidth(){
        return this.boardWidth;
    }
    public int getBoardHeight(){
        return this.boardHeight;
    }
    public boolean getAlive(){
        return this.alive;
    }
    public int getLength(){
        return this.length;
    }
}




class Tuple{
    public byte action;
    public double value;
    public Double[] featureVector;
    public Tuple(byte a, double v, Double[] vec){ 
        this.action = a;
        this.value = v;
        this.featureVector = vec;
    }
}

class Brain{
    private Random random;
    private long rseed;
    private Double[] weights;
    private int weightsSize = 3;
    private double gamma;
    private double alpha;
    public Double[] getWeights(){
        return weights;
    }
    public Brain(){
        this.rseed = 724917;
        this.random = new Random(this.rseed);
        weights = new Double[weightsSize];
        initializeWeights();
        gamma = 0.99;
        alpha = 0.1;
    }
    private void initializeWeights(){
        for(int i = 0; i < weightsSize; i++){
            weights[i] = (random.nextDouble()*2)-1;
        }
    }
    private Double[] calculateFeatureVector(Snake snake, byte action){
        boolean[][] array = snake.getArray();
        double dir = (int)snake.getDir();

        double northDistance = (double)(findDistance(snake, 0, array) - (action == 0 ? 1 : 0))/snake.getBoardHeight() ;
        double eastDistance = (double)(findDistance(snake, 1, array) - (action == 1 ? 1 : 0))/snake.getBoardWidth() ;
        double southDistance =  (double)(findDistance(snake, 2, array) - (action == 2 ? 1 : 0))/snake.getBoardHeight() ;
        double westDistance =(double)(findDistance(snake, 3, array) - (action == 3 ? 1 : 0))/snake.getBoardWidth();
        

        /*double northDistance = (findDistance(snake, 0, array) - (action == 0 ? 1 : 0)) <= 0 ? 1 : 0;
        double eastDistance = (findDistance(snake, 1, array) - (action == 1 ? 1 : 0)) <= 0 ? 1 : 0;
        double southDistance = (findDistance(snake, 2, array) - (action == 2 ? 1 : 0)) <= 0 ? 1 : 0;
        double westDistance = (findDistance(snake, 3, array) - (action == 3 ? 1 : 0)) <= 0 ? 1 : 0;
        */

        double dirDistance = (findDistance(snake, action, array) - 1) < 0 ? 1 : 0;
        int headX = snake.getHeadX();
        int headY = snake.getHeadY();

        if(action == 0){
            headY--;
        }
        else if(action == 1){
            headX++;
        }
        else if(action == 2){
            headY++;
        }
        else if(action == 3){
            headX--;
        }

        double horizontalDist = (double)Math.abs(snake.getPx() - headX)/snake.getBoardWidth();
        double verticalDist = (double)Math.abs(snake.getPy() - headY)/snake.getBoardHeight();
        //Double[] featureVector = {dirDistance, northDistance, eastDistance, southDistance, westDistance, horizontalDist, verticalDist};
        Double[] featureVector = {dirDistance, horizontalDist, verticalDist};
        return featureVector;
    }

    public Tuple nextMove(Snake snake){
        // return action for which Q(s, a) reaches a maximum
        double maxVal = -999999999;
        byte maxAction = 0;
        Double[] maxFeatureVector = null;
        for(int action = 0; action < 4; action++){

            Double[] featureVector = calculateFeatureVector(snake, (byte)action);
            double qsa = 0;
            for(int i = 0; i < weightsSize; i++){
                qsa += weights[i]*featureVector[i];
            }
            double curVal = qsa;
            if(curVal > maxVal){
                maxAction = (byte)action;
                maxVal = curVal;
                maxFeatureVector = featureVector;
            }
        }
        if(maxFeatureVector == null){
            System.out.println("WHAT THE FUCK");
            System.exit(0);
        }
        return new Tuple(maxAction, maxVal, maxFeatureVector);
    }

    public void updateWeights(Snake snake, double rewardToGive, double oldQsa, Double[] oldFeatureVector){
        Tuple subsequentMove = nextMove(snake);

        double qsaprime = subsequentMove.value;
        double difference = (rewardToGive + (gamma*qsaprime)) - oldQsa;
        for(int i = 0; i < this.weights.length; i++){
            if(oldFeatureVector == null){
                System.out.println("subsequentMove was null");
            }
            
            this.weights[i] += this.alpha * difference * oldFeatureVector[i];
        }
        //normalizeWeights();
    }

    private void normalizeWeights(){
        double sum = 0;
        for(int i = 0; i < this.weights.length; i++){
            sum += this.weights[i];
        }
        for(int i = 0; i < this.weights.length; i++){
            this.weights[i] /= sum;
        }
    }


    private int findDistance(Snake snake, int dir, boolean[][] array){
        // change array to be of longs rather than bools
        try{
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
        catch(Exception e){
            return 0;
        }
    }
}


