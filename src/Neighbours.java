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
        final double threshold = 0.33;

        State[][] satisfactionMatrix = getSatisfactionMatrix(world, threshold); //Create matrix with satisfied/unsatisfied.

        int nNotSatisfied = nUnsatisfied(satisfactionMatrix); //Counts number of unsatisfied cells.
        int nNa = nNA(satisfactionMatrix); // Counts number of NA cells
        int[][] unsArray = unsatisfiedArrays(satisfactionMatrix, nNotSatisfied); //Puts coordinates for unsatisfied cells in an array.
        int[][] NAArray = NAArrays(satisfactionMatrix, nNa); //Puts coordinates for unsatisfied cells in an array.

        world = shuffleUnsatisfied(world, unsArray, NAArray); //Shuffles the elements in the NA Array to give the unsatisfied cells new coordinates.
    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override

    //-----------------

    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.49, 0.49, 0.02};

        // Number of locations (places) in world (square)
        int nLocations = 950;

        Actor[] distArray = distribution(nLocations, dist); //Generates array with correct distribution.
        distArray = shuffle(distArray); //Shuffles array
        world = toMatrix(distArray, nLocations, world); //Generates the start world.
        // Should be last
        fixScreenSize(nLocations);
    }

    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up

    boolean checkSatisfaction(Actor[][] world, int i, int j, final double threshold) { //Checks is an element is satisfied/unsatisfied/NA.
        boolean satisfaction = false;
        int length = world.length;
        double nRed = 0;
        double nBlue = 0;

        for (int x = i - 1; x < i + 2; x++) {
            for (int y = j - 1; y < j + 2; y++) {
                if (x >= 0 && x <= length - 1 && y >= 0 && y <= length - 1) {
                    if (world[x][y] == Actor.RED) {
                        nRed++;
                    } else if (world[x][y] == Actor.BLUE) {
                        nBlue++;
                    }
                }
            }
        }

        double satisfactionLevel = 0;

        if (nRed + nBlue != 1) {
            if (world[i][j] == Actor.RED) {
                satisfactionLevel = ((nRed - 1) / (nBlue + nRed - 1));
            } else if (world[i][j] == Actor.BLUE) {
                satisfactionLevel = ((nBlue - 1) / (nBlue + nRed - 1));
            }
        }

        if (satisfactionLevel >= threshold) {
            satisfaction = true;
        }

        return satisfaction;
    }

    boolean activeCell(Actor[][] world, int i, int j) { //Checks is a cell is active or not (NA or RED/BLUE)
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
            int k = rand.nextInt(i);
            Actor tmp = array[k];
            array[k] = array[i - 1];
            array[i - 1] = tmp;
        }
        return array;
    }

    Actor[][] toMatrix(Actor[] distArray, int nLocations, Actor[][] world) { //Places the elements from the array into a matrix.
        int length = (int) Math.sqrt(nLocations);
        world = new Actor[length][length];

        int n = 0;
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                world[i][j] = distArray[n];
                n++;
            }
        }
        return world;
    }

    State[][] getSatisfactionMatrix(Actor[][] world, final double threshold) { //Puts the elements satisfaction level in a matrix.
        State[][] satisfactionMatrix = new State[world.length][world.length];

        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                boolean active = activeCell(world, i, j);
                if (active == true) {
                    boolean satisfaction = checkSatisfaction(world, i, j, threshold);
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
        return satisfactionMatrix;
    }

    int[][] NAArrays(State[][] satisfactionMatrix, int nNA) { //Fills an array with the coordinates of the white spots
        int[][] NAArray = new int[nNA][2];
        int n = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.NA) {
                    NAArray[n][0] = i;
                    NAArray[n][1] = j;
                    n++;
                }
            }
        }
        return NAArray;
    }

    int nNA(State[][] satisfactionMatrix) { //Counts # of NA spots.
        int y = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.NA) {
                    y++;
                }
            }
        }
        return y;
    }

    int[][] unsatisfiedArrays(State[][] satisfactionMatrix, int nNotSatisfied) { //Fills an array with the coordinates of the unsatisfied spots
        int[][] unsArrays = new int[nNotSatisfied][2];
        int n = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.UNSATISFIED) {
                    unsArrays[n][0] = i;
                    unsArrays[n][1] = j;
                    n++;
                }
            }
        }
        return unsArrays;
    }

    int nUnsatisfied(State[][] satisfactionMatrix) { //Counts # of unsatisfied.
        int x = 0;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world.length; j++) {
                State stateCell = satisfactionMatrix[i][j];
                if (stateCell == State.UNSATISFIED) {
                    x++;
                }
            }
        }
        return x;
    }

    Actor[][] shuffleUnsatisfied(Actor[][] world, int[][] USArrays, int[][] NAArrays) { //Shuffles the elements in the NA Array and gives the unsatisfied cells new coordinates.
        for (int i = NAArrays.length; i > 0; i--) {
            int k = rand.nextInt(i);
            int[] tmp = NAArrays[k];
            NAArrays[k] = NAArrays[i - 1];
            NAArrays[i - 1] = tmp;
        }
        if (USArrays.length < NAArrays.length) {
            for (int m = 0; m < USArrays.length; m++) {
                Actor transfer = world[NAArrays[m][0]][NAArrays[m][1]];
                world[NAArrays[m][0]][NAArrays[m][1]] = world[USArrays[m][0]][USArrays[m][1]];
                world[USArrays[m][0]][USArrays[m][1]] = transfer;
            }
        } else {
            for (int m = 0; m < NAArrays.length; m++) {
                Actor transfer = world[NAArrays[m][0]][NAArrays[m][1]];
                world[NAArrays[m][0]][NAArrays[m][1]] = world[USArrays[m][0]][USArrays[m][1]];
                world[USArrays[m][0]][USArrays[m][1]] = transfer;
            }
        }
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
