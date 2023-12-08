package spine_sources;

import ij.IJ;
import ij.Prefs;
import java.util.ArrayList;
import java.util.List;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.measurements.*;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationClosestDistance;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationDistance;
import mcib3d.image3d.ImageHandler;


/**
 *
 * @author jeffgilles from Thomas Boudier code
 */
public class Measures {
    
    /**
     *
     * @param object3DList
     * @param ima
     * @return
     */
//    public static ResultsFrame measurements3D (List<Object3DInt> object3DList, ImageHandler ima) { 
    public static ResultsFrame measurements3D (List<Spine> spineList, ImageHandler ima) { 
//        if (object3DList == null) return null;
        
        if (spineList == null) return null;
        ArrayList<String> headings = new ArrayList<>();
        
        headings.add("Nb");
        headings.add("Name");
        headings.add("Label");
        headings.add("Type");
        

        if (Prefs.get("Spine3D-Options_volume.boolean", true)) {
            headings.add("Head Vol (unit)");
            headings.add("Head Vol (pix)");
        }
        if (Prefs.get("Spine3D-Options_neckLen.boolean", true)) {
            headings.add("Neck length (unit)");
            headings.add("Neck length (pxl)");
        }
        
        if (Prefs.get("Spine3D-Options_NtoH.boolean", true)) {
            headings.add("NtoH (unit)");
            headings.add("NtoH (pxl)");
        }
        
        headings.add("Dist. BB (unit)");
        headings.add("Dist. BB (pxl)");
        
        if (Prefs.get("Spine3D-Options_DtoF.boolean", true)) {
            headings.add("Dist. DF (unit)");
            headings.add("Dist. DF (pxl)");
        }
        
        double[][] headDistsBB = null;
        if (Prefs.get("Spine3D-Options_Closest.boolean", true)){
            headDistsBB = getClosestHead(spineList);
            headings.add("Closest Head BB");
            headings.add("Closest Head dist. BB (unit)");
        }
        
        if (Prefs.get("Spine3D-Options_surface.boolean", true)) {
            headings.add("Surf (unit)");
            headings.add("Surf (pix)");
            headings.add("SurfCorr (pix)");
        }
        if (Prefs.get("Spine3D-Options_dist2Surf.boolean", true)) {
            headings.add("DCMin (pxl)");
            headings.add("DCMax (pxl)");
            headings.add("DCMean (pxl)");
            headings.add("DCMin (unit)");
            headings.add("DCMax (unit)");
            headings.add("DCMean (unit)");
//            headings.add("DCSD (unit)");
        }
        if (Prefs.get("Spine3D-Options_ellipse.boolean", true)) {
            headings.add("EllVol(Unit)");
            headings.add("EllSpareness");
            headings.add("EllMajRad(Unit)");
            headings.add("EllElon");
            headings.add("EllFlatness");
        }
        if (Prefs.get("Spine3D-Options_centroid.boolean", true)) {
            headings.add("CX (pix)");
            headings.add("CY (pix)");
            headings.add("CZ (pix)");
        }
        if (Prefs.get("Spine3D-Options_COM.boolean", true)) {
            headings.add("CMx (pix)");
            headings.add("CMy (pix)");
            headings.add("CMz (pix)");
        }
        if (Prefs.get("Spine3D-Options_compacity.boolean", true)) {
            headings.add("Comp (pix)");
            headings.add("Spher (pix)");
            headings.add("CompCorr (pix)");
            headings.add("SpherCorr (pix)");
//            headings.add("Comp (unit)");
//            headings.add("Spher (unit)");
//            headings.add("CompDiscrete");
        }
        if (Prefs.get("Spine3D-Options_intensity.boolean", true)){
            headings.add("min");
            headings.add("Max");
            headings.add("Mean");
            headings.add("StdDev");
            headings.add("Integrated Dens");
        }
        if (Prefs.get("Spine3D-Options_Mode.boolean", true)){
            headings.add("Mode");
        }
        if (Prefs.get("Spine3D-Options_feret.boolean", true)) {
            headings.add("Feret (unit)");
            headings.add("Feret (pxl)");
        }
        
        
//        final Object[][] data = new Object[object3DList.size()][headings.size()];
        final Object[][] data = new Object[spineList.size()][headings.size()];

        Spine sp;
        Object3DInt obj;
        
        double resXY;
        double resZ;
        
//        for (int i = 0; i < object3DList.size(); i++) {
//            obj = object3DList.get(i);
        for (int i = 0; i < spineList.size(); i++) {
            sp = spineList.get(i);
            obj = sp.head;
            
            int h = 0;
            data[i][h++] = i;
            data[i][h++] = obj.getName();
            data[i][h++] = obj.getLabel();
            if (obj.getType()==0){
                data[i][h++] = "Stubby spine";
            }
            else{
                data[i][h++] = "Spine with neck";
            }
//            data[i][h++] = obj.getType();
            resXY = obj.getVoxelSizeXY();
            resZ = obj.getVoxelSizeZ();
            
            if (Prefs.get("Spine3D-Options_volume.boolean", true)) {
                MeasureVolume vol = new MeasureVolume(obj);
                data[i][h++] = vol.getVolumeUnit();                           //BE CAREFUL OF UNIT vs PIX
                data[i][h++] = vol.getVolumePix();
            }
            
            if (Prefs.get("Spine3D-Options_neckLen.boolean", true)) {
                data[i][h++] = sp.neckDist[1]; //unit
                data[i][h++] = sp.neckDist[0]; //pxl
            }
            
            if (Prefs.get("Spine3D-Options_NtoH.boolean", true)) {
                data[i][h++] = sp.btoHDist[1];
                data[i][h++] = sp.btoHDist[0];
            }
            
            //Automatic values in Table
            data[i][h++] = sp.distancesHeadDendrite[1]; //BBunit
            data[i][h++] = sp.distancesHeadDendrite[0]; //BBpxl
            
            if (Prefs.get("Spine3D-Options_DtoF.boolean", true)) {
                data[i][h++] = sp.distancesHeadDendrite[3]; //HausDunit
                data[i][h++] = sp.distancesHeadDendrite[2]; //HausDpxl
            }
            
            if (Prefs.get("Spine3D-Options_Closest.boolean", true)){ //closest head
//                IJ.log("headLabel="+headDistsBB[i]+" closest="+headDistsBB[i+1][0]);
                data[i][h++] = headDistsBB[i][1];
                data[i][h++] = headDistsBB[i][2];
            }
            
            if (Prefs.get("Spine3D-Options_surface.boolean", true)) {
                MeasureSurface surf = new MeasureSurface(obj);
                Double[] surfVal = surf.getAllValuesMeasurement();
                data[i][h++] = surfVal[2];//unit
                data[i][h++] = surfVal[1];//pxl
                data[i][h++] = surfVal[3];//CorrPxl
//                data[i][h++] = obj.getAreaPixels();
//                if (obj instanceof Object3DVoxels) {
//                    data[i][h++] = ((Object3DVoxels) obj).getAreaPixelsCorrected();
//                } else {
//                    data[i][h++] = -1;
//                }
            }
            
            if (Prefs.get("Spine3D-Options_dist2Surf.boolean", true)) {
                MeasureDistancesCenter dc = new MeasureDistancesCenter(obj);
                Double[] dcval = dc.getAllValuesMeasurement();
                data[i][h++] = dcval[1];
                data[i][h++] = dcval[2];
                data[i][h++] = dcval[3];
                data[i][h++] = dcval[5];
                data[i][h++] = dcval[6];
                data[i][h++] = dcval[7];
            }
            
            if (Prefs.get("Spine3D-Options_ellipse.boolean", true)) {
                MeasureEllipsoid elli = new MeasureEllipsoid(obj);
                Double[] values = elli.getAllValuesMeasurement();
                data[i][h++] = values[1];
                data[i][h++] = values[2];
                data[i][h++] = values[3];
                data[i][h++] = values[4];
                data[i][h++] = values[5];
            }
            
            if (Prefs.get("Spine3D-Options_centroid.boolean", true)) {
                MeasureCentroid centroid = new MeasureCentroid(obj);
                Double[] measureC = centroid.getAllValuesMeasurement();
                    data[i][h++] = measureC[1];     //Cx pxl
                    data[i][h++] = measureC[2];
                    data[i][h++] = measureC[3];
            }
            if (Prefs.get("Spine3D-Options_COM.boolean", true)) {
                MeasureCenterOfMass cmass = new MeasureCenterOfMass(obj, ima);
                Double[] measureCm = cmass.getAllValuesMeasurement();
                data[i][h++] = measureCm[1];     //CMx pxl
                data[i][h++] = measureCm[2];
                data[i][h++] = measureCm[3];
            }
            
            if (Prefs.get("Spine3D-Options_compacity.boolean", true)) {
                MeasureCompactness compact = new MeasureCompactness(obj);
                Double[] measureComp = compact.getAllValuesMeasurement();
                data[i][h++] = measureComp[1];//Comp (pix)
                data[i][h++] = measureComp[2];//Spher (pix)
                data[i][h++] = measureComp[3];//CompCorr (pix)
                data[i][h++] = measureComp[4];//SpherCorr (pix)
//                data[i][h++] = measureComp[4];//Comp (unit)
//                data[i][h++] = measureComp[5];//Spher (unit)
//                data[i][h++] = measureComp[6];//CompDiscrete
            }
            
            if (Prefs.get("Spine3D-Options_intensity.boolean", true)) {
                MeasureIntensity intens = new MeasureIntensity(obj, ima);
                Double[] measureInt = intens.computeIntensityValues();
                data[i][h++] = measureInt[0];
                data[i][h++] = measureInt[1];
                data[i][h++] = measureInt[2];
                data[i][h++] = measureInt[3];
                data[i][h++] = measureInt[4];
            }
            if (Prefs.get("Spine3D-Options_Mode.boolean", true)){
                MeasureIntensityHist hist = new MeasureIntensityHist(obj, ima);
//                data[i][h++] = hist.getMedianValue();
                data[i][h++] = hist.getModeValue();
            }

            if (Prefs.get("Spine3D-Options_feret.boolean", true)) {
                MeasureFeret feret = new MeasureFeret(obj);
                Double[] measureFeret = feret.getAllValuesMeasurement();
                data[i][h++] = measureFeret[2];
                data[i][h++] = measureFeret[1];
            }
            
            
        }
        
        Objects3DIntPopulation Oheads = new Objects3DIntPopulation();
        spineList.forEach(spine -> {
            Oheads.addObject(spine.head);
        });
        
        double distMax = 20000;
        MeasurePopulationClosestDistance distance = new MeasurePopulationClosestDistance(Oheads, Oheads, distMax, MeasurePopulationClosestDistance.CLOSEST_BB1_PIX);
        distance.setRemoveZeroDistance(true);
        Oheads.getObjects3DInt().forEach(objt -> {
            //IJ.log("o "+ objt.getLabel());
            double[] dists = distance.getValuesObject1Sorted(objt.getLabel(),true);
            //IJ.log("k");
            if(dists.length>0){ IJ.log("obj: "+dists[0]);}//+" obj2:"+dists[1]+" dist:"+dists[2]);
        });
        
        String[] heads = new String[headings.size()];
        heads = headings.toArray(heads);
        ResultsFrame tableResultsMeasure = new ResultsFrame("Spines Measure", heads, data, ResultsFrame.OBJECT_1);

        return tableResultsMeasure;
        
    }
    
