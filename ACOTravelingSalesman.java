package TravelingSalesmanProblemACO;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


/**
 * Created by Nadine on 28.09.2017.
 */
public class ACOTravelingSalesman {
    private double originalNum = 1.0;  //original number of trails
    private double alpha = 1; //controls the pheromone importance
    private double beta = 5; //controls the distance priority
    private double evaporation = 0.5; //the percent how much the pheromone is evaporating
    private double totalAmount = 500; //information about the total amount of pheromone left
    private double antFactor = 0.8; //how many ants use per city
    private double randFactor = 0.01;

    private double graphCities[][]; //cities graphCities
    private int numOfCities;
    private int numOfAnts;
    private double trails[][];
    private double probabilities[];

    private int currentIndex;

    private int maxIterations = 1000;

    private List<Ant> ants = new ArrayList<>();

    private Random rand = new Random();

    private int[] bestTourOrder;
    private double bestTourLength;
    double[][] randomMatrix;

    public ACOTravelingSalesman(int numOfCities){
        graphCities = generateDistanceMatrix(numOfCities);
        this.numOfCities = graphCities.length;
        numOfAnts = (int) (this.numOfCities * antFactor);

        trails = new double[this.numOfCities][this.numOfCities];
        probabilities = new double[this.numOfCities];
        IntStream.range(0, numOfAnts)
                .forEach(i -> ants.add(new Ant(this.numOfCities)));

    }

    /**
     * Generate initial solution
     */
    public double[][] generateDistanceMatrix(int n) {
        randomMatrix = new double[n][n];
        //get data about cities from serialized HashMap
        HashMap<Point.Double, String> cities = this.deserializedFromFile("hashmap.ser");
        AtomicInteger ai = new AtomicInteger(0);  //An int value that may be updated atomically
        AtomicInteger aj = new AtomicInteger(0); //as atomically incremented counters and no Integer because foreach
        cities.forEach((key, value) ->{
                    cities.forEach((key1, value1) -> {
                        //get Distance for current city and others
                        randomMatrix[ai.get()][aj.get()] = this.getDistance(key.getX(),
                                key.getY(),
                                key1.getX(),
                                key1.getY());
                        aj.incrementAndGet(); //Atomically increments by one the current value
                    });
                    aj.set(0); //for new list of others cities
                    ai.incrementAndGet();
                }
        );

        System.out.println("Cities Distance");
        for (int i = 0; i < randomMatrix.length; i++) {
            for (int j = 0; j < randomMatrix.length; j++) {
                //for short output
                System.out.print(new DecimalFormat("#0.00").format(randomMatrix[i][j]) + " ");
            }
            System.out.println();
        }

        return randomMatrix;
    }

    /**
     * Perform ant optimization
     */
    public void startAntOptimization(int count) {
        IntStream.rangeClosed(1, count)
                .forEach(i -> {
                    System.out.println("Attempt #" + i);
                    solve();
                });
    }

    /**
     * Run the logic
     */
    public int[] solve() {
        setupAntsAtCities();
        clearTrails();
        IntStream.range(0, maxIterations)
                .forEach(i -> {
                    moveAntsToCities(); //ants go
                    updateTrails(); //new trails
                    updateBestSolve();
                });
        System.out.println("Best tour length: " + new DecimalFormat("#0.00").format(bestTourLength - numOfCities));
        System.out.println("Best tour order: " + Arrays.toString(bestTourOrder));
        return bestTourOrder.clone();
    }

    /**
     * Prepare ants for the simulation. Setup the ants matrix to start with a rand city
     */
    private void setupAntsAtCities() {
        IntStream.range(0, numOfAnts)
                .forEach(i -> {
                    ants.forEach(ant -> {
                        ant.clear();
                        ant.visitCity(-1, rand.nextInt(numOfCities));
                    });
                });
        currentIndex = 0;
    }

    /**
     * At each iteration, move ants
     */
    private void moveAntsToCities() {
        IntStream.range(currentIndex, numOfCities - 1)
                .forEach(i -> {
                    ants.forEach(ant -> ant.visitCity(currentIndex, selectNextCity(ant)));
                    currentIndex++;
                });
    }

    /**
     * The most important part
     * Select next city for each ant
     * Select the next town based on the probability logic.
     */
    private int selectNextCity(Ant ant) {
        //check if Ant should visit a rand city
        int t = rand.nextInt(numOfCities - currentIndex);
        if (rand.nextDouble() < randFactor) {
            //may or may not contain value
            OptionalInt cityIndex = IntStream.range(0, numOfCities)
                    .filter(i -> i == t && !ant.visited(i)) //i equals t and an ant didn't visit this city
                    .findFirst(); //return first element from stream
            //If a city is present
            if (cityIndex.isPresent()) {
                return cityIndex.getAsInt();
            }
        }
        //If no select any rand city, need to calculate probabilities to select the next city
        calcProbabilities(ant);
        //After calculating probabilities, decide to which city to go
        double r = rand.nextDouble();
        double total = 0;
        for (int i = 0; i < numOfCities; i++) {
            total += probabilities[i];
            if (total >= r) {
                return i;
            }
        }

        throw new RuntimeException("There are no other cities");
    }

