
package spine_sources;

import spine_utils.SWCReader;
import spine_path.AutoTrace2;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import mcib3d.geom.Point3D;
import mcib3d.geom2.BoundingBox;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.Measure2Distance;
import mcib3d.geom2.measurements.MeasureCentroid;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.segment.LocalThresholder;
import mcib3d.image3d.segment.LocalThresholderConstant;
import mcib3d.image3d.segment.LocalThresholderGaussFit;
import mcib3d.image3d.segment.LocalThresholderMean;
import mcib3d.image3d.segment.Segment3DSpots;
import mcib3d.image3d.segment.SpotSegmenter;
import mcib3d.image3d.segment.SpotSegmenterBlock;
import mcib3d.image3d.segment.SpotSegmenterClassical;
import mcib3d.image3d.segment.SpotSegmenterMax;
import spine_utils.DBSCAN;
import spine_utils.KDTreeC;

public class ComputeDendrite {
    //peaks
    public double thrsIntVal = 10, minDist = 0.1, maxDist = 3.5;
    public int kernelVar = 0;       // kernel variations
    public Calibration cal0, cal1;
    
    private ImagePlus imgshow;
    private ImagePlus EDT;
    
    public ImagePlus imageOrg;
    public ImagePlus dendriteShape;
    public ImagePlus imgMaxMerge;
    public ImagePlus imgMaxProj;
    public ImagePlus imaResult;
    public int max16 = (int) Math.pow(2, 16) - 1;
    public List<Peak3D> listPeaksOrg, trimListPeaks;
    public Object3DInt dendShape;
    //public Object3D dendShapeOld;
    public static ComputeDendrite instance;
    public Objects3DIntPopulation heads;
    public int nbObjectSelected = 0;
    public String textLogDendLength = "";
    
    public ImageHandler imaOrgHand;
    
    //full Spines
    public List<Spine> listSpines;
    public List<Spine> newSpines;
    public List<Float> spinesLabels;
    private ResultsFrame tableResultsMeasure;


    //@Override
    public void run(String s) {
        
        instance = this;
        
        if(WindowManager.getCurrentImage() == null){
            IJ.error("Need an image");
            return;
        }
        if (!dialog()) return; 
        imageOrg = WindowManager.getCurrentImage();

//        imageOrg = new ImagePlus("/Users/jeffgilles/Documents/Data/Spine_Test/SpineTest.tif");
        imaOrgHand = ImageHandler.wrap(imageOrg);

        // Calibration
        cal0 = imageOrg.getCalibration();
        if (!cal0.scaled()){
            IJ.showStatus("Calibrate your image");
            IJ.run(imageOrg, "Properties...", "");
            cal0 = imageOrg.getCalibration();
            if (!cal0.scaled())return;
        }
        cal1 = new Calibration();
        cal1.pixelWidth = 1;
        cal1.pixelHeight = 1;
        cal1.pixelDepth = 1;
        
        // SWC
        SWCReader reader = new SWCReader();
        FileInfo fileInfo = imageOrg.getOriginalFileInfo();
        String fileSWC = fileInfo.directory + fileInfo.fileName.replace(".tif", ".swc");
        if (!fileExists(fileSWC)){
            GenericDialog fileDial = new GenericDialog("SWC file error");
            fileDial.addMessage("Can't find SWC file with the same name");
            fileDial.addMessage("Select it here or create it first with SNT plugin or equivalent");
            fileDial.addFileField("", fileInfo.directory);
            fileDial.showDialog();
            if(fileDial.wasCanceled()) return;
            fileSWC = fileDial.getNextString();
        }
        imageOrg.hide();
        
        //dendrite shape
        reader.SWCFileReader(fileSWC);
        dendriteShape = reader.drawSWC(imageOrg.duplicate(), 20);
        textLogDendLength = "Dendrite length = "+reader.getLength() + " " +cal0.getUnits();
        IJ.log(""+textLogDendLength);
        dendShape = new Objects3DIntPopulation(ImageHandler.wrap(dendriteShape)).getFirstObject();
        
        updateMax();
        
        //Peaks Dialog box
        PeaksJDialog dialogdist = new PeaksJDialog(this, new javax.swing.JFrame(), false);
        dialogdist.setDendrite(this);
        dialogdist.setVisible(true);
        
    }
    
    public void updateMax(){
        
        // Build peaks List
        float radiusXY = 3;
        float radiusZ = 3;
        PeaksList peaks = new PeaksList();
        peaks.buildMaxima(imageOrg, radiusXY+kernelVar, radiusZ+kernelVar);
        EDT = peaks.buildEDT(dendriteShape);
        //dendriteShape.setCalibration(cal0);

        imgshow = NewImage.createImage("", imageOrg.getWidth(), imageOrg.getHeight(), imageOrg.getNSlices(), 16, NewImage.FILL_BLACK);
        
        
        // filter peaks list
        listPeaksOrg = peaks.filterPeaks(thrsIntVal, minDist, maxDist);
        //Object3DInt dendriteObject = new Object3DInt(ImageHandler.wrap(dendriteShape));
        
        listPeaksOrg.parallelStream().forEach(peak -> {
//                IJ.showStatus("computing distances to dendrite...");
//                peak.computeClosestDistance(dendriteObject);
                peak.setDistToDendrite(ImageHandler.wrap(EDT).getPixel((int) peak.x, (int) peak.y, (int) peak.z));
                        });
        
        // create an empty stack filled with black
        ImagePlus maxLocalStack = NewImage.createImage("Max_local_stack", imageOrg.getWidth(), imageOrg.getHeight(), imageOrg.getNSlices(), 16, NewImage.FILL_BLACK);
        
        // add max local in new stack
//        listPeaksOrg.stream().forEach(max -> maxLocalStack.getStack().setVoxel((int) max.getX(), (int) max.getY(), (int) max.getZ(), max16));
        
        // Trim max local on intensity and update maxLocalStack
        // filter peaks list
        
        // Keep max local original list
        trimListPeaks = new ArrayList<>(listPeaksOrg);
        int nbMaxLocal = trimListPeaks.size();
        
        // merge green imgMaxLocal and spineDendFill blue -> imageMerge + red for removed max local on green and red channels put brightness contrast set 0 - 1
        for (int s = 1; s <= imgshow.getNSlices(); s++) {
            imgshow.setZ(s);
            imgshow.getProcessor().setMinAndMax(0, 1);
            maxLocalStack.setZ(s);
            maxLocalStack.getProcessor().setMinAndMax(0, 1);
        }

        // create image stack
        ImagePlus[] imageArray = {imgshow, maxLocalStack, imageOrg};
        imgMaxMerge = makeComposite(imageArray, imageOrg.getTitle() + "(Max local merge)");
        
        //Zposition = Math.round(imgMaxMerge.getNSlices() / 2);
        // enhance contrast for blue channel 
        imgMaxMerge.setC(3);
        imgMaxMerge.setLut(LUT.createLutFromColor(Color.WHITE));
        imgMaxMerge.setZ(imgMaxMerge.getNSlices()/2);
        IJ.run(imgMaxMerge, "Enhance Contrast", "saturated=0.15 normalize");
        
        imgMaxMerge.updateAndDraw();
        imgMaxMerge.setCalibration(cal0);
        imgMaxMerge.setC(1);
        imgMaxMerge.show();
        
        // create image max projection
        for (int c = 0; c < imageArray.length; c++) {
            ZProjector proj = new ZProjector(imageArray[c]);
            proj.setMethod(ZProjector.MAX_METHOD);
            proj.doProjection();
            imageArray[c] = proj.getProjection();
            if ((c == 0) || (c == 1)) {
                imageArray[c].getProcessor().setMinAndMax(0, 1);
            }
        }
        double maxPixelvalue = imageArray[2].getProcessor().getStats().max;
        imageArray[2].getProcessor().setMinAndMax(0, maxPixelvalue);
        
        imgMaxProj = makeComposite(imageArray, imageOrg.getTitle() + "(Max local projection)");
        updateListViews();

        // enhance contrast for blue channel
//        imgMaxProj.setC(3);
//        IJ.run(imgMaxProj, "Enhance Contrast", "saturated=0.35 normalize");
        imgMaxProj.setC(2);
        imgMaxProj.setCalibration(cal0);
        imgMaxProj.updateAndDraw();
        imgMaxProj.show();
        imgMaxMerge.setDisplayRange(0, maxPixelvalue, 3);
//        maxLocalStack.close();
        // compute image map from spineDendFill
        // binarize dendrite mask and  maxLocalStack
        // compute distances from max local to dendrite
        // find min and max distance to show in dialog box
        adaptZoom(370, 350);
    }
    
