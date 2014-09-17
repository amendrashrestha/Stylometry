package com.test.stylometry;

/**
 *
 * @author ITE
 */
import com.test.IOHandler.IOProperties;
import com.test.IOHandler.IOReadWrite;
import com.test.model.Alias;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is some code for doing stylometric matching of aliases based on posts
 * (such as discussion board messages). Features: letters (26), digits (10),
 * punctuation (11), function words (293), word length (20), sentence length
 * (6). Except for freq. of sentence lengths, this is a subset of the features
 * used in Narayanan et al. (On the Feasibility of Internet-Scale Author
 * Identification)
 *
 * Some problems to consider: The more features, the more "sparse" the feature
 * vectors will be (many zeros) in case of few posts --> similar feature vectors
 * due to a majority of zeros
 *
 * Since all features are not of the same "dimension", it makes sense to
 * normalize/standardize the features to have mean 0 and variance 1, as in
 * Narayanan et al. The above standardization works when finding the best
 * matching candidate, but may be problematic since the "similarity" between two
 * aliases will depend on the features of other aliases (since the
 * standardization works column/(feature)-wise).
 *
 * If we do not use normalization/standardization, we cannot use feature which
 * are not frequencies, since the features with large magnitudes otherwise will
 * dominate completely!!! Even if we do only use frequencies, the results
 * without normalization seems poor (good with normalization) Try to improve the
 * unnormalized version before using it on real problems...
 *
 * Observe that the obtained similarity values cannot be used directly as a
 * measure of the "match percentage"!
 *
 *
 * @author frejoh
 *
 */
public class StylometricAnalysisMain {

    private List<String> functionWords;			// Contains the function words we are using
    private List<Alias> aliases;				// The aliases we are interested in to compare        
    private List<List<Float>> featVectorForAllAliases;

    public StylometricAnalysisMain() {
        loadFunctionWords(IOProperties.FUNCTION_WORDS);
        loadDataFile(IOProperties.INDIVIDUAL_USER_FILE_PATH);
        aliases = new ArrayList<>();
    }

    private void loadDataFile(String path) {
        String filepath = System.getProperty("user.home") + path;
        //System.out.println(filepath);
    }

    public List<Float> executeAnalysis(String ID) throws IOException, SQLException {
        IOReadWrite ioRW = new IOReadWrite();
        Alias user = new Alias();
        String tempBasePath = IOProperties.INDIVIDUAL_USER_FILE_PATH;
        String basePath = getClass().getResource("../../../../").getFile() + tempBasePath;
        String ext = IOProperties.USER_FILE_EXTENSION;

        user = ioRW.convertTxtFileToAliasObj(basePath, ID, ext);
        List<Float> freatuteVector = createFeatureVectors(user);
        return freatuteVector;
    }

    public List<Float> executePostAnalysis(List posts) {
        Alias user = new Alias();
        user.setPosts(posts);
        List<Float> freatuteVector = createFeatureVectors(user);
        return freatuteVector;
    }