    /**
     * Calculate the next city picks probabilites
     * Ants prefer to follow stronger and shorter trails. Do this by storing the probability of moving to each city in the array
     */
    public void calcProbabilities(Ant ant) {
        int i = ant.trail[currentIndex];
        double pheromone = 0.0;
        for (int l = 0; l < numOfCities; l++) {
            if (!ant.visited(l)) {
                pheromone += Math.pow(trails[i][l], alpha) * Math.pow(1.0 / graphCities[i][l], beta);
            }
        }
        for (int j = 0; j < numOfCities; j++) {
            if (ant.visited(j)) {
                probabilities[j] = 0.0;
            } else {
                double m = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graphCities[i][j], beta);
                probabilities[j] = m / pheromone;
            }
        }
    }

    /**
     * Update trails that ants and the left pheromone used
     */
    private void updateTrails() {
        for (int i = 0; i < numOfCities; i++) {
            for (int j = 0; j < numOfCities; j++) {
                trails[i][j] *= evaporation;
            }
        }
        for (Ant a : ants) {
            double contribution = totalAmount / a.trailLength(graphCities);
            for (int i = 0; i < numOfCities - 1; i++) {
                trails[a.trail[i]][a.trail[i + 1]] += contribution;
            }
            trails[a.trail[numOfCities - 1]][a.trail[0]] += contribution;
        }
    }

    /**
     * Update the best solution
     */
    private void updateBestSolve() {
        if (bestTourOrder == null) {
            bestTourOrder = ants.get(0).trail;
            bestTourLength = ants.get(0)
                    .trailLength(graphCities);
        }

        for (Ant a : ants) {
            if (a.trailLength(graphCities) < bestTourLength) {
                bestTourLength = a.trailLength(graphCities);
                bestTourOrder = a.trail.clone();
            }
        }
    }

    /**
     * Clear trails after simulation
     */
    private void clearTrails() {
        IntStream.range(0, numOfCities)
                .forEach(i -> {
                    IntStream.range(0, numOfCities)
                            .forEach(j -> trails[i][j] = originalNum);
                });
    }

    final static int  EARTH_RADIUS = 6372795;

    public double getDistance(double phi1, double g1, double phi2, double g2){
        double lat1 = phi1 * Math.PI / 180;
        double lat2 = phi2 * Math.PI / 180;
        double long1 = g1 * Math.PI / 180;
        double long2 = g2 * Math.PI / 180;
        double delta = long1 - long2;

        double cl1 = Math.cos(lat1);
        double cl2 = Math.cos(lat2);
        double sl1 = Math.sin(lat1);
        double sl2 = Math.sin(lat2);
        double cdelta = Math.cos(delta);
        double sdelta = Math.sin(delta);


        // вычисления длины большого круга
        double y = Math.sqrt(Math.pow(cl2 * sdelta, 2) + Math.pow(cl1 * sl2 - sl1 * cl2 * cdelta, 2));
        double x = sl1 * sl2 + cl1 * cl2 * cdelta;
        double t = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1-lat2)/2),2) + cl1*cl2*Math.pow(Math.sin(delta/2),2)));

        //
        double ad = Math.atan2(y, x);
        double dist = t * EARTH_RADIUS;


        return dist * 0.001;

    }



    /* public void hh (){
        HashMap<Point.Double, String> hmap = new HashMap<Point.Double, String>();
        //Adding elements to HashMap
        hmap.put(new Point.Double(48.0,37.47), "Donetsk");
        hmap.put(new Point.Double(49.13,28.28), "Vinnytsia");
        hmap.put(new Point.Double(46.28,30.43), "Odessa");
        hmap.put(new Point.Double(48.17,37.10), "Krasnoarmeysk");
        hmap.put(new Point.Double(50.27,30.30), "Kiev");
        hmap.put(new Point.Double(52.13,21.1), "Warsaw");

        try
        {
            FileOutputStream fos =
                    new FileOutputStream("hashmap.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hmap);
            oos.close();
            fos.close();
            System.out.printf("Serialized HashMap data is saved in hashmap.ser");
        }catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }*/

    /**
     * Deserialized HashMap with coordinates of cities from file
     */
    public HashMap<Point.Double, String> deserializedFromFile (String file){
        //Coordinates are in Point Class
        HashMap<Point.Double, String> map = null;
        try
        {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap) ois.readObject();
            ois.close();
            fis.close();
        }catch(IOException ioe)
        {
            ioe.printStackTrace();

        }catch(ClassNotFoundException c)
        {
            System.out.println("Class not found");
            c.printStackTrace();

        }

        return map;
    }



}