    /**
     * Check if the file exists
     * @param path
     * @return 
     */
    private boolean fileExists(String path){
        File fileSWC;
        fileSWC = new File(path);
        return fileSWC.exists();
    }
    
    private Calibration checkCalibration(){
        Calibration cal = new Calibration();
        GenericDialog calDial = new GenericDialog("Adjust the Calibration");
        calDial.addNumericField("XY size", 100, 2, 6, "nm");
        calDial.addNumericField("Z size", 300, 2, 6, "nm");
        calDial.showDialog();
        double xy = calDial.getNextNumber();
        double z = calDial.getNextNumber();
        cal.setUnit("nm");
        cal.pixelHeight = xy;
        cal.pixelWidth = xy;
        cal.pixelDepth = z;
        
        return cal;
    }
    
    
    public void setPaths(boolean straightNeck){
        List<Spine> list = convertPoint3DToSpines(getCenters(getHeadsInt()));
        setListSpines(list);
        computeSpinePath(straightNeck);
        
        if(!straightNeck){
            Path_Dial openDial = new Path_Dial(this);
            openDial.setVisible(true);
        }
    }
    
    public void computeSpinePath(boolean straightNeck){
        
        imgMaxMerge.setC(1);
        newSpines = new ArrayList<>();
        
        //sort
        heads.getObjects3DInt().sort(new Comparator<Object3DInt>() {
            @Override
            public int compare(Object3DInt p1, Object3DInt p2) {
                return (int)(p1.getLabel() - p2.getLabel());
            }
        });
        
        heads.getObjects3DInt().stream().forEach(hd -> {
                Measure2Distance msD = new Measure2Distance(hd, dendShape);
                VoxelInt d = msD.getBorder1Pix();
                VoxelInt d2 = new VoxelInt(90, 48, 7, 9);
                float dIntensity = imaOrgHand.getPixel(d.getX(), d.getY(), d.getZ());
                Spine s = new Spine(d.getX(), d.getY(), d.getZ(), dIntensity);
                
//                float cIntensity = imaOrgHand.getPixel(c.getX(), c.getY(), c.getZ());
//                Spine s = new Spine(c.getX(), c.getY(), c.getZ(), cIntensity);
                s.setHead(hd);
                s.setHeadLab(hd.getLabel());
                newSpines.add(s);
        });
//        
//        imageOrg.setCalibration(cal1);
//        dendriteShape.setCalibration(cal1);
        
        //multichannel output
        ImagePlus tempNeckStack = NewImage.createImage("local_stack", imageOrg.getWidth(), imageOrg.getHeight(), imageOrg.getNSlices(), 16, NewImage.FILL_BLACK);
        IJ.run(tempNeckStack, "3-3-2 RGB", "");
        tempNeckStack.setDisplayRange(0, heads.getNbObjects()*10+5);
        ImagePlus tempObjStack = NewImage.createImage("local_stack", imageOrg.getWidth(), imageOrg.getHeight(), imageOrg.getNSlices(), 16, NewImage.FILL_BLACK);
        IJ.run(tempObjStack, "3-3-2 RGB", "");
        tempObjStack.setDisplayRange(0, heads.getNbObjects()*10);
        
        //for Straight Neck, uniform values
        ImagePlus ivoid = imageOrg.duplicate();
        ImageHandler ihvoid = ImageHandler.wrap(ivoid);
        ihvoid.fill(10);
        
        newSpines.forEach(spine -> {
                    if(straightNeck){
                        ImagePlus dupli = ivoid.duplicate();
                        dupli.setCalibration(cal1);
                        AutoTrace2 autoTrace = new AutoTrace2(dupli);
                        //spine.computeFull(dendShape, autoTrace, cal0.pixelWidth, cal0.pixelDepth);
                        spine.computeFull(dendShape, autoTrace, 1, 1);
                        spine.drawPathDendrite(tempNeckStack, (int)((spine.getHeadLabel())*10+5));
                        spine.getHead().drawObject(ImageHandler.wrap(tempObjStack), spine.getHeadLabel()*10);
                        dupli.close();
                    }
                    else{
                        ImagePlus dupli = imageOrg.duplicate();
                        dupli.setCalibration(cal1);
                        AutoTrace2 autoTrace = new AutoTrace2(dupli);
//                        spine.computeFull(dendShape, autoTrace, cal0.pixelWidth, cal0.pixelDepth);
                        spine.computeFull(dendShape, autoTrace, cal0.pixelWidth, cal0.pixelDepth);
                        spine.drawPathDendrite(tempNeckStack, (int)((spine.getHeadLabel())*10+5));
                        spine.getHead().drawObject(ImageHandler.wrap(tempObjStack), spine.getHeadLabel()*10);
                        dupli.close();
                    }
        });
        
        ivoid.close();
        
        dendShape.drawObject(ImageHandler.wrap(dendriteShape), 1);
        dendriteShape.setDisplayRange(0, 2);
        
        //multi-output
        ImagePlus[] imageArray = {tempObjStack, tempNeckStack, dendriteShape, imageOrg};
        imaResult = makeComposite(imageArray, imageOrg.getTitle() + "(heads-necks-dendrite-original)");
        imaResult.setCalibration(cal0);
        imaResult.show();
        
        imgMaxMerge.close();
        imgMaxProj.close();
        imgshow.close();            //TODO close before
        dendriteShape.close();
        
        adaptZoom1Image(370, 350);
        measure3D();
    }
    
    
    public void computeSpineNoPath(){
        imgMaxMerge.setC(1);
        newSpines = new ArrayList<>();
        
        //sort
        heads.getObjects3DInt().sort((Object3DInt p1, Object3DInt p2) -> (int)(p1.getLabel() - p2.getLabel()));
        
        heads.getObjects3DInt().stream().forEach(hd -> {
                hd.getLabel();
                MeasureCentroid msc = new MeasureCentroid(hd);
                VoxelInt c = msc.getCentroidRoundedAsVoxelInt();
                Measure2Distance msD = new Measure2Distance(hd, dendShape);
                VoxelInt d = msD.getBorder1Pix();
                float dIntensity = imaOrgHand.getPixel(d.getX(), d.getY(), d.getZ());
                Spine s = new Spine(d.getX(), d.getY(), d.getZ(), dIntensity);
                s.setHead(hd);
                s.setHeadLab(hd.getLabel());
                s.computeDistances(dendShape);
                newSpines.add(s);
        });
        //IJ.log("newSp"+newSpines.size());
        
        measure3D();
    }
    
