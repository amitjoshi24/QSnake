import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;
import java.awt.image.*;
import java.util.*;

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
    private double mutationRate = 0.2;
    private double mutationSize = 0.8;
    private double geneRange = 2;
    private final int TRIALS = 1;
    private NNBrain[] population;
    private NNBrain bestBrain; //lives until it is no longer the best brain
    private NNBrain curBrain;
    private double bestScore = 0;
    private boolean running = false;
    private int[] structure = {10, 5, 4};
    //private static int[] structure = {2, 4};
    private int populationSize;
    private HashMap<Integer, Double> lowerGeneValue; //is used to set the lower gene value for initialization, mutation can cause lower values
    private HashMap<Integer, Double> upperGeneValue; //is used to set the upper gene value for initial population, mutation can cause higher values
    private Random geneticRandom;
    private long rseed;
    private javax.swing.Timer moveTimer;

    private Snake snake;
    private Thread thread;
    private boolean[][] array;
    private BufferedImage myImage = new BufferedImage(1218, 850, BufferedImage.TYPE_INT_RGB);
    private Graphics2D myBuffer = (Graphics2D)myImage.getGraphics();
    private Timer timer;
    private int delay = 40;
    public void newGame(){
        array = new boolean[120][80];
        setFocusable(true);
        myBuffer.setBackground(Color.RED);
        myBuffer.clearRect(0, 0, myImage.getWidth(), myImage.getHeight());
        //addKeyListener(new Key());
        setPreferredSize(new Dimension(1218, 855));
        snake.reset(array);
        //snake = new Snake(myBuffer, array);

        //timer.start();

        //thread = new Thread(snake);
        //thread.start();
    }
    public void restart(){
        this.newGame();
    }
    public SnakePanel(){
        this.bestBrain = null;
        this.curBrain = null;
        this.bestScore = 0;
        this.rseed = 724;
        this.geneticRandom = new Random(this.rseed);
        this.running = false;
        this.populationSize = 100; //1000 brains competing for the top spot in population
        population = new NNBrain[this.populationSize];
        lowerGeneValue = new HashMap<Integer, Double>();
        upperGeneValue = new HashMap<Integer, Double>();
        this.populate();

        addKeyListener(new Key());
        setPreferredSize(new Dimension(1218, 855));
        array = new boolean[120][80];
        snake = new Snake(myBuffer, array, delay);
        thread = new Thread(snake);


        setFocusable(true);
        myBuffer.setBackground(Color.RED);
        myBuffer.clearRect(0, 0, myImage.getWidth(), myImage.getHeight());

        moveTimer = new Timer(delay/2, new MoveTimerListener());
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
                if(snake.getAlive() && curBrain != null) {
                    byte nextMove = curBrain.nextMove(snake);
                    press(nextMove);
                }
                drawBoard();
                myBuffer.clearRect(100, 0, 620, 10);
                myBuffer.setColor(Color.BLACK);
                myBuffer.drawString("Length: " + snake.length(), 100, 10);
                repaint();
            }
        }
    }
    private class MoveTimerListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            if(snake.getAlive() && curBrain != null) {
                byte nextMove = curBrain.nextMove(snake);
                press(nextMove);
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

    private void press(Byte b){
        snake.setDir(b);
    }
    private void pressUp(){
        snake.setDir((byte)0);
    }
    private void pressRight(){
        snake.setDir((byte)1);
    }
    private void pressDown(){
        snake.setDir((byte)2);
    }
    private void pressLeft(){
        snake.setDir((byte)3);
    }
    private void pressEnter(){
        synchronized (myBuffer) {
            restart();
        }
    }

    private class Key extends KeyAdapter{
        public void keyPressed(KeyEvent e){
            if(e.getKeyCode() == KeyEvent.VK_UP){
                pressUp();
            }
            else if(e.getKeyCode() == KeyEvent.VK_RIGHT){
                pressRight();
            }
            else if(e.getKeyCode() == KeyEvent.VK_DOWN){
                pressDown();
            }
            else if(e.getKeyCode() == KeyEvent.VK_LEFT){
                pressLeft();
            }
            else if(e.getKeyCode() == KeyEvent.VK_ENTER){
                pressEnter();
            }
        }
    }
    private void populate(){
        for(int populationCounter = 0; populationCounter < population.length; populationCounter++){
            ArrayList<Double[][]> newWeightMatList = new ArrayList<>();
            ArrayList<Double[]> newBiasMatList = new ArrayList<>();
            for(int i = 0; i < this.structure.length-1; i++){
                Double[][] weightLayer = new Double[this.structure[i]][this.structure[i+1]];
                Double[] biasLayer = new Double[this.structure[i+1]];
                for(int r = 0; r < weightLayer.length; r++) {
                    for(int c = 0; c < weightLayer[0].length; c++) {
                        double newWeightComponent = 1 * ((geneRange * this.geneticRandom.nextDouble()) - (geneRange/2));
                        weightLayer[r][c] = newWeightComponent;
                    }
                }
                for(int c = 0; c < biasLayer.length; c++){
                    double newBiasComponent = 1 * ((geneRange * this.geneticRandom.nextDouble()) - (geneRange/2));
                    biasLayer[c] = newBiasComponent;
                }
                newWeightMatList.add(weightLayer);
                newBiasMatList.add(biasLayer);
            }
            this.population[populationCounter] = new NNBrain(newWeightMatList, newBiasMatList);
        }
    }

    private void populateFromParent(NNBrain bestThisGeneration){
        //my implementation of genetic algorithm will have the overall best brain always be a part of the new population
        population[0] = this.bestBrain;
        for(int i = 1; i < this.populationSize; i++){
            population[i] = this.mutate(bestThisGeneration);
        }
    }

    private void populateFromPreviousPopulation(NavigableMap<Double, NNBrain> navigableMap, double totalScore){
        population[0] = this.bestBrain;
        for(int i = 1; i < population.length; i++){
            double value1 = geneticRandom.nextDouble() * totalScore;
            NNBrain brain1 =  navigableMap.higherEntry(value1).getValue();

            NNBrain brain2;
            do {
                double value2 = geneticRandom.nextDouble() * totalScore;
                brain2 = navigableMap.higherEntry(value2).getValue();
            }while (brain1 != brain2);

            NNBrain newBrain = this.mutate(this.crossover(brain1, brain2));
            this.population[i] = newBrain;
        }
    }

    public void runGenetic(){
        int generation = 1;
        while(true){
            NavigableMap<Double, NNBrain> navigableMap = new TreeMap<Double, NNBrain>();
            double totalScore = 0;
            NNBrain bestThisGeneration = null;
            double bestScoreThisGeneration = 0;
            for(int i = 0; i < population.length; i++){
                curBrain = population[i];
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
                totalScore += score;
                navigableMap.put(totalScore, curBrain);
                System.out.println("Generation: " + generation + "#" + i + " " + score);
                if(score > bestScoreThisGeneration){
                    bestScoreThisGeneration = score;
                    bestThisGeneration = curBrain;
                }
            }
            if(bestScoreThisGeneration > this.bestScore || this.bestBrain == null){
                this.bestScore = bestScoreThisGeneration;
                this.bestBrain = bestThisGeneration;
            }
            //this.populateFromParent(bestThisGeneration);
            this.populateFromPreviousPopulation(navigableMap, totalScore);
            generation += 1;
        }
    }
    public NNBrain mutate(NNBrain original){
        ArrayList<Double[][]> weightMatList = original.getWeightMatList();
        ArrayList<Double[]> biasMatList = original.getBiasMatList();

        ArrayList<Double[][]> newWeightMatList = new ArrayList<Double[][]>();
        ArrayList<Double[]> newBiasMatList = new ArrayList<Double[]>();
        for(int i = 0; i < this.structure.length-1; i++){
            Double[][] weightLayer = new Double[this.structure[i]][this.structure[i+1]];
            Double[] biasLayer = new Double[this.structure[i+1]];
            for(int r = 0; r < weightLayer.length; r++) {
                for(int c = 0; c < weightLayer[0].length; c++) {
                    if(this.geneticRandom.nextDouble() < this.mutationRate) {
                        double oldWeightComponent = (1 - this.mutationSize) * weightMatList.get(i)[r][c];
                        double newWeightComponent = this.mutationSize * ((geneRange * this.geneticRandom.nextDouble()) - (geneRange/2));
                        weightLayer[r][c] = oldWeightComponent + newWeightComponent;
                    }
                    else{
                        weightLayer[r][c] = weightMatList.get(i)[r][c];
                    }
                }
            }
            for(int c = 0; c < biasLayer.length; c++){
                if(this.geneticRandom.nextDouble() < this.mutationRate) {
                    double oldBiasComponent = (1 - this.mutationSize) * biasMatList.get(i)[c];
                    double newBiasComponent = this.mutationSize * ((geneRange * this.geneticRandom.nextDouble()) - (geneRange/2));
                    biasLayer[c] = oldBiasComponent + newBiasComponent;
                }
                else{
                    biasLayer[c] = biasMatList.get(i)[c];
                }
            }
            newWeightMatList.add(weightLayer);
            newBiasMatList.add(biasLayer);
        }
        return new NNBrain(newWeightMatList, newBiasMatList);
    }

    private NNBrain crossover(NNBrain brain1, NNBrain brain2){
        ArrayList<Double[][]> weightMatList1 = brain1.getWeightMatList();
        ArrayList<Double[]> biasMatList1 = brain1.getBiasMatList();

        ArrayList<Double[][]> weightMatList2 = brain2.getWeightMatList();
        ArrayList<Double[]> biasMatList2 = brain2.getBiasMatList();

        ArrayList<Double[][]> newWeightMatList = new ArrayList<Double[][]>();
        ArrayList<Double[]> newBiasMatList = new ArrayList<Double[]>();

        for(int i = 0; i < weightMatList1.size(); i++){
            Double[][] weightLayer1 = weightMatList1.get(i);
            Double[] biasLayer1 = biasMatList1.get(i);

            Double[][] weightLayer2 = weightMatList2.get(i);
            Double[] biasLayer2 = biasMatList2.get(i);

            Double[][] newWeightLayer = new Double[weightLayer1.length][weightLayer1[0].length];
            Double[] newBiasLayer = new Double[biasLayer1.length];
            for(int r = 0; r < weightLayer1.length; r++){
                for(int c = 0; c < weightLayer1[0].length; c++){
                    if(geneticRandom.nextDouble() < 0.5){
                        newWeightLayer[r][c] = weightLayer1[r][c];
                    }
                    else{
                        newWeightLayer[r][c] = weightLayer2[r][c];
                    }
                }
            }
            for(int c = 0; c < biasLayer1.length; c++){
                if(geneticRandom.nextDouble() < 0.5){
                    newBiasLayer[c] = biasLayer1[c];
                }
                else{
                    newBiasLayer[c] = biasLayer2[c];
                }
            }
            newWeightMatList.add(newWeightLayer);
            newBiasMatList.add(newBiasLayer);
        }
        return new NNBrain(newWeightMatList, newBiasMatList);
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
    private boolean alive;
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
            return;
        }
        headX = x;
        headY = y;
        if(array[x][y] && !queue.peek().equals(new Point(x, y))){
            snakeTimer.stop();
            this.alive = false;
            return;
        }
        array[x][y] = true;
    }
    private void remove(Point p){
        int x = p.x;
        int y = p.y;
        array[x][y] = false;
    }
    public void setDir(byte d){
        if(this.moveQueue.size() == 0 || this.moveQueue.getLast() != ((d+2)%4)) {
            if(this.getDir() != ((d+2)%4)) {
                if(this.moveQueue.size() > 0 && this.moveQueue.getLast() == d){
                    return;
                }
                this.moveQueue.add(d);
            }
        }
    }
    private class SnakeListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            if(moveQueue.size() > 0){
                dir = moveQueue.poll();
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