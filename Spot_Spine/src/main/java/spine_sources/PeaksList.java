package spine_sources;


//import ij.IJ;
import ij.ImagePlus;
//import java.util.ArrayList;
import java.util.Comparator;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.distanceMap3d.EDT;
import mcib3d.image3d.processing.FastFilters3D;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PeaksList {
    List<Peak3D> peaks;

    public PeaksList() {
        peaks = new LinkedList<>();
    }
    
    public void wrapList(List<Peak3D> spineList){
        peaks = spineList;
    }

    public void buildMaxima(ImagePlus plus, float radXY, float radZ) {
        ImageHandler handler = ImageHandler.wrap(plus);
        FastFilters3D.getListMaxima(handler, radXY, radXY, radZ, 0, false).stream()
                .forEach(max -> 
                    peaks.add(new Peak3D(max.getRoundX(), max.getRoundY(), max.getRoundZ(), handler.getPixel(max.getRoundX(), max.getRoundY(), max.getRoundZ()))));
    }

    public ImagePlus buildEDT(ImagePlus plus) {
        ImageHandler handler = ImageHandler.wrap(plus);
        ImageHandler edt = EDT.run(handler, 10, true, 0);
        peaks.stream().forEach(pk -> pk.setDistToDendrite(edt.getPixel((int) pk.x, (int) pk.y, (int) pk.z)));
        
        return edt.getImagePlus();
    }

    /**
     * Filter Peaks depending of intensity & distance
     * @param thrVal threshold value
     * @param distMin distance mini
     * @param distMax distance maxi
     * @return 
     */
    public List<Peak3D> filterPeaks(double thrVal, double distMin, double distMax) {
        return peaks.stream()
                .filter(pk -> pk.v > thrVal)
                .filter(pk -> pk.getDistToDendrite() < distMax)
                .filter(pk -> pk.getDistToDendrite() > distMin)
                .collect(Collectors.toList());
    }
    
    public void addPeak3D(Peak3D pk){
        peaks.add(pk);
    }
    
    /**
     * Return the max intensity in the list
     * @return 
     */
    public double getMaxValue (){
        return peaks.stream()
                    .max(Comparator.comparing(Peak3D::getValue))
                    .get().getValue();
    }
    
    /**
     * Return the max distance in the list
     * @return 
     */
    public double getMaxDist (){
        return peaks.stream()
                    .max(Comparator.comparing(Peak3D::getDistToDendrite))
                    .get().getDistToDendrite();
    }
    
    /**
     * Return the min distance in the list
     * @return 
     */
    public double getMinDist (){
        return peaks.stream()
                    .min(Comparator.comparing(Peak3D::getDistToDendrite))
                    .get().getDistToDendrite();
    }
    
}
