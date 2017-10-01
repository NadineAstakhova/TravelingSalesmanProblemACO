package TravelingSalesmanProblemACO;

/**
 * Created by Nadine on 01.10.2017.
 */
public class Main {
    public static void main(String[] args) {
        ACOTravelingSalesman antColony = new ACOTravelingSalesman(6);
        antColony.startAntOptimization(15);
        //   antColony.hh();
    }
}