    private static double[][] getClosestHead(List<Spine> spineList){
        Objects3DIntPopulation heads = new Objects3DIntPopulation();
        spineList.forEach(spine -> {
            heads.addObject(spine.head);
        });
        double distMax = 1.8;//um
        MeasurePopulationDistance distance = new MeasurePopulationDistance(heads, heads, distMax, MeasurePopulationDistance.DIST_BB_UNIT);
        Object3DInt ob = heads.getFirstObject();
        double[][] dists2 = new double[(int)heads.getMaxLabel()][3]; // getMaxLabel = avoid PB when delete
        heads.getObjects3DInt().forEach(obj -> {
            
            double[] dists = distance.getValuesObject1Sorted(obj.getLabel(), true);
//            IJ.log("len "+dists.length);
            if (dists.length > 5){
                dists2[(int)obj.getLabel()-1][0] = dists[3];
                dists2[(int)obj.getLabel()-1][1] = dists[4];
                dists2[(int)obj.getLabel()-1][2] = dists[5];
//            IJ.log("obj1:"+dists[(int)obj.getLabel()][3]+" obj2:"+dists[(int)obj.getLabel()][4]+" dist:"+dists[(int)obj.getLabel()][5]);//0, 1, 2 are the closest, so = to the same object
//            IJ.log("obj1:"+dists2[(int)obj.getLabel()-1][0]+" obj2:"+dists2[(int)obj.getLabel()-1][1]+" dist:"+dists2[(int)obj.getLabel()-1][2]);
            }
            else{
                dists2[(int)obj.getLabel()-1][0] = (double)obj.getLabel();
                dists2[(int)obj.getLabel()-1][1] = 0;
                dists2[(int)obj.getLabel()-1][2] = 0;
            }
        });
        
        return dists2;
    }
    
}
