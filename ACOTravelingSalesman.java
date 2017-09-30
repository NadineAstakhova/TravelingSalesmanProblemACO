package TravelingSalesmanProblemACO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Created by Nadine on 28.09.2017.
 */
public class ACOTravelingSalesman {
    private double c = 1.0;  //original number of trails
    private double alpha = 1; //controls the pheromone importanc
    private double beta = 5; //controls the distance priority
    private double evaporation = 0.5; //the percent how much the pheromone is evaporating
    private double Q = 500; //information about the total amount of pheromone left
    private double antFactor = 0.8; //how many ants use per city
    private double randomFactor = 0.01;

    private double graph[][]; //cities graph
    private int numberOfCities;
    private int numberOfAnts;
    private double trails[][];
    private double probabilities[];

    private int currentIndex;

    private int maxIterations = 1000;

    private List<Ant> ants = new ArrayList<>();

    private Random random = new Random();

    private int[] bestTourOrder;
    private double bestTourLength;
    double[][] randomMatrix;

    public ACOTravelingSalesman(int noOfCities){
        graph = generateRandomMatrix(noOfCities);
        numberOfCities = graph.length;
        numberOfAnts = (int) (numberOfCities * antFactor);

        trails = new double[numberOfCities][numberOfCities];
        probabilities = new double[numberOfCities];
        IntStream.range(0, numberOfAnts)
                .forEach(i -> ants.add(new Ant(numberOfCities)));

    }

    public static void main(String[] args) {
        ACOTravelingSalesman antColony = new ACOTravelingSalesman(6);

        antColony.startAntOptimization();
        System.out.println(antColony.getDistance(49.13, 28.28, 55.45, 37.37));
        //antColony.fileRead("C:\\Users\\Nadine\\IdeaProjects\\test\\src\\main\\resources\\cities.txt");
     //   antColony.hh();


    }


    /**
     * Generate initial solution
     */
    public double[][] generateRandomMatrix(int n) {
        randomMatrix = new double[n][n];
        /*IntStream.range(0, n)
                .forEach(i -> IntStream.range(0, n)
                        .forEach(j -> randomMatrix[i][j] = Math.abs(random.nextInt(100) + 1)));*/
        HashMap<Point.Double, String> cities = this.deserializedFromFile("hashmap.ser");
        System.out.println("Deserialized HashMap..");
        // Display content using Iterator
        Set set = cities.entrySet();
        Iterator iterator = set.iterator();
        Iterator iterator2 = set.iterator();
        DecimalFormat df2 = new DecimalFormat(".##");


        for (int i = 0; i < randomMatrix.length; i++) {

                if(iterator.hasNext()) {

                    Map.Entry entry = (Map.Entry) iterator.next();

                    for (int j = 0; j < randomMatrix.length;) {
                        if(i == j) {
                            randomMatrix[i][j] = 0;
                            j++;
                        }

                    while (iterator.hasNext()) {
                        Map.Entry mentry = (Map.Entry) iterator.next();
                        randomMatrix[i][j] = this.getDistance(((Point.Double) entry.getKey()).getX(),
                                ((Point.Double) entry.getKey()).getY(),
                                ((Point.Double) mentry.getKey()).getX(),
                                ((Point.Double) mentry.getKey()).getY()
                        );
                        System.out.println(df2.format(randomMatrix[i][j]));
                        j++;
                    }

                }
            i++;



        }


        }
       // while (iterator.hasNext()) {
       /* for (int i = 0; i < randomMatrix.length; i++) {
            for (int j = 0; j < randomMatrix.length; j++) {


                    Map.Entry mentry = (Map.Entry) iterator.next();
                    //randomMatrix[k][l] = this.getDistance(((Point.Double) mentry.getKey()).getX(), ((Point.Double) mentry.getKey()).getY(), ((Point.Double) mentry.getKey()).getX(), 37.37);
                    System.out.print("key: " + ((Point.Double) mentry.getKey()).getX() + " & Value: ");
                    if (iterator.hasNext())

                   randomMatrix[i][j] = this.getDistance(((Point.Double) mentry.getKey()).getX(),
                           ((Point.Double) mentry.getKey()).getY(),
                           ((Point.Double)((Map.Entry) iterator.next()).getKey()).getX(),
                           ((Point.Double)((Map.Entry) iterator.next()).getKey()).getY());


                    System.out.println(mentry.getValue());

                }
            }}*/

       /*for (int i = 0; i < randomMatrix.length; i++) {
            for (int j = 0; j < randomMatrix.length; j++) {
                if (i==j)
                    randomMatrix[i][j] = 0;
                else
                    randomMatrix[i][j] = this.getDistance(49.13, 28.28, 55.45, 37.37);

            }
        }*/
        System.out.println("");
        for (int i = 0; i < randomMatrix.length; i++) {
            for (int j = 0; j < randomMatrix.length; j++) {
                System.out.print(randomMatrix[i][j] + " ");
            }
            System.out.println();
        }

        return randomMatrix;
    }