    private void measure3D() {
        if( tableResultsMeasure != null){
            tableResultsMeasure.dispose();
        }
        tableResultsMeasure = Measures.measurements3D(newSpines, imaOrgHand);
        //if (tableResultsMeasure == null) return false;
        //tableResultsMeasure.setManager(this);
        tableResultsMeasure.showFrame();

    }
    
    public void setListSpines(List<Spine> list){
        listSpines = list;
    }
    
    
    
    /**
     * 
     * @param list
     * @param x
     * @param y
     * @param z
     * @return 
     */
    public Peak3D findNearestPeak(List<Peak3D> list, int x, int y, int z){ //check if getX etc are calibrated or not

        Peak3D pk = null;
        double d, distanceMin;
        distanceMin = 10000;
        
        if (z<2){ //2D
            for (int i = 0; i < list.size(); i++){
                d = list.get(i).computeDistanceToPt2D(x, y);
                if (d < distanceMin) {
                    distanceMin = d;
                    pk = list.get(i);
                }
            }
        }else{ //3D
            for (int i = 0; i < list.size(); i++){
                d = list.get(i).computeDistanceToPt(x, y, z);
                if (d < distanceMin) {
                    distanceMin = d;
                    pk = list.get(i);
                }
            }
        }
        
        return pk;
    }
    
    public List<Peak3D> findNearestPeaks(List<Peak3D> list, int x, int y, int z){
        //Find the closest peak
        Peak3D nearest = findNearestPeak(list, x, y, z);
        KDTreeC kd = new KDTreeC(3);
        kd.setScale2(1, 1, 1);
        list.stream().forEach(p ->{
            kd.add(Arrays.copyOf(p.toArray(), 3), 1);
        });
        
        double[] coord = {(double) x, (double) y, (double) z};
//        KDPoint[] near = kd.getNearestNeighbors(coord, 3);
        
        //Find if cluster
        List<Peak3D> listpeaks = findNearestPks(list, nearest);
        List listResult = new ArrayList();
        if( listpeaks==null){
//            IJ.log("no cluster");
            listResult.add(nearest);
        }else{
            trimListPeaks.stream().forEach(s ->{
                if (listpeaks.contains(s)) {
                    listResult.add(s);
                }
            });
        }
        
        return listResult;
    }
    
    
    public List<Peak3D> findNearestPks(List<Peak3D> list, Peak3D nearest){
        
        List<Peak3D> peaks = new ArrayList<>();
        list.stream().forEach((Peak3D p) -> {
            peaks.add(p);
        });
        
        DBSCAN db = new DBSCAN(peaks, 2, 5);
        List<List<Peak3D>> cluster = db.performClustering();
        Optional<List<Peak3D>> filteredList = cluster.stream()
                .filter(l -> l.contains(nearest))
                .findFirst();
        
        //convert to List
        List<Peak3D> result = null;
        if (filteredList.isPresent()) {
            result = filteredList.get();
        }
        return result;
    }
    
//    private static List<double[]> convertToArray(List<Spine> list){
//        ArrayList<double[]> coords = new ArrayList<>();
//        list.stream().forEach(pk ->{
//                coords.add(pk.getPoint().getArray());
//                });
//        return coords;
//    }
    
    /**
     * Remove Peak from list
     * @param list 
     * @param sp 
     */
    public void removePeak(List<Peak3D> list, Peak3D sp){
        list.remove(sp);
    }
    
    public void removePeakInLists(Peak3D sp){
        listPeaksOrg.remove(sp);
        trimListPeaks.remove(sp);
    }
    
    private boolean isPeakInList(Peak3D pk){
        return trimListPeaks.stream().anyMatch(p -> p.equals(pk));
    }
    
    /**
     * Add Peak3D in both Lists
     * @param sp 
     */
    public void addPeakinLists(Peak3D pk){
//        IJ.log(""+listPeaksOrg.size()+" sp:"+sp.x+" "+sp.y+" "+sp.z);
        if (!isPeakInList(pk)) {
//            Object3DInt dendriteObject = new Object3DInt(ImageHandler.wrap(dendriteShape));
            //setClosest & setDistance
//            pk.computeClosestDistance(dendriteObject);
            pk.setDistToDendrite(ImageHandler.wrap(EDT).getPixel((int) pk.x, (int) pk.y, (int) pk.z));
            listPeaksOrg.add(pk);
            trimListPeaks.add(pk);
        }
        //else
        //    IJ.showMessage("Error", "Point is already set, adjust intensity and/or distance to add it");
        
    }
    

    
    /**
     * Filter shown list with intensity and distance (from dendrite)
     * @param currThres min intensity value
     * @param currMinDist min distance from dendrite
     * @param currMaxDist max distance from dendrite
     */
    public void selectedSpines(double currThres, double currMinDist, double currMaxDist){
        PeaksList list1 = new PeaksList();
        list1.wrapList(listPeaksOrg);
        trimListPeaks = list1.filterPeaks(currThres, currMinDist, currMaxDist);
    }
    
