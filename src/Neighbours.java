import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Random;

import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.lang.System.*;

/*
 *  Program to simulate segregation.
 *  See : http://nifty.stanford.edu/2014/mccown-schelling-model-segregation/
 *
 * NOTE:
 * - JavaFX first calls method init() and then method start() far below.
 * - To test uncomment call to test() first in init() method!
 *
 */
// Extends Application because of JavaFX (just accept for now)
public class Neighbours extends Application {
    final static Random rand = new Random();

    // Enumeration type for the Actors
    enum Actor {
        BLUE, RED, NONE   // NONE used for empty locations
    }

    // Enumeration type for the state of an Actor
    enum State {
        UNSATISFIED,
        SATISFIED,
        NA     // Not applicable (NA), used for NONEs
    }

    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.7;

        //int i = 1;
        //int j = 1;
        //boolean active = activeCell(world, i, j);
        //out.print("Active " + active);

        //boolean satisfaction = checkSatisfaction(world, i, j, threshold);
        //out.println(" Satisfaction: " + satisfaction);

        State[][] satisfactionMatrix = getSatisfactionMatrix(world, threshold);
        //out.print(satisfactionMatrix.length);
        //out.print(satisfactionMatrix[1][1]);
        out.println(Arrays.toString(satisfactionMatrix[15]));


        int nNotSatisfied = nUnsatisfied(satisfactionMatrix);
        int nNa = nNA(satisfactionMatrix);
        //out.println(nNotSatisfied);
        //out.println(nNa);

        int[][] unsArray = unsatisfiedArrays(satisfactionMatrix, nNotSatisfied);
        int[][] NAArray = NAArrays(satisfactionMatrix, nNa);
        //out.println(Arrays.toString(unsArray[2]));
        //out.println(Arrays.toString(NAArray[2]));

        world = shuffleUnsatisfied(world, unsArray, NAArray);

    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE

        // Number of locations (places) in world (square)
        int nLocations = 900;
        double[] dist = {0.25, 0.25, 0.50};

        Actor[] distArray = distribution(nLocations, dist); //Generates array with correct distribution.
        //out.println(Arrays.toString(distArray));
        // ---------------------------------------------
        distArray = shuffle(distArray); //Shuffles array
        //out.println(Arrays.toString(distArray));
        world = toMatrix(distArray, nLocations, world); //Generates the start world.


