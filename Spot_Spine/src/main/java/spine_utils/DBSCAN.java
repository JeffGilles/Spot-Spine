/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package spine_utils;

import ij.IJ;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import spine_sources.Peak3D;
import spine_utils.KDTreeC.KDPoint;


public final class DBSCAN {
    
    /** maximum distance of values to be considered as cluster */
    private double epsilon = 1f;
    
    /** minimum number of members to consider cluster */
    private int minNumberOfClusterMembers = 2;
    
    /** internal list of input values to be clustered */
    private List<Peak3D> inputValues = null;
    
    /** index visited points */
    private HashSet<Peak3D> visitedPoints = new HashSet<>();
    
    private KDTreeC kd = null;
    
    private int nbDimensions = 3;
    
    /**
     * Creates a DBSCAN clusterer instance
     * 
     * @param inputValues Input values to be clustered
     * @param minNumElements Minimum number of elements to constitute cluster
     * @param maxDistance Maximum distance of elements to consider clustered
     * @param dimension Number of dimensions of the data
     */
    public DBSCAN(final List<Peak3D> inputValues, int minNumElements, double maxDistance) {
        setInputValues(inputValues);
        setMinNumberOfMembersForCluster(minNumElements);
        setMaxDistanceOfClusterMembers(maxDistance);
//        setKDTree(dimension);
    }
    
    public void setInputValues(final List<Peak3D> collection) {
        if (collection == null) {
            throw new UnsupportedOperationException("DBSCAN: List of input values is null.");
        }
//        this.inputValues = new ArrayList<>(collection);
        this.inputValues = collection;
    }
    
    public void setMinNumberOfMembersForCluster(final int minimalNumberOfMembers) {
        this.minNumberOfClusterMembers = minimalNumberOfMembers;
    }
    
    /**
     * Sets the maximal distance members of the same cluster can have while
     * still be considered in the same cluster.
     * 
     * @param maximalDistance epsilon
     */
    public void setMaxDistanceOfClusterMembers(final double maximalDistance) {
        this.epsilon = maximalDistance;
    }
    
    /**
     * Set KDTree if kd search
     * @param dimension set number of dimension of the data
     */
    public void setKDTree(int dimension){
        KDTreeC kdT = new KDTreeC(dimension);
        
        inputValues.stream().forEach((Peak3D p) -> {
            kdT.add(Arrays.copyOf(p.toArray(), dimension), 1);
        });
        kdT.setScale2(1, 1, 1);
        this.kd = kdT;
        this.nbDimensions = dimension;
    }
    
    /**
     * Determines the neighbours of a given input value.
     * 
     * @param inputValue Input value for which neighbours are to be determined
     * @return list of neighbours
     */
    private List<Peak3D> getNeighbours(final Peak3D inputValue) {
        List<Peak3D> neighbours = new ArrayList<>();
        inputValues.stream().forEach(pt -> {
            if (measureDistance(inputValue.toArray(), pt.toArray()) <= epsilon) {
                neighbours.add(pt);
            }
        });
        return neighbours;
    }
    
    private List<Peak3D> getNeighboursKD(final Peak3D inputValue) {
        List<Peak3D> neighbours = new ArrayList<>();
        double[] inputPk = Arrays.copyOf(inputValue.toArray(), 3);
        
        KDPoint[] nearests = kd.getNearestNeighborsWithinDistance(inputPk, 4, epsilon);
//        if (inputValues.size() > minNumberOfClusterMembers){                         //to change
            //if nearest coords == coord peak3D, take it
            for (int i = 0; i < inputValues.size(); i++) {
                for (KDPoint nearest : nearests) {
                    if (inputValues.get(i).isSamePeak(nearest.pnt)){
                        neighbours.add(inputValues.get(i));
                    }
                }
            }
        
        return neighbours;
    }
    
    /**
     * Add elements of neighbours2 to neighbours1 if not in it
     * @param neighbours1
     * @param neighbours2
     * @return 
     */
    private List<Peak3D> addToNeighbours(final List<Peak3D> neighbours1, final List<Peak3D> neighbours2) {
        neighbours2.stream().forEach(pt -> {
            if (!neighbours1.contains(pt)) {
                neighbours1.add(pt);
            }});
        return neighbours1;
    }
    