    public void updateNeck(VoxelInt headBorder){
        //IJ.log(" headborder"+headBorder.getValue());
        ImagePlus[] channels = ChannelSplitter.split(imaResult);
        newSpines.forEach(spine -> {
            if(spine.getHead().getLabel() == headBorder.getValue()){
                
                //undraw & delete old Path 
                spine.erasePathInImage(channels[1]); //c1 = neck channel
                spine.deleteNeck();
                
                //get and draw new path
                ImagePlus dupli = imageOrg.duplicate();
                dupli.setCalibration(cal1);
                AutoTrace2 autoTrace = new AutoTrace2(dupli);
//                spine.computePathDendrite(autoTrace, headBorder, 1, 1);
//                spine.getPath().shortenPath(spine.head);
                spine.updatePath(autoTrace, headBorder, dendShape, cal0.pixelWidth, cal0.pixelDepth);
                //IJ.log("draw... z0:"+spine.path.getList().get(0).z/cal0.pixelDepth+" z1:"+spine.path.getList().get(1).z/cal0.pixelDepth);
                spine.drawPathDendriteCalib(channels[1], (int)((spine.getHeadLabel())*10+5));
                dupli.close();
            }
        });
        channels[1].updateAndDraw();
        ImagePlus imaRes = makeComposite(channels, imageOrg.getTitle() + "(heads-necks-dend-original)");
        imaResult.setImage(imaRes);
        
        //update table
        measure3D();
        
    }
    
    public void deleteNeck(Object3DInt head){
        ImagePlus[] channels = ChannelSplitter.split(imaResult);
        newSpines.forEach(spine -> {
            if(spine.getHead().equals(head)){
                //undraw
                spine.erasePathInImage(channels[1]);
                //delete
                spine.deleteNeck();
                spine.computeDistances(dendShape);
            }
        });
        channels[1].updateAndDraw();
        ImagePlus imaRes = makeComposite(channels, imageOrg.getTitle() + "(heads-necks-dend-original)");
        imaResult.setImage(imaRes);
        imaResult.setC(2);
        imaResult.setSlice(6);
        
        //update table
        measure3D();
    }
    
    /**
     * Peaks on 3 channels merged image
     */
    public void updateListViews(){
        //ImageStack z0 inclue
        //ImagePlus setz and setPosition -> one based
        
        int ch = imgMaxMerge.getC();
        int sl = imgMaxMerge.getZ();
        
        //get Spines not selected in trimListPeaks
        List<Peak3D> excluded = listPeaksOrg.stream()
                .filter(s1 -> (trimListPeaks.stream().filter(s2 -> s2.equals(s1)).count()) < 1).collect(Collectors.toList());
        
        
        //apply colors green to trimList, excluded in red
        imgMaxMerge.setC(2);
        listPeaksOrg.stream().forEach(max -> {
                    imgMaxMerge.setZ((int) max.z+1);
                    imgMaxMerge.getProcessor().set((int) max.x, (int) max.y, 0);
                    });
        trimListPeaks.stream().forEach(max -> {
                    imgMaxMerge.setZ((int) max.z+1);
                    imgMaxMerge.getProcessor().set((int) max.x, (int) max.y, (int) max.getValue());
                    });
//        imgMaxMerge.updateAndDraw();
        imgMaxMerge.setC(1);
        listPeaksOrg.stream().forEach(max -> {
                    imgMaxMerge.setZ((int) max.z+1);
                    imgMaxMerge.getProcessor().set((int) max.x, (int) max.y, 0);
                    });
        excluded.stream().forEach(max -> {
                    imgMaxMerge.setZ((int) max.z+1);
                    imgMaxMerge.getProcessor().set((int) max.x, (int) max.y, max16);
                    });
        
        imgMaxMerge.setC(ch);
        imgMaxMerge.updateAndDraw();
        
        //same in proj
        imgMaxProj.setC(2);
        listPeaksOrg.stream().forEach(max -> imgMaxProj.getProcessor().set((int) max.x, (int) max.y, 0));
        trimListPeaks.stream().forEach(max -> imgMaxProj.getProcessor().set((int) max.x, (int) max.y, (int) max.getValue()));
        
        imgMaxProj.setC(1);
        listPeaksOrg.stream().forEach(max -> imgMaxProj.getProcessor().set((int) max.x, (int) max.y, 0));
        excluded.stream().forEach(max -> imgMaxProj.getProcessor().set((int) max.x, (int) max.y, max16));
        imgMaxProj.updateAndDraw();
        imgMaxProj.setC(ch);
        imgMaxMerge.setZ(sl);
        
    }
    
    /**
     * AdaptZoom on images to be seen correctly depending of a frame dialogbox
     * Supposing that this frame is located at 0,0.
     * @param frameWidth
     * @param frameHeight
     */
    public void adaptZoom1Image(int frameWidth, int frameHeight) {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        ImageWindow iwm = imaResult.getWindow();
        int imageWidth = imaResult.getWidth();
        int edgeW = iwm.getWidth() - imageWidth;
        int widthAvailable = (int)(screenSize.getWidth() - frameWidth);
        
//        int heightAvailable = (int)(screenSize.getHeight()- this.frame.getHeight()- this.frame.getY());
        double zoomFactor = 3.5;
        //Set a screen arround divided by 4, avoiding zooming to much and let so space
        while (zoomFactor*iwm.getWidth() > widthAvailable / 3){
            zoomFactor = zoomFactor - 0.25;
        }
        //if (zoomFactor < 1) zoomFactor = 1;
        
        if (iwm.getWidth() < widthAvailable/3){
            //locX, locY, width, height -> frame+frameLocX, idemY, ima*zoom-edgeSizeX, ima*zoom-edgeSizeY)
            iwm.setLocationAndSize(frameWidth + 20, frameHeight / 2, (int) (imageWidth * zoomFactor + edgeW), (int) (imaResult.getHeight() * zoomFactor + (iwm.getHeight() - imaResult.getHeight())) );
            }
        iwm.toFront();
    }
    