    /**
     * Perform ant optimization
     */
    public void startAntOptimization() {
        IntStream.rangeClosed(1, 3)
                .forEach(i -> {
                    System.out.println("Attempt #" + i);
                    solve();
                });
    }
    /**
     * Run the main logic
     */
    public int[] solve() {
        setupAnts();
        clearTrails();
        IntStream.range(0, maxIterations)
                .forEach(i -> {
                    moveAnts();
                    updateTrails();
                    updateBest();
                });
        System.out.println("Best tour length: " + (bestTourLength - numberOfCities));
        System.out.println("Best tour order: " + Arrays.toString(bestTourOrder));
        return bestTourOrder.clone();
    }

    /**
     * Prepare ants for the simulation. Setup the ants matrix to start with a random city
     */
    private void setupAnts() {
        IntStream.range(0, numberOfAnts)
                .forEach(i -> {
                    ants.forEach(ant -> {
                        ant.clear();
                        ant.visitCity(-1, random.nextInt(numberOfCities));
                    });
                });
        currentIndex = 0;
    }

    /**
     * At each iteration, move ants
     */
    private void moveAnts() {
        IntStream.range(currentIndex, numberOfCities - 1)
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
        //check if Ant should visit a random city
        int t = random.nextInt(numberOfCities - currentIndex);
        if (random.nextDouble() < randomFactor) {
            OptionalInt cityIndex = IntStream.range(0, numberOfCities)
                    .filter(i -> i == t && !ant.visited(i))
                    .findFirst();
            if (cityIndex.isPresent()) {
                return cityIndex.getAsInt();
            }
        }
        //If no select any random city, need to calculate probabilities to select the next city
        calculateProbabilities(ant);
        //After calculating probabilities, decide to which city to go to by using
        double r = random.nextDouble();
        double total = 0;
        for (int i = 0; i < numberOfCities; i++) {
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
    public void calculateProbabilities(Ant ant) {
        int i = ant.trail[currentIndex];
        double pheromone = 0.0;
        for (int l = 0; l < numberOfCities; l++) {
            if (!ant.visited(l)) {
                pheromone += Math.pow(trails[i][l], alpha) * Math.pow(1.0 / graph[i][l], beta);
            }
        }
        for (int j = 0; j < numberOfCities; j++) {
            if (ant.visited(j)) {
                probabilities[j] = 0.0;
            } else {
                double numerator = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
                probabilities[j] = numerator / pheromone;
            }
        }
    }

    /**
     * Update trails that ants and the left pheromone used
     */
    private void updateTrails() {
        for (int i = 0; i < numberOfCities; i++) {
            for (int j = 0; j < numberOfCities; j++) {
                trails[i][j] *= evaporation;
            }
        }
        for (Ant a : ants) {
            double contribution = Q / a.trailLength(graph);
            for (int i = 0; i < numberOfCities - 1; i++) {
                trails[a.trail[i]][a.trail[i + 1]] += contribution;
            }
            trails[a.trail[numberOfCities - 1]][a.trail[0]] += contribution;
        }
    }

    /**
     * Update the best solution
     */
    private void updateBest() {
        if (bestTourOrder == null) {
            bestTourOrder = ants.get(0).trail;
            bestTourLength = ants.get(0)
                    .trailLength(graph);
        }
        for (Ant a : ants) {
            if (a.trailLength(graph) < bestTourLength) {
                bestTourLength = a.trailLength(graph);
                bestTourOrder = a.trail.clone();
            }
        }
    }

    /**
     * Clear trails after simulation
     */
    private void clearTrails() {
        IntStream.range(0, numberOfCities)
                .forEach(i -> {
                    IntStream.range(0, numberOfCities)
                            .forEach(j -> trails[i][j] = c);
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

    public void fileRead(String file){
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = "{\"name\":\"JavaInterviewPoint\", \"department\":\"x y\"}";

            Map<String, Object> jsonMap = new HashMap<String, Object>();

            // convert JSON string to Map
            jsonMap = mapper.readValue(jsonString,
                    new TypeReference<Map<String, Point>>(){});
            for(Map.Entry m:jsonMap.entrySet()){
                System.out.println(m.getKey()+" "+m.getValue());
            }


            System.out.println(jsonMap);
        }
        catch(IOException ie)
        {
            ie.printStackTrace();
        }


    }

    /*public void hh (){
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

    public HashMap<Point.Double, String> deserializedFromFile (String file){
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