        // Should be last
        fixScreenSize(nLocations);
    }


    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up
    Actor[][] shuffleUnsatisfied(Actor[][] world, int[][] USArray, int[][] NAArray) {
        for (int i = NAArray.length; i > 0; i--) {
            int k = rand.nextInt(i);
            int[] tmp = NAArray[k];
            NAArray[k] = NAArray[i - 1];
            NAArray[i - 1] = tmp;
        }

        for (int j = 0; j < USArray.length; j++){
            Actor transfer = world[NAArray[j][0]][NAArray[j][1]];
            world[NAArray[j][0]][NAArray[j][1]] = world[USArray[j][0]][USArray[j][1]];
            world[USArray[j][0]][USArray[j][1]] = transfer;
        }

        return world;
    }

    int[][] unsatisfiedArrays(State[][] satisfactionMatrix, int nNotSatisfied){
        int[][] unsArray = new int[nNotSatisfied][2];
        int n = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.UNSATISFIED){
                    unsArray[n][0] = i;
                    unsArray[n][1] = j;
                    n++;
                }
            }
        }
        return unsArray;
    }

    int[][] NAArrays(State[][] satisfactionMatrix, int nNA){
        int[][] NAArray = new int[nNA][2];
        int n = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.NA){
                    NAArray[n][0] = i;
                    NAArray[n][1] = j;
                    n++;
                }
            }
        }
        return NAArray;
    }

    int nUnsatisfied(State[][] satisfactionMatrix){
        int x = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.UNSATISFIED){
                    x++;
                }
            }
        }
        return x;
    }

    int nNA(State[][] satisfactionMatrix){
        int y = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.NA){
                    y++;
                }
            }
        }
        return y;
    }




    State[][] getSatisfactionMatrix(Actor[][] world, final double threshold) {

        State[][] satisfactionMatrix = new State[world.length][world.length];
        //int i = 0;
        //int j = world.length -1;

        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                boolean active = activeCell(world, i, j);
                //out.print("Active " + active);
                //TODO if statement for putting in enum satisfaction into world matrix.
                if (active == true) {
                    boolean satisfaction = checkSatisfaction(world, i, j, threshold);
                    //out.println(" Satisfaction: " + satisfaction);

                    if (satisfaction == true) {
                        satisfactionMatrix[i][j] = State.SATISFIED;
                    } else {
                        satisfactionMatrix[i][j] = State.UNSATISFIED;
                    }
                } else {
                    satisfactionMatrix[i][j] = State.NA;
                }
            }
        }
        //out.println(satisfactionMatrix[0][0]);
        return satisfactionMatrix;
    }

    boolean checkSatisfaction(Actor[][] world, int i, int j, final double threshold) {
        boolean satisfaction = false;
        //int length = (int)Math.sqrt(nLocations);
        int length = world.length;
        int nRed = 0;
        int nBlue = 0;
        int nWhite = 0;


        int sum = 0;
        for (int x = i - 1; x < i + 2; x++) {
            for (int y = j - 1; y < j + 2; y++) {
                if (x < 0 || x > length - 1 || y < 0 || y > length - 1) {
                    //out.println("outof bounds" + x + y);
                    nWhite++;
                } else {
                    //out.println("used:" + x + y);
                    if (world[x][y] == Actor.RED) {
                        nRed++;
                    } else if (world[x][y] == Actor.BLUE) {
                        nBlue++;
                    } else {
                        nWhite++;
                    }
                }
            }
        }

        int satisfactionLevel = 0;

        if (nRed + nBlue != 1) {


            if (world[i][j] == Actor.RED) {
                satisfactionLevel = ((nRed - 1) / (nBlue + nRed - 1)) * 100;
            } else if (world[i][j] == Actor.BLUE) {
                satisfactionLevel = ((nBlue - 1) / (nBlue + nRed - 1)) * 100;
            }
        }

        if (satisfactionLevel >= threshold * 100) {
            satisfaction = true;
        }

        return satisfaction;
    }

    boolean activeCell(Actor[][] world, int i, int j) {
        boolean active = false;
        if (world[i][j] != Actor.NONE) {
            active = true;
        }
        return active;
    }


    Actor[] distribution(int nLocations, double[] dist) { // Create array with correct distribution of actors.
        Actor[] distArray = new Actor[nLocations];

        for (int i = 0; i < distArray.length; i++) {
            if (i >= 0 && i < dist[0] * nLocations) {
                distArray[i] = Actor.RED;
            } else if (i >= dist[0] * nLocations && i < (dist[0] + dist[1]) * nLocations) {
                distArray[i] = Actor.BLUE;
            } else if (i >= (dist[0] + dist[1]) * nLocations) {
                distArray[i] = Actor.NONE;
            }
        }
        return distArray;
    }

    Actor[] shuffle(Actor[] array) { //Shuffles the array created in distribution.
        for (int i = array.length; i > 0; i--) {
            int j = rand.nextInt(i);
            Actor tmp = array[j];
            array[j] = array[i - 1];
            array[i - 1] = tmp;
        }

        return array;
    }

    Actor[][] toMatrix(Actor[] distArray, int nLocations, Actor[][] world) { //TODO Method for array to matrix.
        int length = (int) Math.sqrt(nLocations);
        //out.println(length);
        world = new Actor[length][length]; //TODO Math.sqrt. How turn into int?

        int n = 0;

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                world[i][j] = distArray[n];
                n++;
            }
        }
        //for(int i = 0; i < length; i++) {
        //    out.println(Arrays.toString(world[i]));
        //}
        return world;
    }

    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work
    void test() {
        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {Actor.RED, Actor.RED, Actor.NONE},
                {Actor.NONE, Actor.BLUE, Actor.NONE},
                {Actor.RED, Actor.NONE, Actor.BLUE}
        };
        double th = 0.5;   // Simple threshold used for testing
        int size = testWorld.length;

        //Test distribution method distribution
        int nLocations = 900;
        double[] dist = {0.25, 0.25, 0.50};


        Actor[] distArray = distribution(nLocations, dist);
        //out.println(Arrays.toString(distArray));
        // ---------------------------------------------
        distArray = shuffle(distArray);
        //out.println(Arrays.toString(distArray));
        // TODO test methods
        world = toMatrix(distArray, nLocations, world);


        exit(0);
    }

    // Helper method for testing (NOTE: reference equality)
    <T> int count(T[] arr, T toFind) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == toFind) {
                count++;
            }
        }
        return count;
    }

    // *****   NOTHING to do below this row, it's JavaFX stuff  ******

    double width = 400;   // Size for window
    double height = 400;
    long previousTime = nanoTime();
    final long interval = 450000000;
    double dotSize;
    final double margin = 50;

    void fixScreenSize(int nLocations) {
        // Adjust screen window depending on nLocations
        dotSize = (width - 2 * margin) / sqrt(nLocations);
        if (dotSize < 1) {
            dotSize = 2;
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Build a scene graph
        Group root = new Group();
        Canvas canvas = new Canvas(width, height);
        root.getChildren().addAll(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a timer
        AnimationTimer timer = new AnimationTimer() {
            // This method called by FX, parameter is the current time
            public void handle(long currentNanoTime) {
                long elapsedNanos = currentNanoTime - previousTime;
                if (elapsedNanos > interval) {
                    updateWorld();
                    renderWorld(gc, world);
                    previousTime = currentNanoTime;
                }
            }
        };

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulation");
        primaryStage.show();

        timer.start();  // Start simulation
    }


    // Render the state of the world to the screen
    public void renderWorld(GraphicsContext g, Actor[][] world) {
        g.clearRect(0, 0, width, height);
        int size = world.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                double x = dotSize * col + margin;
                double y = dotSize * row + margin;

                if (world[row][col] == Actor.RED) {
                    g.setFill(Color.RED);
                } else if (world[row][col] == Actor.BLUE) {
                    g.setFill(Color.BLUE);
                } else {
                    g.setFill(Color.WHITE);
                }
                g.fillOval(x, y, dotSize, dotSize);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