    /**
     * AdaptZoom on images to be seen correctly depending of a frame dialogbox
     * Supposing that this frame is located at 0,0.
     * @param frameWidth
     * @param frameHeight
     */
    public void adaptZoom(int frameWidth, int frameHeight) {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        ImageWindow iwm = imgMaxMerge.getWindow();
        int imageWidth = imgMaxMerge.getWidth();
        int edgeW = iwm.getWidth() - imageWidth;
        ImageWindow iwp = imgMaxProj.getWindow();
        int widthAvailable = (int)(screenSize.getWidth() - frameWidth);
        
//        int heightAvailable = (int)(screenSize.getHeight()- this.frame.getHeight()- this.frame.getY());
        double zoomFactor = 3.5;
        //Set a screen arround divided by 4, avoiding zooming to much and let so space
        while (zoomFactor*iwm.getWidth() > widthAvailable / 3){
            zoomFactor = zoomFactor - 0.25;
        }
        //if (zoomFactor < 1) zoomFactor = 1;
        
        if (iwm.getWidth() < widthAvailable/3){
            //locX, locY, width, height -> frame+frameLocX, idemY, ima*zoom-edgeSizeX, ima*zoom-edgeSizeY)
            iwm.setLocationAndSize(frameWidth + 20, frameHeight / 2, (int) (imageWidth * zoomFactor + edgeW), (int) (imgMaxMerge.getHeight() * zoomFactor + (iwm.getHeight() - imgMaxMerge.getHeight())) );
            iwp.setLocationAndSize(frameWidth + 10 + widthAvailable / 3, frameHeight / 2, (int) (imgMaxProj.getWidth() * zoomFactor + iwp.getWidth() - imgMaxProj.getWidth()), (int) (imgMaxProj.getHeight() * zoomFactor + (iwp.getHeight() - imgMaxProj.getHeight())) );
        }
        iwp.toFront();
    }
    
    /**
     * Return the max intensity in the list
     * @param list
     * @return 
     */
    public double getMaxValue (List<Peak3D> list){
        return list.stream()
                    .max(Comparator.comparing(Peak3D::getValue))
                    .get().getValue();
    }
    
    /**
     * Return the max distance in the list
     * @param list
     * @return 
     */
    public double getMaxDist (List<Spine> list){
        return list.stream()
                    .max(Comparator.comparing(Spine::getDistDendrite))
                    .get().getDistDendrite();
    }
    
    /**
     * Return the min distance in the list
     * @param list
     * @return 
     */
    public double getMinDist (List<Spine> list){
        return list.stream()
                    .min(Comparator.comparing(Spine::getDistDendrite))
                    .get().getDistDendrite();
    }
    
    
    /**
     * Find the max local in the local area centered on (xp,yp) on all z slices
     * @param xp x coord center
     * @param yp y coord center
     * @param img imagePlus
     * @return Voxel coord & value
     */
    @Deprecated
    public Spine findPeakInArea(int xp, int yp, ImagePlus img) {
        double maxInt = img.getProcessor().getMin();
        Spine sp = new Spine(0, 0, 0, 0);
        
        // scan all z in image for pixel max intensity arround 3x3
        for (int y = yp - 1; y <= yp + 1; y++) {
            for (int x = xp - 1; x <= xp + 1; x++) {
                for (int z = 1; z <= img.getNSlices(); z++) {
                   img.setPositionWithoutUpdate(1, z, 1);
                    if (img.getProcessor().getPixel(x, y) > maxInt) {
                        maxInt = img.getProcessor().getPixel(x, y);
                        sp.setX(x);
                        sp.setY(y);
                        sp.setZ(z-1);
                        sp.setValue((int)maxInt);
                    }
                }
            }
        }
        
        return (sp);
    }
    

    /**
     * merge channels in composite image
     * @param imgs
     * @param title
     * @return 
     */
    public ImagePlus makeComposite(ImagePlus[] imgs, String title) {
        ImagePlus img = RGBStackMerge.mergeChannels(imgs, true);
        img.setTitle(title);
        //img.setCalibration(cal);
        return (img);
    }

    private boolean dialog() {
        GenericDialog dialog = new GenericDialog("SPOT SPINE");
        dialog.addMessage("For a faster peak detection, adjust these values");
        dialog.addNumericField("Threshold intensity", thrsIntVal);
        dialog.addNumericField("Minimum distance to dendrite", minDist);
        dialog.addNumericField("Maximum distance to dendrite", maxDist);
        dialog.showDialog();
        thrsIntVal = dialog.getNextNumber();
        minDist = dialog.getNextNumber();
        maxDist = dialog.getNextNumber();
        
        return dialog.wasOKed();
    }
    
    /**
     * 
     * @param local_method 0 Constant, 1 Mean, 2 GaussFit
     * @param spot_method 0 Classical, 1 Max, 2 Block
     * @param watershed 
     * @param methodRad radius or max radius
     * @param methValue Thresh for Constant, Weight for Mean, GaussPC for Gaussian
     */
    public void Segmentation(int local_method, int spot_method, boolean watershed, int methodRad, float methValue) {
        IJ.showStatus("Computing...");
        //init images
        int currZ = imgMaxMerge.getSlice();
        ImageHandler imOrigin = ImageHandler.wrap(imageOrg.duplicate());
        dendShape.drawObject(imOrigin, 0); //exclude dendrite from drawn spines
//        imgMaxMerge.setCalibration(cal0);
        imgMaxMerge.setC(2);
        ImageHandler imSeeds = ImageHandler.newBlankImageHandler("seeds", imOrigin);
        trimListPeaks.stream().forEach(max -> {
                    imSeeds.setPixel((int) max.x, (int) max.y, (int) max.z, (int) max.getValue());
                    });
        Segment3DSpots seg = new Segment3DSpots(imOrigin, imSeeds);
        
        // set parameters
        seg.setSeedsThreshold(1);//Global background 
//        seg.setSeedsThreshold((int) thrsIntVal);
        seg.setUseWatershed(watershed);
        seg.setVolumeMin(1);
        seg.setVolumeMax(10000);

        // create thresholder
        LocalThresholder localThresholder;
        switch (local_method) {
            case 1:
                localThresholder = new LocalThresholderMean(methodRad, methodRad, methodRad, methValue);
                break;
            case 2:
                localThresholder = new LocalThresholderGaussFit(methodRad, methValue);
                break;
            default:
                localThresholder = new LocalThresholderConstant(methValue);
                break;
        }
        // create spot segmenter
        SpotSegmenter spotSegmenter;
        switch (spot_method) {
            case 1:
                spotSegmenter = new SpotSegmenterMax();
                break;
            case 2:
                spotSegmenter = new SpotSegmenterBlock();
                break;
            default:
                spotSegmenter = new SpotSegmenterClassical();
                break;
        }
//        segment
        seg.setLocalThresholder(localThresholder);
        seg.setSpotSegmenter(spotSegmenter);
        seg.segmentAll();
        ImageHandler lab = seg.getLabeledImage();
        
        heads = new Objects3DIntPopulation(lab);
        
        //Adjust display
        ImagePlus imObj = lab.getImagePlus();
        for (int s = 1; s <= imObj.getNSlices(); s++) {
            imObj.setZ(s);
            imObj.getProcessor().setMinAndMax(0, trimListPeaks.size()+1);//+1 for transparency
        }
//        ImagePlus[] imageArray = {imObj, imSeeds.getImagePlus(), imageOrg};
        ImagePlus[] imageArray = {imObj, imageOrg};
        ImagePlus temp = makeComposite(imageArray, imageOrg.getTitle() + "segmentation merge");
        imgMaxMerge.setImage(temp);
        imgMaxMerge.setCalibration(cal0);
        imgMaxMerge.setC(1);
        imgMaxMerge.setZ(currZ);
        
        IJ.log("nb image:"+imageArray.length);
        
        // create image max projection
        ImagePlus[] imageArrayProj = new ImagePlus[imageArray.length];
        for (int c = 0; c < imageArray.length; c++) {
            ZProjector proj = new ZProjector(imageArray[c]);
            proj.setMethod(ZProjector.MAX_METHOD);
            proj.doProjection();
            imageArrayProj[c] = proj.getProjection();
            if ((c == 0) || (c == 1)) {
                imageArrayProj[c].getProcessor().setMinAndMax(0, trimListPeaks.size()+1); //+1 for transparency
            }
        }
        
//        ZProjector proj2 = new ZProjector(imgMaxMerge);
//        proj2.setMethod(ZProjector.MAX_METHOD);
//        proj2.doProjection();
//        ImagePlus temp3 = proj2.getProjection();
        
        double maxPixelvalue = imageArray[1].getProcessor().getStats().max;
        imageArray[1].getProcessor().setMinAndMax(0, maxPixelvalue);
        IJ.log("imgMaxproj: "+imgMaxProj.getTitle());
        
        ImagePlus temp2 = makeComposite(imageArray, imageOrg.getTitle() + "(Max local projection)");
        IJ.log(" temp2: "+temp2.getTitle());
        imgMaxProj.setImage(temp2);
        imgMaxProj.setCalibration(cal0);
        //adaptZoom(370, 350);
        
    }
    