    public double returnStylo(List post1, List post2) {
        double stylo = 0.0;
        try {
            IOReadWrite ioRW = new IOReadWrite();
            List<Alias> aliasList = ioRW.convertUserToObj(post1, post2);
            stylo = executeStylo(aliasList);
        } catch (SQLException ex) {
            Logger.getLogger(StylometricAnalysisMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return stylo;
    }

    public double executeStylo(List<Alias> aliasList) throws SQLException {
        this.aliases = aliasList;
        createFeatureVectors();
        //System.out.println("User1: " + aliases.get(0).getFeatureVector());
        // System.out.println("User2: " + aliases.get(1).getFeatureVector());

        double stylo = compareFeatureVectors(aliases.get(0).getFeatureVector(), aliases.get(1).getFeatureVector());
        return stylo;
    }

    public List<Float> executeTxtStylo(String post) {
        Alias user = new Alias();
        user.setSinglePost(post);
        List<Float> freatuteVector = createFeatureVectors(user);
        return freatuteVector;
    }

    /**
     * Extract words from text string, remove punctuation etc.
     *
     * @param text
     * @return
     */
    public static List<String> extractWords(String text) {
        List<String> wordList = new ArrayList<>();
        String[] words = text.split("\\s+");
        wordList.addAll(Arrays.asList(words)); // words[i] = words[i].replaceAll("[^\\w]", "");
        return wordList;
    }

    /**
     * Load the list of function words from file
     *
     * @param path
     */
    private void loadFunctionWords(String path) {
        functionWords = new ArrayList<>();
        BufferedReader br;
        try {
            path = System.getProperty("user.home") + File.separator + path;
            System.out.println(path);
            br = new BufferedReader(new FileReader(path));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                String trimmedLine = strLine.trim();
                if (!"".equals(trimmedLine)) {
                    functionWords.add(trimmedLine.trim());
                }
            }
            br.close();
        } catch (Exception e) {
        }
    }

    /**
     * Create a list containing the number of occurrences of the various
     * function words in the post (list of extracted words)
     *
     * @param words
     * @return
     */
    public ArrayList<Float> countFunctionWords(List<String> words) {
        ArrayList<Float> tmpCounter = new ArrayList<>(Collections.nCopies(functionWords.size(), 0.0f));	// Initialize to zero

        for (String word : words) {
            if (functionWords.contains(word)) {
                int place = functionWords.indexOf(word);
                float value = tmpCounter.get(place);
                value++;
                tmpCounter.set(place, value);
            }
        }
        // "Normalize" the values by dividing with length of the post (nr of words in the post)
        for (int i = 0; i < tmpCounter.size(); i++) {
            tmpCounter.set(i, tmpCounter.get(i) / (float) words.size());
        }
        return tmpCounter;
    }

    /**
     * Create a list containing the number of occurrences of letters a to z in
     * the text
     *
     * @param post
     * @return
     */
    public ArrayList<Float> countCharactersAZ(String post) {
        post = post.toLowerCase();	// Upper or lower case does not matter, so make all letters lower case first...
        char[] ch = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'ö', 'å', 'ä'};
        ArrayList<Float> tmpCounter = new ArrayList<>(Collections.nCopies(ch.length, 0.0f));
        for (int i = 0; i < ch.length; i++) {
            int value = countOccurrences(post, ch[i]);
            tmpCounter.set(i, (float) value);
        }
        // "Normalize" the values by dividing with total nr of characters in the post (excluding white spaces)
        int length = post.replaceAll(" ", "").length();
        for (int i = 0; i < tmpCounter.size(); i++) {
            tmpCounter.set(i, tmpCounter.get(i) / (float) length);
        }
        return tmpCounter;
    }

