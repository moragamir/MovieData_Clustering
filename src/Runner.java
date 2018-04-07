import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Guy on 02-Apr-17.
 */

public class Runner {
    public static void main(String[] args) {
        Runner run = new Runner();
        run.start(args[0], Integer.parseInt(args[1]), args[2]);
    }

    public void start(String datasetfolder, int algNum, String moviesubsetfile){
        String rating, movie;
        int startIndex=0, endIndex=0, userNum, movieNum;
        int views[][] = new int[6041][4000]; //marks every movie-user pair with 1 if the user has seen the movie
        double userViews[] = new double[6041];// for each user, the amount of movies he saw
        double movieP[] = new double [4000]; //P of every movie according to the given formula
        double moviePairP[][] = new double[4000][4000]; //P of every pair of movies according to the given formula
        int input[] = new int[100]; //an array of 100 movie numbers for the clutering function
        List<Integer> moviesInput = new ArrayList<Integer>();// a group of movies as an input to the clustering function (same as above but as a list)
        List<Integer> moviesInput2 = new ArrayList<Integer>();// the same group of movies, but for the improved algorithm
        List<List<Integer>> clusters = new ArrayList<List<Integer>>();// the output group of clusters
        List<List<Integer>> improved_clusters = new ArrayList<List<Integer>>();// the output group of the improved correlation clusters
        HashMap<Integer,ArrayList<String>> movieGenres = new HashMap<Integer, ArrayList<String>>();
        
        System.out.println("Movie numbers are:");
      //Parsing the movie numbers input file
        try {
            File file = new File(moviesubsetfile);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                movie = scanner.nextLine();
                movieNum = Integer.parseInt(movie.substring(0, movie.length()));  
                System.out.print(movieNum+", ");
                moviesInput.add(movieNum);
                moviesInput2.add(movieNum);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        //Parsing the ratings input file
        
        try {
            File file = new File("data/ratings.dat");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                rating = scanner.nextLine();
                if (rating.length() < 15) continue;
                endIndex = startIndex = 0;
                while(rating.charAt(endIndex) != ':')
                    endIndex++;

                userNum = Integer.parseInt(rating.substring(startIndex, endIndex));
                endIndex += 2;
                startIndex = endIndex;
                while(rating.charAt(endIndex) != ':')
                    endIndex++;
                movieNum = Integer.parseInt(rating.substring(startIndex, endIndex));
                	views[userNum][movieNum] = 1;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        System.out.print("\n");
        for (int i=0; i<4000; i++){
            for (int j=0; j<6041; j++) {
                if (views[j][i]==1)
                    userViews[j]++;
            }
        }
        //Parsing the movies input file for movie names and the improved algorithm
        try {
            FileReader file = new FileReader("data/movies.txt");
            BufferedReader br = new BufferedReader(file);
            //Scanner scanner = new Scanner(file);
            int movieNumber, i=0;
                while ((movie = br.readLine()) != null) {
                	movie = movie.concat("|");
                    i=0;
                    while (movie.charAt(i)!=':')
                        i++;
                    movieNumber = Integer.parseInt(movie.substring(0,i));
                    
                    if (moviesInput.contains(movieNumber))
                    	addLine(movieGenres, movie, movieNumber);
                }
                try {

    				if (br != null)
    					br.close();

    				if (file != null)
    					file.close();

    			} catch (IOException ex) {

    				ex.printStackTrace();

    			}
                //scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Iterator<Integer> iter = moviesInput.iterator();
        while(iter.hasNext()){
        	int current = iter.next();
        	if (!movieGenres.containsKey((current))){
        		iter.remove();
        	}
        }
        Iterator<Integer> iter2 = moviesInput2.iterator();
        while(iter2.hasNext()){
        	int current = iter2.next();
        	if (!movieGenres.containsKey(current)){
        		iter2.remove();
        	}
        }
        for(int i=0; i<moviesInput.size(); i++){
            int current = moviesInput.get(i);
            input[i] = current;
        }
        //Calculating the P of every movie
        for (int i=0; i<100; i++){
            double x=0;
            for (int j = 1; j < 6041; j++) {
                if (views[j][input[i]] == 1)
                    x += (2.0 / userViews[j]);
            }
            movieP[input[i]] = ((double)((double)1 / (double)6041)) * (((double)((double)2 / (double)4000)) + x);
        }
        //Calculating the P of every movie pair
        for(int i=0; i<100; i++){
            for (int i2=i+1; i2<100; i2++) {
                double y = 0;
                for (int j = 1; j < 6041; j++) {
                    if (views[j][input[i]] == 1 && views[j][input[i2]] == 1)
                        y += (2.0 / (userViews[j] * (userViews[j] - 1)));
                }
                moviePairP[input[i]][input[i2]] = ((double)((double)1 / (double)6041)) * ( ((double)((double)2 / ((double)4000*(double)3999))) + y);
            }
        }
        if (algNum == 1){
	        correlationClustering(moviesInput, clusters, moviePairP, movieP);
	        System.out.println("the clusters of the basic algorithm are:");
	        print(clusters, movieGenres);
	        System.out.println("Total Cost: " + totalCost(clusters, movieP, moviePairP)+"\n");
        }
        else if (algNum == 2){
	        improvedCorrelationClustering(moviesInput2, improved_clusters ,movieGenres, moviePairP, movieP);
	        System.out.println("the clusters of the improved algorithm are:");
	        print(improved_clusters, movieGenres);
	        System.out.println("Total Cost: " + totalCost(improved_clusters, movieP, moviePairP)+"\n");
        }
    }
    //Main function of the basic algorithm
    public void correlationClustering(List<Integer> input, List<List<Integer>> clusters, double[][] moviePairP, double[] movieP){
        if (input.isEmpty()) return;

        List<Integer> newCluster = new ArrayList<Integer>();
        int pivot = input.get(0);
        input.remove(0);
        newCluster.add(pivot);
        Iterator<Integer> iter = input.iterator();
        while(iter.hasNext()){
        	int current = iter.next();
        	if (moviePairP[pivot][current] >= movieP[pivot]*movieP[current]){
        		newCluster.add(current);
        		iter.remove();
        	}
        }
        clusters.add(newCluster);
        correlationClustering(input, clusters, moviePairP, movieP);
    }

    //Main function of the improved algorithm
    public void improvedCorrelationClustering(List<Integer> input, List<List<Integer>> clusters,HashMap<Integer,ArrayList<String>> movieGenres, double[][] moviePairP, double[] movieP){
        if (input.isEmpty()) return;
        List<Integer> newCluster = new ArrayList<Integer>();
        int pivot = input.get(0);
        input.remove(0);
        newCluster.add(pivot);
        Iterator<Integer> iter = input.iterator();
        while(iter.hasNext()){
        	int current = iter.next();
        	if (hasSameGenre(pivot, current, movieGenres) && moviePairP[pivot][current] >= movieP[pivot]*movieP[current]){
        		newCluster.add(current);
        		iter.remove();
        	}
        }
        clusters.add(newCluster);
        improvedCorrelationClustering(input, clusters, movieGenres, moviePairP, movieP);
    }

    //The addLine function takes a given line from the movies file, breaks its genres into a list and adds it to the movies map
    public void addLine(HashMap<Integer,ArrayList<String>> movieGenres, String movie, int key){
        int startIndex=0;
        ArrayList<String> genres = new ArrayList<String>();
        
        while (movie.charAt(startIndex) != ':' || movie.charAt(startIndex+1) != ':')
            startIndex++;
        startIndex += 2;
        int endIndex = startIndex;
      //now we expect start and end indexes to be on the first character of the movie name, which will reside in the beginning of the list
        while (movie.charAt(endIndex) != ':' || movie.charAt(endIndex+1) != ':'){
        	if (movie.charAt(endIndex) == '(')
        		genres.add(movie.substring(startIndex, endIndex));
        	endIndex++;
        }
        endIndex += 2;
        startIndex = endIndex;
        //now we expect start and end indexes to be on the first character of the first genre
        while (endIndex < movie.length()-1) {
            while (movie.charAt(endIndex) != '|')
                endIndex++;
            genres.add(movie.substring(startIndex, endIndex));
            //System.out.print(movie.substring(startIndex, endIndex)+", ");
            startIndex = endIndex = endIndex+1;
        }
        //System.out.print("\n");
        movieGenres.put(key, genres);
        
    }

    //return true if two given movies have at least one common genre, and false otherwise
    public boolean hasSameGenre(int movie1, int movie2, HashMap<Integer, ArrayList<String>> movieGenres){
        String genre1, genre2;
        if (movieGenres.containsKey(movie1)) {
            for (int i = 0; i < movieGenres.get(movie1).size(); i++) {
                genre1 = movieGenres.get(movie1).get(i);
                if (movieGenres.containsKey(movie2)) {
                    for (int j = 0; j < movieGenres.get(movie2).size(); j++) {
                        genre2 = movieGenres.get(movie2).get(j);
                        if (genre1.equals(genre2))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    //Prints the output clusters to the screen
    public void print(List<List<Integer>> clustersToPrint, HashMap<Integer, ArrayList<String>> movieGenres){
        int clusterNumber = 1;
        for (int i=0; i<clustersToPrint.size(); i++){
            System.out.print("cluster number "+clusterNumber+":\n");
            clusterNumber++;
            for (int j=0; j<clustersToPrint.get(i).size(); j++){
            	int movie = clustersToPrint.get(i).get(j);
                System.out.print(movie +" "+ movieGenres.get(movie).get(0) +", ");
            }
            System.out.print("\n");
        }
    }
    //Calculates the cost of the total output clusters
    public double totalCost(List<List<Integer>> resultClusters, double[] movieP, double[][] moviePairP){
        double sum = 0, currentCost;
        for (int i=0; i<resultClusters.size(); i++){
            currentCost = cost(resultClusters.get(i), movieP, moviePairP);
            sum = sum + currentCost;
        }
        return sum;
    }
    //Calculates the cost of a single cluster
    public double cost (List<Integer> cluster, double[] movieP, double[][] moviePairP){
        if (cluster.isEmpty()) return 0;
        double cost = 0;
        double x = (double)1 / movieP[cluster.get(0)];
        if (cluster.size() == 1)
            cost = (double)Math.log((double)1 / movieP[cluster.get(0)]);
        else {
            for (int i=0; i<cluster.size(); i++){
                for (int j=i+1; j<cluster.size(); j++){
                    if (moviePairP[cluster.get(i)][cluster.get(j)] > 0)
                        cost = cost + (((double)1 / (double)(cluster.size()-1)) * (double)Math.log((double)1 / moviePairP[cluster.get(i)][cluster.get(j)]));
                }
            }
        }
        return cost;
    }
}