    /**
     * Segment image to add one object at seed Peak3D localisation to head population, then update
     * @param peak seed to add
     * @param local_method
     * @param spot_method
     * @param watershed
     * @param methodRad
     * @param methValue 
     */
    public void Segmentation1Peak(Peak3D peak, int local_method, int spot_method, boolean watershed, int methodRad, float methValue) {
        int currZ = imgMaxMerge.getSlice();
        ImageHandler imOrigin = ImageHandler.wrap(this.imageOrg.duplicate());
        dendShape.drawObject(imOrigin, 0); //exclude dendrite from drawn spines
//        imgMaxMerge.setCalibration(cal0);
        imgMaxMerge.setC(2);
        ImageHandler imSeeds = ImageHandler.newBlankImageHandler("seeds", imOrigin);
        imSeeds.setPixel((int) peak.x, (int) peak.y, (int) peak.z, (int) peak.getValue());
        Segment3DSpots seg = new Segment3DSpots(imOrigin, imSeeds);
        // set parameters
        seg.setSeedsThreshold(1);//Global background 
//        seg.setSeedsThreshold((int) thrsIntVal);
        seg.setUseWatershed(watershed);
        seg.setVolumeMin(1);
        seg.setVolumeMax(10000);

        // create thresholder
        LocalThresholder localThresholder;
        switch (local_method) {
            case 1:
                localThresholder = new LocalThresholderMean(methodRad, methodRad, methodRad, methValue);
                break;
            case 2:
                localThresholder = new LocalThresholderGaussFit(methodRad, methValue);
                break;
            default:
                localThresholder = new LocalThresholderConstant(methValue);
                break;
        }
        // create spot segmenter
        SpotSegmenter spotSegmenter;
        switch (spot_method) {
            case 1:
                spotSegmenter = new SpotSegmenterMax();
                break;
            case 2:
                spotSegmenter = new SpotSegmenterBlock();
                break;
            default:
                spotSegmenter = new SpotSegmenterClassical();
                break;
        }
        
        // segment
        seg.setLocalThresholder(localThresholder);
        seg.setSpotSegmenter(spotSegmenter);
        seg.segmentAll();
        ImageHandler lab = seg.getLabeledImage();
        
        Objects3DIntPopulation pop = new Objects3DIntPopulation(lab);
        heads.addObject(pop.getFirstObject(), heads.getMaxLabel()+1);
        heads.drawInImage(lab);
        lab.getImagePlus().getProcessor().setMinAndMax(0, heads.getMaxLabel());
        
        //Adjust display
        ImagePlus imObj = lab.getImagePlus();
        
        ImagePlus[] imageArray = {imObj, imageOrg};
        ImagePlus temp = makeComposite(imageArray, imageOrg.getTitle() + "segmentation merge");
        imgMaxMerge.setImage(temp);
        imgMaxMerge.setCalibration(cal0);
        imgMaxMerge.setC(1);
        imgMaxMerge.setZ(currZ);
        
        // create image max projection
        for (int c = 0; c < imageArray.length; c++) {
            ZProjector proj = new ZProjector(imageArray[c]);
            proj.setMethod(ZProjector.MAX_METHOD);
            proj.doProjection();
            imageArray[c] = proj.getProjection();
            if ((c == 0) || (c == 1)) {
                imageArray[c].getProcessor().setMinAndMax(0, heads.getMaxLabel()+1); //+1 for transparency
            }
        }
        double maxPixelvalue = imageArray[1].getProcessor().getStats().max;
        imageArray[1].getProcessor().setMinAndMax(0, maxPixelvalue);
        
        ImagePlus temp2 = makeComposite(imageArray, imageOrg.getTitle() + "(Max local projection)");
        imgMaxProj.setImage(temp2);
        imgMaxProj.setCalibration(cal0);
        adaptZoom(370, 350);
        
    }
    
//    public Objects3DPopulation getHeads(){
//        return this.headsOld;
//    }
    
    public Objects3DIntPopulation getHeadsInt(){
        return this.heads;
    }
    
    public List<Point3D> getCenters (Objects3DIntPopulation pop){
        List<Point3D> list = new ArrayList<>();
        pop.getObjects3DInt().stream().forEach(obj -> {
            MeasureCentroid cent = new MeasureCentroid(obj);
            Point3D p = cent.getCentroidAsPoint();
            list.add(p);
            });
        return list;
    }
    
//    public List<Point3D> getCenters (Objects3DPopulation pop){
//        List<Point3D> list = new ArrayList<>();
//        pop.getObjectsList().stream().forEach(obj -> {
//            Point3D p = obj.getCenterAsPoint();
//            list.add(p);
//            });
//        return list;
//    }
    