    /**
     * Create a list containing the number of special characters in the text
     *
     * @param post
     * @return
     */
    public ArrayList<Float> countSpecialCharacters(String post) {
        post = post.toLowerCase();	// Upper or lower case does not matter, so make all letters lower case first...
        char[] ch = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '?', '!', ',', ';', ':', '(', ')', '"', '-', '´'};
        ArrayList<Float> tmpCounter = new ArrayList<>(Collections.nCopies(ch.length, 0.0f));
        for (int i = 0; i < ch.length; i++) {
            int value = countOccurrences(post, ch[i]);
            tmpCounter.set(i, (float) value);
        }
        // "Normalize" the values by dividing with total nr of characters in the post (excluding whitespaces)
        int length = post.replaceAll(" ", "").length();
        for (int i = 0; i < tmpCounter.size(); i++) {
            tmpCounter.set(i, tmpCounter.get(i) / (float) length);
        }
        return tmpCounter;
    }

    /**
     * Counts the frequency of various word lengths in the list of words.
     *
     * @param words
     * @return
     */
    public ArrayList<Float> countWordLengths(List<String> words) {
        ArrayList<Float> tmpCounter = new ArrayList<>(Collections.nCopies(20, 0.0f));	// Where 20 corresponds to the number of word lengths of interest 
        int wordLength = 0;
        for (String word : words) {
            wordLength = word.length();
            // We only care about wordLengths in the interval 1-20
            if (wordLength > 0 && wordLength <= 20) {
                float value = tmpCounter.get(wordLength - 1);	// Observe that we use wordLength-1 as index!
                value++;
                tmpCounter.set(wordLength - 1, value);
            }
        }
        // "Normalize" the values by dividing with length of the post (nr of words in the post)
        for (int i = 0; i < tmpCounter.size(); i++) {
            tmpCounter.set(i, tmpCounter.get(i) / (float) words.size());
        }
        return tmpCounter;
    }

    /**
     * Counts the frequency of various sentence lengths in the post.
     *
     * @param post
     * @return
     */
    public ArrayList<Float> countSentenceLengths(String post) {
        ArrayList<Float> tmpCounter = new ArrayList<>(Collections.nCopies(6, 0.0f));	// Where 6 corresponds to the number of sentence lengths of interest
        // Split the post into a number of sentences
        List<String> sentences = splitIntoSentences(post);
        int nrOfWords = 0;
        for (String sentence : sentences) {
            // Get number of words in the sentence
            List<String> words = extractWords(sentence);
            nrOfWords = words.size();
            if (nrOfWords > 0 && nrOfWords <= 10) {
                tmpCounter.set(0, tmpCounter.get(0) + 1);
            } else if (nrOfWords <= 20) {
                tmpCounter.set(1, tmpCounter.get(1) + 1);
            } else if (nrOfWords <= 30) {
                tmpCounter.set(2, tmpCounter.get(2) + 1);
            } else if (nrOfWords <= 40) {
                tmpCounter.set(3, tmpCounter.get(3) + 1);
            } else if (nrOfWords <= 50) {
                tmpCounter.set(4, tmpCounter.get(4) + 1);
            } else if (nrOfWords >= 51) {
                tmpCounter.set(5, tmpCounter.get(5) + 1);
            }
        }
        // "Normalize" the values by dividing with nr of sentences in the post
        for (int i = 0; i < tmpCounter.size(); i++) {
            tmpCounter.set(i, tmpCounter.get(i) / (float) sentences.size());
        }
        return tmpCounter;
    }

    /**
     * Splits a post/text into a number of sentences
     *
     * @param text
     * @return
     */
    public List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            sentences.add(text.substring(start, end));
        }
        return sentences;
    }

    /**
     * Count the number of occurrences of certain character in a String
     *
     * @param haystack
     * @param needle
     * @return
     */
    public static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    /**
     * Loops through all aliases and construct their feature vectors
     *
     * @param user
     * @return
     */
    public List<Float> createFeatureVectors(Alias user) {
        List<Float> featureVector = new ArrayList<>();
        featVectorForAllAliases = new ArrayList<>();
        //  for (Alias alias : aliases) {
        int cnt = 0;
        user.setFeatureVectorPosList(user.initializeFeatureVectorPostList());
        // Calculate each part of the "feature vector" for each individual post
        for (String post : user.getPosts()) {
            List<String> wordsInPost = extractWords(post);
            int placeInFeatureVector = 0;

            placeInFeatureVector = countFunctionWords(wordsInPost).size();

            user.addToFeatureVectorPostList(countFunctionWords(wordsInPost), 0, cnt);
            user.addToFeatureVectorPostList(countWordLengths(wordsInPost), placeInFeatureVector, cnt);

            placeInFeatureVector = placeInFeatureVector + countWordLengths(wordsInPost).size();
            user.addToFeatureVectorPostList(countCharactersAZ(post), placeInFeatureVector, cnt);

            placeInFeatureVector = placeInFeatureVector + countSpecialCharacters(post).size();
            user.addToFeatureVectorPostList(countSpecialCharacters(post), placeInFeatureVector, cnt);
            cnt++;
            //   }

            ArrayList<ArrayList<Float>> featureVectorList = user.getFeatureVectorPosList();

            int numberOfPosts = user.getPosts().size();
            int nrOfFeatures = featureVectorList.get(0).size();
            featureVector = new ArrayList<>(Collections.nCopies(nrOfFeatures, 0.0f));
            // Now we average over all posts to create a single feature vector for each alias
            for (int i = 0; i < nrOfFeatures; i++) {
                float value = 0.0f;
                for (int j = 0; j < numberOfPosts; j++) {
                    value += featureVectorList.get(j).get(i);
                }
                value /= numberOfPosts;
                featureVector.set(i, value);
            }
            user.setFeatureVector(featureVector);
            featVectorForAllAliases.add(featureVector);
        }
        return featureVector;
        //normalizeFeatureVector();
    }

    /**
     * Loops through all aliases and construct their feature vectors
     *
     * @param user
     */
    public void createFeatureVectors(List<Alias> user) {
        List<Float> featureVector = new ArrayList<>();
        featVectorForAllAliases = new ArrayList<>();
        for (Alias alias : user) {
            int cnt = 0;
            alias.setFeatureVectorPosList(alias.initializeFeatureVectorPostList());
            // Calculate each part of the "feature vector" for each individual post
            for (String post : alias.getPosts()) {
                List<String> wordsInPost = extractWords(post);
                alias.addToFeatureVectorPostList(countFunctionWords(wordsInPost), 0, cnt);
                alias.addToFeatureVectorPostList(countWordLengths(wordsInPost), 293, cnt);
                alias.addToFeatureVectorPostList(countCharactersAZ(post), 313, cnt);
                alias.addToFeatureVectorPostList(countSpecialCharacters(post), 339, cnt);
                cnt++;
            }

            ArrayList<ArrayList<Float>> featureVectorList = alias.getFeatureVectorPosList();

            int numberOfPosts = alias.getPosts().size();
            int nrOfFeatures = featureVectorList.get(0).size();
            featureVector = new ArrayList<>(Collections.nCopies(nrOfFeatures, 0.0f));
            // Now we average over all posts to create a single feature vector for each alias
            for (int i = 0; i < nrOfFeatures; i++) {
                float value = 0.0f;
                for (int j = 0; j < numberOfPosts; j++) {
                    value += featureVectorList.get(j).get(i);
                }
                value /= numberOfPosts;
                featureVector.set(i, value);
            }
            alias.setFeatureVector(featureVector);
            featVectorForAllAliases.add(featureVector);
        }
        normalizeFeatureVector();
    }

    public void createFeatureVectors() {
        List<Float> featureVector;
        featVectorForAllAliases = new ArrayList<>();
        for (Alias alias : aliases) {
            int cnt = 0;
            alias.setFeatureVectorPosList(alias.initializeFeatureVectorPostList());
            // Calculate each part of the "feature vector" for each individual post
            for (String post : alias.getPosts()) {
                List<String> wordsInPost = extractWords(post);
                int placeInFeatureVector = 0;

                alias.addToFeatureVectorPostList(countFunctionWords(wordsInPost), 0, cnt);

                placeInFeatureVector = countFunctionWords(wordsInPost).size();
                alias.addToFeatureVectorPostList(countWordLengths(wordsInPost), placeInFeatureVector, cnt);

                placeInFeatureVector = placeInFeatureVector + countWordLengths(wordsInPost).size();
                alias.addToFeatureVectorPostList(countCharactersAZ(post), placeInFeatureVector, cnt);

                placeInFeatureVector = placeInFeatureVector + countSpecialCharacters(post).size();
                alias.addToFeatureVectorPostList(countSpecialCharacters(post), placeInFeatureVector, cnt);
                cnt++;
            }

            ArrayList<ArrayList<Float>> featureVectorList = alias.getFeatureVectorPosList();

            int numberOfPosts = alias.getPosts().size();
            int nrOfFeatures = featureVectorList.get(0).size();
            featureVector = new ArrayList<>(Collections.nCopies(nrOfFeatures, 0.0f));
            // Now we average over all posts to create a single feature vector for each alias
            for (int i = 0; i < nrOfFeatures; i++) {
                float value = 0.0f;
                for (int j = 0; j < numberOfPosts; j++) {
                    value += featureVectorList.get(j).get(i);
                }
                value /= numberOfPosts;
                featureVector.set(i, value);
            }

            alias.setFeatureVector(featureVector);
            featVectorForAllAliases.add(featureVector);
        }
        normalizeFeatureVector();
    }

    /**
     * Used for comparing two feature vectors
     *
     * @param featVector1
     * @param featVector2
     * @return
     */
    public double compareFeatureVectors(List<Float> featVector1, List<Float> featVector2) {
        List<Float> floatList = featVector1;
        float[] floatArray1 = new float[floatList.size()];

        for (int i = 0; i < floatList.size(); i++) {
            Float f = floatList.get(i);
            floatArray1[i] = (f != null ? f : Float.NaN);
        }

        List<Float> floatList2 = featVector2;
        float[] floatArray2 = new float[floatList2.size()];

        for (int i = 0; i < floatList2.size(); i++) {
            Float f = floatList2.get(i);
            floatArray2[i] = (f != null ? f : Float.NaN);
        }
        return calculateSimilarity(floatArray1, floatArray2);
    }

    /**
     * Calculates cosine similarity between two real vectors
     *
     * @param value1
     * @param value2
     * @return
     */
    public double calculateSimilarity(float[] value1, float[] value2) {
        float sum = 0.0f;
        float sum1 = 0.0f;
        float sum2 = 0.0f;
        for (int i = 0; i < value1.length; i++) {
            float v1 = value1[i];
            float v2 = value2[i];
            if ((!Float.isNaN(v1)) && (!Float.isNaN(v2))) {
                sum += v2 * v1;
                sum1 += v1 * v1;
                sum2 += v2 * v2;
            }
        }
        if ((sum1 > 0) && (sum2 > 0)) {
            double result = sum / (Math.sqrt(sum1) * Math.sqrt(sum2));
            // result can be > 1 (or -1) due to rounding errors for equal vectors, 
            //but must be between -1 and 1
            return Math.min(Math.max(result, -1d), 1d);
            //return result;
        } else if (sum1 == 0 && sum2 == 0) {
            return 1d;
        } else {
            return 0d;
        }
    }

    /**
     * Calculate similarity between all pairs of aliases (a lot of comparisons
     * if there are many aliases)
     */
    public void compareAllPairsOfAliases() {
        for (int i = 0; i < aliases.size(); i++) {
            for (int j = i + 1; j < aliases.size(); j++) {
                double sim = compareFeatureVectors(aliases.get(i).getFeatureVector(), aliases.get(j).getFeatureVector());
                System.out.println("Similarity between alias " + aliases.get(i).getUserID() + " and " + aliases.get(j).getUserID() + " is: " + sim);
            }
        }
    }

    /**
     * Find the index of the alias that is most similar to the selected alias.
     *
     * @param index
     * @return
     */
    public int findBestMatch(int index) {
        double highestSimilarity = -10.0;
        int indexMostSimilar = 0;
        for (int i = 0; i < aliases.size(); i++) {
            if (i != index) {
                double sim = compareFeatureVectors(aliases.
                        get(i).getFeatureVector(),
                        aliases.get(index).getFeatureVector());
                if (sim > highestSimilarity) {
                    highestSimilarity = sim;
                    indexMostSimilar = i;
                }
            }
        }
        return indexMostSimilar;
    }

    /**
     * Standardize/normalize the feature vectors for all aliases. Aim is mean 0
     * and variance 1 for each feature vector. Please note that this will result
     * in feature vectors that depend on the feature vectors of the other
     * aliases...
     */
    public void normalizeFeatureVector() {
        int nrOfFeatures = featVectorForAllAliases.get(0).size();
        List<Double> avgs = new ArrayList<>(nrOfFeatures);
        List<Double> stds = new ArrayList<>(nrOfFeatures);

        // Calculate avg (mean) for each feature
        for (int i = 0; i < nrOfFeatures; i++) {
            double sum = 0.0;
            for (int j = 0; j < aliases.size(); j++) {
                sum += featVectorForAllAliases.get(j).get(i);
            }
            avgs.add(sum / aliases.size());
        }

        // Calculate std for each feature
        for (int i = 0; i < nrOfFeatures; i++) {
            double avg = avgs.get(i);
            double tmp = 0.0;
            for (int j = 0; j < aliases.size(); j++) {
                tmp += (avg - featVectorForAllAliases.get(j).get(i)) * (avg - featVectorForAllAliases.get(j).get(i));
            }
            stds.add(Math.sqrt(tmp / aliases.size()));
        }

        // Do the standardization of the feature vectors
        for (int i = 0; i < nrOfFeatures; i++) {
            for (int j = 0; j < aliases.size(); j++) {
                if (stds.get(i) == 0.0) {
                    aliases.get(j).setFeatureValue(i, 0.0f);
                } else {
                    float featureVector = featVectorForAllAliases.get(j).get(i);
                    double average = avgs.get(i);
                    double stdDev = stds.get(i);

                    /* System.out.println("Feature Vector: " + featureVector);
                    System.out.println("Average" + i + " "+ average);
                    System.out.println("Standard Deviation: " + stdDev);*/
                    float newFeat = (float) ((featureVector - average) / stdDev);
                    /* System.out.println("New Feature: " + newFeat);
                    System.out.println("---------------");*/

                    aliases.get(j).setFeatureValue(i, newFeat);
                }
            }
        }

    }

    public static void main(String[] args) throws SQLException {
        StylometricAnalysisMain test = new StylometricAnalysisMain();
        Alias dulney1 = new Alias("Dulney1");

        String text1 = "Invandrare är absolut värst på att ljuga, manipulera, köra fulsälj, hetsa etc. "
                + "De är helt skamlösa och passar därför extremt bra in i denna bransch. Många av dem "
                + "saknade helt moraliska kompasser och det i en bransch där det redan är "
                + "illa ställt." + "araber har våldsammare gener" + "invandrare är absolut värst på att ljuga,"
                + " manipulera, köra fulsälj, hetsa etc";

        String text12 = "(namnet på den dömde) är väl ett zigenarnamn va? Känns viktigt att påpeka isf"
                + "även om födslotalen bland muslimer som bott ett längre tag i t.ex. Sverige eller Danmark "
                + "minskar så motverkas det ändå att man, i alla fall i Sveriges fall, importerar "
                + "nya muslimer i tiotusental som kommer att föda lika många barn som de muslimer " + "Man"
                + " får inte missa en sådan grundläggande sak, här i Sverige har ";

        String text13 = "vi fortfarande en massinvandring som är bortom all vett och sans, vi har "
                + "knappt börjat oroa oss för nästa steg som är att vi blir 'utfödda' i vårt "
                + "eget land, och vi betalar dem för det genom generöst vårdnadsbidrag och "
                + "barnbidrag." + "Systemet måste göras om helt och hållet, alla bidrag ska "
                + "omvandlas till skattelättnader eftersom vi då åtminstone träffar de som "
                + "arbetar(dvs svenskar till största delen). De som inte arbetar kommer ";

        String text14 = "alltså inte att tjäna något på att föda fler barn. Det är en början." + "Vedervärdigt, det "
                + "är bara en tidsfråga innan hela fasaden krackelerar fullständigt och Svensson på allvar inser "
                + "att deras äldre ligger och dör i sina egna kroppsvätskor för att Ali 23 år ska kunna bli servad "
                + "med mat lagad i ett lyxkök." + "VAKNA SVERIGE FÖR I HELV...." + "vad som hänt är att man "
                + "försummar sin skolplikt, man utnyttjar välfärdssystemen hänsynslöst, man har ett "
                + "bemötande mot svenskar som är under all kritik.";

        dulney1.addPost(text1);
        dulney1.addPost(text12);
        dulney1.addPost(text13);
        dulney1.addPost(text14);

        test.aliases.add(dulney1);

        Alias dulney2 = new Alias("Dulney2");

        String text2 = "Paulina Neuding skrev nyligen en kolumn i SvD om hur medier och folkpartister"
                + "(för vilken gång i ordningen?) vilseleder allmänheten om statusen kring mångkultur och "
                + "invandring";

        dulney2.addPost(text13);
        dulney2.addPost(text14);
        dulney2.addPost(text1);
        dulney2.addPost(text12);

        test.aliases.add(dulney2);

        test.createFeatureVectors();

        for (Alias alias : test.aliases) {
            List<Float> featVec = alias.getFeatureVector();
            System.out.println("Feature Vector" + featVec);
            System.out.println("------------");
        }

        test.compareAllPairsOfAliases();
        System.out.println("The best matching alias is: " + test.findBestMatch(0));

    }
}