    /**
     *
     * @return
     */
    public List<List<Peak3D>> performClustering() {
        //throws Exceptions
        if (inputValues == null || inputValues.isEmpty()) throw new UnsupportedOperationException("DBSCAN: List of input values is null.");
        if (inputValues.size() < 2) throw new UnsupportedOperationException("DBSCAN: Less than two input values cannot be clustered. Number of input values: " + inputValues.size());
        if (epsilon < 0)throw new UnsupportedOperationException("DBSCAN: Maximum distance of input values cannot be negative. Current value: " + epsilon);
        if (minNumberOfClusterMembers < 2) throw new UnsupportedOperationException("DBSCAN: Clusters must have minimum 2 members. Current value: " + minNumberOfClusterMembers);
    
        List<List<Peak3D>> resultList = new ArrayList<>();
        visitedPoints.clear();

        List<Peak3D> neighbours;
        int index = 0;
        
        while (inputValues.size() > index) {
            Peak3D p = inputValues.get(index);
            if (!visitedPoints.contains(p)) {
                visitedPoints.add(p);
                neighbours = getNeighbours(p);

                if (neighbours.size() >= minNumberOfClusterMembers) {
                    int ind = 0;
                    while (neighbours.size() > ind) {
                        Peak3D r = neighbours.get(ind);
                        if (!visitedPoints.contains(r)) {
                            visitedPoints.add(r);
                            List<Peak3D> individualNeighbours = getNeighbours(r);
                            if (individualNeighbours.size() >= minNumberOfClusterMembers) {
                                neighbours = addToNeighbours(neighbours, individualNeighbours);
                            }
                        }
                        ind++;
                    }
                    resultList.add(neighbours);
                }
            }
            index++;
        }
        
        return resultList;
    }
    
    public List<List<Peak3D>> performClusteringwKDTree(int nbDimension) {
        //throws Exceptions
        if (inputValues == null || inputValues.isEmpty()) throw new UnsupportedOperationException("DBSCAN: List of input values is null.");
        if (inputValues.size() < 2) throw new UnsupportedOperationException("DBSCAN: Less than two input values cannot be clustered. Number of input values: " + inputValues.size());
        if (epsilon < 0)throw new UnsupportedOperationException("DBSCAN: Maximum distance of input values cannot be negative. Current value: " + epsilon);
        if (minNumberOfClusterMembers < 2) throw new UnsupportedOperationException("DBSCAN: Clusters must have minimum 2 members. Current value: " + minNumberOfClusterMembers);
        if(kd == null) setKDTree(nbDimension);
        
        List<List<Peak3D>> resultList = new ArrayList<>();
        visitedPoints.clear();

        List<Peak3D> neighbours;
        int index = 0;
        
        while (inputValues.size() > index) {
            Peak3D p = inputValues.get(index);
            if (!visitedPoints.contains(p)) {
                visitedPoints.add(p);
                neighbours = getNeighboursKD(p);

                if (neighbours.size() >= minNumberOfClusterMembers) {
                    int ind = 0;
                    while (neighbours.size() > ind) {
                        Peak3D r = neighbours.get(ind);
                        if (!visitedPoints.contains(r)) {
                            visitedPoints.add(r);
                            List<Peak3D> individualNeighbours = getNeighboursKD(r);
                            if (individualNeighbours.size() >= minNumberOfClusterMembers) {
                                neighbours = addToNeighbours(neighbours, individualNeighbours);
                            }
                        }
                        ind++;
                    }
                    resultList.add(neighbours);
                }
            }
            index++;
        }
        
        return resultList;
    }
    
    private double measureDistance(double[] a, double[] b) {
        double total = 0;
        for (int i = 0; i < a.length; ++i) {
            total += (b[i] - a[i]) * (b[i] - a[i]) ;//* scale[i];
        }
        return Math.sqrt(total);
    }
    
//    private class Point3D {
//        
//        public double[] pnt;
//        public Object obj;
////        public double distanceSq;
//        
//        private Point3D(double[] p, Object o) {
//            pnt = p;
//            obj = o;
//        }
//    }
}