    /**
     * 
     * @param list peaks
     * @return 
     */
    public List<Spine> convertPoint3DToSpines (List<Point3D> list){
//        ImageHandler ih = ImageHandler.wrap(imRef);
        List<Spine> spines = new ArrayList<>();
        //create Spine with value
        list.stream().forEach(p -> {
            Spine sp = new Spine(p.getRoundX(), p.getRoundY(), p.getRoundZ(), imaOrgHand.getPixel(p.getRoundX(), p.getRoundY(), p.getRoundZ()));
            spines.add(sp);
            });
        //add a distance from dendrite
        Object3DInt dendriteObject = new Object3DInt(ImageHandler.wrap(dendriteShape));
        spines.stream().forEach(sp -> { //setClosest & setDistance
            sp.computeClosestDistance(dendriteObject);
            sp.setDistDendrite(ImageHandler.wrap(EDT).getPixel(sp.getX(), sp.getY(), sp.getZ()));
            });
        
        return spines;
    }
    
        /**
     * Find the max local in the local area centered on (xp,yp,zp)
     * full Z if zp = 0
     * @param xp x coord 
     * @param yp y coord
     * @param zp z coord 0-based
     * @return Voxel coord & value without distance
     */
    public Peak3D findMax(int xp, int yp, int zp) {
        double maxInt = 0; //img.getProcessor().getMin();
        Peak3D V = new Peak3D((double) xp, (double) yp, (double) zp-1, (double) 0);
        int zBegin = 1;
        int zEnd = imageOrg.getNSlices();
        if (zp > 1){// scan z in image for pixel max intensity arround 5x5x3
            zBegin = zp; //1-based
            zEnd = zp+2; //1-based
        }
        for (int z = zBegin; z <= zEnd; z++) {
            for (int y = yp - 2; y <= yp + 2; y++) {
                for (int x = xp - 2; x <= xp + 2; x++) {
                    if(z > 0 || z <= imageOrg.getNSlices()){ //upper & lower edges
                        imageOrg.setZ(z);//PositionWithoutUpdate(1, z, 1);//1-based
                        if (imageOrg.getProcessor().getPixel(x, y) > maxInt) {
                            maxInt = imageOrg.getProcessor().getPixel(x, y);
                            V.x = x;
                            V.y = y;
                            V.z = z-1;//0-based
                            V.setValue((float)maxInt);
                        }
                    }
                }
            }
        }
        return (V);
    }
    
    /**
     * Add Peak from segFrame window
     * @param xp
     * @param yp
     * @param zp
     * @param img 
     */
    public Peak3D addPeak(int xp, int yp, int zp, ImagePlus img){
        Peak3D s = findMax(xp, yp, zp);//, img);
        addPeakinLists(s);
        return s;
    }
    
    
    public void deleteObject (float objLabel){
        //getPoints in obj vol to remove in listPeaksOrg and trimListPeaks
//        heads.getObjectByLabel(objLabel).drawObject(handler, 0);
        heads.removeObject(objLabel);
    }
    
    public void removeObjects(List Labels){
        Labels.stream().forEach(obj -> heads.removeObject((float)obj));
    }
    
    @Deprecated
    public void removeObj(List<Float> Labels){
//        IJ.log("before removing trim:"+trimListPeaks.size()+" heads:"+heads.getNbObjects());
        //imgMaxMerge.
        ImagePlus[] imgChannel = ChannelSplitter.split(imgMaxMerge);
        ImageHandler ihand = ImageHandler.wrap(imgChannel[0]);
        Labels.stream().forEach(obj -> {
//            Object3D ob = headsOld.getObjectByValue(obj.intValue());
            Object3DInt oInt = heads.getObjectByLabel(obj);
            oInt.getObject3DPlanes().stream().forEach(plan ->{
                plan.drawObject(ihand, 0);
            });
            heads.removeObject(oInt);
            //remove peaks inside obj
//            List del = new ArrayList<Spine>();
//            trimListPeaks.stream().forEach(p -> {
//                if (ob.inside(p.x, p.y, p.z)){
//                    del.add(p);
//                }
//            });
//            trimListPeaks.removeAll(del);
            //remove obj
//            ob.draw(ihand, 0);
//            ihand.updateDisplay();
//            headsOld.removeObject(ob);
                });
        heads.getObjects3DInt().sort((Object3DInt p1, Object3DInt p2) -> (int)(p1.getLabel() - p2.getLabel()));
        heads.drawInImage(ihand);
        ihand.updateDisplay();
        ImagePlus temp = makeComposite(imgChannel, imgMaxMerge.getTitle()); //imgChannel is linked to ihand
        imgMaxMerge.setImage(temp);
        imgMaxMerge.updateAndDraw();
        
        // create image max projection
        for (int c = 0; c < imgChannel.length; c++) {
            ZProjector proj = new ZProjector(imgChannel[c]);
            proj.setMethod(ZProjector.MAX_METHOD);
            proj.doProjection();
            imgChannel[c] = proj.getProjection();
        }
        
        ImagePlus temp2 = makeComposite(imgChannel, imageOrg.getTitle() + "(Max local projection)");
        imgMaxProj.setImage(temp2);
        imgMaxProj.setCalibration(cal0);
        
//        IJ.log("after removing trim:"+trimListPeaks.size()+" heads:"+heads.getNbObjects());
    }
    
    /**
     * 
     * @param Labels
     * @param Changing 0 remove, 1 merge, 2 nothing, 3 dilate, 4 erode
     */
    public void updateDisplayHeads(List<Float> Labels, int Changing){
        ImagePlus[] imgChannel = ChannelSplitter.split(imgMaxMerge);
        ImageHandler ihand = ImageHandler.wrap(imgChannel[0]).createSameDimensions();
        switch (Changing) {
            case 1://merge
                BoundingBox box1 = heads.getObjectByLabel(Labels.get(0)).getBoundingBox();
//                //get objs
                Object3DInt obj = heads.getObjectByLabel(Labels.get(0));
                
                Object3DInt merged = null;// = heads.getObjectByLabel(Labels.get(0));
                //IJ.log("merge av nZ:"+merged.getObject3DPlanes().size());
                //remove
                for (int i = 1; i < Labels.size(); i++) {
                    Object3DComputation merging = new Object3DComputation(obj);
                    Object3DInt addobj = heads.getObjectByLabel(Labels.get(i));
                    merged = merging.getUnion(addobj);
                    heads.removeObject(addobj);
                }
                heads.removeObject(obj);
                Object3DComputation close = new Object3DComputation(merged);//slightly fill the space between the objects 
                merged = close.getObjectClosed(1, 1, 1);
                heads.addObject(merged, Labels.get(0));
                
                //adjust upper obj values
                
                //del spines from trimListPeaks & add new center in trimListPeaks
//                findNearestPoint(trimListPeaks, x, y, z);
//                trimListPeaks
                break;
            case 2://update
                //TODO
                break;
            case 3://dilate
                for (int i = 0; i < Labels.size(); i++) {
                    obj = heads.getObjectByLabel(Labels.get(i));
                    //MeasureVolume vol = new MeasureVolume(obj);
//                    IJ.log("before obj:"+obj.getLabel()+" vol:"+vol.getVolumePix());
                    Object3DComputation compute = new Object3DComputation(obj);
                    Object3DInt obj1 = compute.getObjectDilated(1, 1, 1);
                    //MeasureVolume vol1 = new MeasureVolume(obj1);
                    heads.removeObject(obj);
                    heads.addObject(obj1);
                }
                break;
            case 4://erode
                for (int i = 0; i < Labels.size(); i++) {
                    obj = heads.getObjectByLabel(Labels.get(i));
                    //MeasureVolume vol = new MeasureVolume(obj);
                    Object3DComputation compute = new Object3DComputation(obj);
//                    obj.getObject3DPlanes().stream().forEach(plan ->{
//                        plan.drawObject(ihand, 0);
//                    });
                    Object3DInt obj1 = compute.getObjectEroded(1, 1, 1);
                    //MeasureVolume vol1 = new MeasureVolume(obj1);
//                    IJ.log("after "+obj1.getLabel()+" "+vol1.getVolumePix());
                    heads.removeObject(obj);
                    heads.addObject(obj1);
                }
                break;
            default: //0 remove
                Labels.stream().forEach(o -> {
                    //Get Obj
                    Object3DInt oInt = heads.getObjectByLabel(o);
                    //remove peaks inside obj
                    List todel = new ArrayList<Spine>();
                    trimListPeaks.stream().forEach(p -> {
                        oInt.getObject3DPlanes().stream().forEach(zplane ->{
                            if(zplane.contains(p.getVoxel3DInt())){
                                todel.add(p);
                            }
                        });
                    });
                    trimListPeaks.removeAll(todel);
                    //del obj
//                    oInt.getObject3DPlanes().stream().forEach(plan ->{
//                        plan.drawObject(ihand, 0);
//                    });
                    //remove obj
                    heads.removeObject(oInt);
                    });
                    
                break;
        }
//        ihand = ihand.newBlankImageHandler("obj", ihand);
        //heads.getObjects3DInt().sort((Object3DInt p1, Object3DInt p2) -> (int)(p1.getLabel() - p2.getLabel()));
        //heads = fillremovedObjectsPopulation(heads);
//        IJ.log("avant2 del "+heads.getNbObjects()+" v:"+ihand.getPixel(114, 36, 6));
        checkContinuityHeads();
        heads.drawInImage(ihand);
        ihand.updateDisplay();
        imgChannel[0] = ihand.getImagePlus();
        IJ.run(imgChannel[0], "Red", "");
        ImagePlus temp = makeComposite(imgChannel, imgMaxMerge.getTitle()); //imgChannel is linked to ihand
        imgMaxMerge.setImage(temp);
        imgMaxMerge.setC(1);
        imgMaxMerge.updateAndDraw();
        
        // create image max projection
        for (int c = 0; c < imgChannel.length; c++) {
            ZProjector proj = new ZProjector(imgChannel[c]);
            proj.setMethod(ZProjector.MAX_METHOD);
            proj.doProjection();
            imgChannel[c] = proj.getProjection();
        }
        
//        ImagePlus temp2 = makeComposite(imageArray, imageOrg.getTitle() + "(Max local projection)");
        ImagePlus temp2 = makeComposite(imgChannel, imageOrg.getTitle() + "(Max local projection)");
        imgMaxProj.setImage(temp2);
        imgMaxProj.setCalibration(cal0);
        ihand.closeImagePlus();
    }
    
    /**
     * 
     * @param selectedObj is > 1
     */
    public void mergeHeads(List<Float> selectedObj){
        //get objects
        Object3DInt obj = heads.getObjectByLabel(selectedObj.get(0));
        Object3DInt merged = null;
        for (int i = 1; i < selectedObj.size(); i++) {
            Object3DComputation merging = new Object3DComputation(obj);
            Object3DInt addobj = heads.getObjectByLabel(selectedObj.get(i));
            merged = merging.getUnion(addobj);
//            IJ.log(""+obj.getName());
            heads.removeObject(addobj);
        }
        heads.removeObject(obj);
        Object3DComputation close = new Object3DComputation(merged);//slightly fill the space between the objects 
        merged = close.getObjectClosed(1, 1, 1);
        heads.addObject(merged);
    }
    
    
    
    public void removeRedChannel() {
        int Z = imgMaxMerge.getZ();
        ImagePlus[] imgChannel = ChannelSplitter.split(imgMaxProj);
        // remove red channel
        imgChannel[0] = null;
        imgMaxProj.hide();
        imgMaxProj = makeComposite(imgChannel, imgMaxProj.getTitle());
        imgMaxProj.updateAndDraw();
        imgMaxProj.show();

        imgMaxMerge.hide();
        imgChannel = ChannelSplitter.split(imgMaxMerge);
        imgChannel[0] = null;
        imgMaxMerge = makeComposite(imgChannel, imgMaxMerge.getTitle());
        imgMaxMerge.updateAndDraw();
        imgMaxMerge.setZ(Z);
        imgMaxMerge.show();
    }
    
    private void checkContinuityHeads(){
        int f = 1;
        Objects3DIntPopulation newpop = new Objects3DIntPopulation();
        for (int i = 0; i < heads.getNbObjects(); i++) {
//            heads.getObjects3DInt().get(i);
//            if (heads.getObjects3DInt().get(i).getLabel() != i+1);
//                heads.getObjects3DInt().get(i).setLabel(i+1);
//                heads.getObjects3DInt().get(i).setIdObject(i+1);
//                if(i<9){
//                    heads.getObjects3DInt().get(i).setName("Obj-0"+(i+1));
//                }
//                else{
//                    heads.getObjects3DInt().get(i).setName("Obj-"+(i+1));
//                }
            
            newpop.addObject(heads.getObjects3DInt().get(i), (i+1));
            newpop.getObjects3DInt().get(i).setIdObject(i+1);
            if(i<9){
                newpop.getObjects3DInt().get(i).setName("NewObj-0"+(i+1));
            }
            else{
                newpop.getObjects3DInt().get(i).setName("NewObj-"+(i+1));
            }

        }
        heads = newpop;
        
    }
    
    /**
     * Clear the stack at this channel
     * @param plus
     * @param channel one-based
     */
    private void clearChannel(ImagePlus plus, int channel){
        plus.setC(channel);
        for (int z = 0; z < plus.getNSlices(); z++) {
//            for (int y = 0; y < plus.getHeight(); y++) {
//                for (int x = 0; x < plus.getWidth(); x++) {
                    plus.setZ(z+1);
                    plus.getProcessor().set(0);
//                }
//            }
        }
    }

}
