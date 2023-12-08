package spine_sources;

import spine_path.PathSpine;
import spine_path.AutoTrace2;
import ij.IJ;
import ij.ImagePlus;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.geom.Point3D;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.Measure2Distance;
import mcib3d.geom2.measurements.MeasureVolume;

public class Spine extends VoxelInt {
    double distDendrite;
    VoxelInt closestDendrite = null;
    PathSpine path = null;
    float headLab;
    Object3DInt head = null;
    int type;
    double[] distancesHeadDendrite;
    double[] neckDist;
    double[] btoHDist;

    public Spine(int x, int y, int z, float val) {
        super(x, y, z, val);
    }
    
    public VoxelInt getVoxelInt(){
        return new VoxelInt(this.getX(), this.getY(), this.getZ(), this.getValue());
    }

    public double getDistDendrite() {
        return distDendrite;
    }
    
    public VoxelInt getClosestDendrite() {
        return closestDendrite;
    }
    
    public float getHeadLabel(){
        return this.headLab;
    }
    
    public Object3DInt getHead(){
        return this.head;
    }
    
    
    public void setDistDendrite(double distDendrite) {
        this.distDendrite = distDendrite;
    }
    
    public void setHeadLab(float labValue){
        this.headLab = labValue;
    }
    
    public void setHead(Object3DInt obj){
        this.head = obj;
    }
    
    public Point3D getPoint(){
        Point3D p = new Point3D(this.getX(), this.getY(), this.getZ());
        return p;
    }
    
    public Peak3D getPeak(){
        Peak3D p = new Peak3D((double) this.getX(), (double) this.getY(), (double) this.getZ(), (double) this.getValue());
        return p;
    }
    
    public PathSpine getPath(){
        return path;
    }
    
    public void setPath(PathSpine newPath){
         path = newPath;
    }
    
    /**
     * Used to determine StubbySp if =0 and SpwNeck if =1
     * @param value 
     */
    private void setType(int value){
        type = value;
        head.setType(value);
    }
    
    /**
     * 0=BB_pxl, 1=BB_unit, 2=HausD_pxl, 3=HausD_unit
     * @return 
     */
    public double[] getHeadDendriteDistances(){
        return distancesHeadDendrite;
    }
    
//    public void computeSpineElements(ImagePlus plus, Object3DInt dendrite){
//        
//    }
    
    public void computeDistDendrite(Object3DInt dendrite) { //no calibration
        if (closestDendrite == null){
            closestDendrite = computeClosestVoxel(dendrite);
        }
        distDendrite = closestDendrite.distance(this);
    }
    
    
    public double computeDistToPoint(int x, int y, int z){
        int deltaZ = Math.abs(this.getZ()-z);
        if (deltaZ > 1){ //3D
            return Math.sqrt(Math.pow(this.getX() - x, 2) + Math.pow(this.getY() - y, 2) + Math.pow(this.getZ() - z, 2));
        }
        else { //2D
            return Math.sqrt(Math.pow(this.getX() - x, 2) + Math.pow(this.getY() - y, 2));
        }
    }

    /**
     * Compute and set closest VoxelInt
     * @param dendrite 
     */
    public void computeClosestDistance(Object3DInt dendrite) {
        Object3DInt spObj = new Object3DInt(new VoxelInt(this.getX(), this.getY(), this.getZ(), 1));
//        IJ.showStatus("computing distances to dendrite...");
        Measure2Distance ms = new Measure2Distance(spObj, dendrite);
        closestDendrite = ms.getBorder2Pix();
        //IJ.log("x:"+this.getX()+" y:"+this.getY()+" z:"+this.getZ()+" val:"+this.getValue()+" closestx:"+closestDendrite.getX()+" closesty:"+closestDendrite.getY()+" closestz:"+closestDendrite.getZ());
    }
    
    /**
     * 
     * @param dendrite
     * @return VoxelInt closest
     */
    public VoxelInt computeClosestVoxel(Object3DInt dendrite) {
        Object3DInt spObj = new Object3DInt(new VoxelInt(this.getX(), this.getY(), this.getZ(), 1));
        Measure2Distance ms = new Measure2Distance(spObj, dendrite);
//        VoxelInt closestD = ms.getBorder2Pix();
        //IJ.log("x:"+this.getX()+" y:"+this.getY()+" z:"+this.getZ()+" val:"+this.getValue()+" closestx:"+closestD.getX()+" closesty:"+closestD.getY()+" closestz:"+closestD.getZ());
//        return closestD;
        return ms.getBorder2Pix();
    }
    
    public void computePathDendrite(AutoTrace2 autoTrace, VoxelInt border, double calibXY, double calibZ) {
        //IJ.log("PathToDendrite x:"+this.getX()+" y:"+this.getY()+" z+1:"+(this.getZ()+1)+" val:"+this.getValue()+" dist"+this.distDendrite+" closestx:"+closestDendrite.getX()+" closesty:"+closestDendrite.getY()+" closestz+1:"+(closestDendrite.getZ()+1));
        
        this.path = autoTrace.getPath(border.getX(), border.getY(), (border.getZ()+1), closestDendrite.getX(), closestDendrite.getY(), (closestDendrite.getZ()+1));
        //IJ.log("P0:"+this.path.getList().get(0).x+" "+this.path.getList().get(0).y+" "+this.path.getList().get(0).z+" calP0:"+this.path.getList().get(0).x/calibXY+" "+this.path.getList().get(0).y/calibXY+" "+(this.path.getList().get(0).z/calibZ));
//        IJ.log("Path length : " + path.getLength() + " (" + path.getSize() + " points)"+ " myListsize"+path.getList().size());
        path.setCalibXY(calibXY);
        path.setCalibZ(calibZ);
        
    }

    public void computePathDendrite(AutoTrace2 autoTrace, double calibXY, double calibZ) {
        //IJ.log("PathToDendrite x:"+this.getX()+" y:"+this.getY()+" z+1:"+(this.getZ()+1)+" val:"+this.getValue()+" dist"+this.distDendrite+" closestx:"+closestDendrite.getX()+" closesty:"+closestDendrite.getY()+" closestz+1:"+(closestDendrite.getZ()+1));
        
        this.path = autoTrace.getPath(this.getX(), this.getY(), (this.getZ()+1), closestDendrite.getX(), closestDendrite.getY(), (closestDendrite.getZ()+1));
        //IJ.log("P0:"+this.path.getList().get(0).x+" "+this.path.getList().get(0).y+" "+this.path.getList().get(0).z+" calP0:"+this.path.getList().get(0).x/calibXY+" "+this.path.getList().get(0).y/calibXY+" "+(this.path.getList().get(0).z/calibZ));
//        IJ.log("Path length : " + path.getLength() + " (" + path.getSize() + " points)"+ " myListsize"+path.getList().size());
        path.setCalibXY(calibXY);
        path.setCalibZ(calibZ);
        
    }
    
    public void computeFull(Object3DInt dendrite, AutoTrace2 autoTrace, double calibXY, double calibZ){
//        computeClosestVoxel(dendrite);//pas trop utile due a computeDistDendrite
        computeDistDendrite(dendrite);
        computePathDendrite(autoTrace, calibXY, calibZ);
        path.neckPath(dendrite);
        adjustNeck(head);
        
        //recalculate
//        closestDendrite = this.path.IntersectionPointPath(dendrite, true);
//        computeDistDendrite(dendrite);
//        computePathDendrite(autoTrace, calibXY, calibZ);
        MeasureVolume vl = new MeasureVolume(head);
//        IJ.log(" volumeDend"+vl.getVolumePix());
        computeDistances(dendrite);
        
    }
    
    public void computeDistances(Object3DInt dendrite){
        Measure2Distance ms = new Measure2Distance(head, dendrite);
        distancesHeadDendrite = new double[4];
        distancesHeadDendrite[0] = ms.getValue("DistBorderBorderPix");
        distancesHeadDendrite[1] = ms.getValue("DistBorderBorderUnit");
        distancesHeadDendrite[2] = ms.getValue("DistHausdorffPix");       //Takes the longest probably the wrong !
        distancesHeadDendrite[3] = ms.getValue("DistHausdorffUnit");      //Takes the longest probably the wrong !
//        IJ.log("distBB: "+ms.getValue("DistBorderBorderPix")+" distHD: "+ms.getValue("DistHausdorffPix"));
        
        getHPointDist();
        if(ms.getValue("DistBorderBorderPix") > 1){
            setType(1);
        }
        neckDist = new double[2];
        if(this.path == null){
            neckDist[0] = 0;
            neckDist[1] = 0;
        }
        else{
            neckDist[1] = this.path.MeasureLength(false);//unit
            neckDist[0] = this.path.MeasureLength(true);//pxl
        }
        
    }
    
    
    public void getHPointDist() {
//        IJ.log("sizePATH:"+path.getList().size());
        btoHDist = new double[2];
        btoHDist[0] = 0;btoHDist[1] = 0;
        if( path != null && this.path.getSize() > 0 ){
//            IJ.log("x:"+path.getList().get(0).x+" y:"+path.getList().get(0).y);
            VoxelInt base = new VoxelInt((int)path.getList().get(0).x, (int)path.getList().get(0).y, (int)path.getList().get(0).z+1, 1);
            Object3DInt ob1 = new Object3DInt(base);
            Measure2Distance ms = new Measure2Distance(head, ob1);
            VoxelInt hausdorffPt = ms.getHausdorff1Pix();
            VoxelInt hausdorffPt2 = ms.getHausdorff2Pix();
    //        IJ.log("DH1 pxX:"+hausdorffPt.getX()+ " Y:"+hausdorffPt.getY()+" Z:"+hausdorffPt.getZ()+" DH2 px2X:"+hausdorffPt2.getX()+ " Y2:"+hausdorffPt2.getY()+" Z2:"+hausdorffPt2.getZ());
    //        double distHausdorff = ms.getValue("DistHausdorffPix");
            double[] result = new double[2];
            btoHDist[0] = ms.getValue("DistHausdorffPix");
            btoHDist[1] = ms.getValue("DistHausdorffUnit");
        }
        
    }
    
    /**
     * Reduce the path to the dendrite, add to the upper of head
     * @param autoTrace
     * @param head begin
     * @param dendriteObj end
     */
    public void adjustPath(Object3DInt head, Object3DInt dendriteObj){
        path.shortenPath(dendriteObj);
        
        VoxelInt pointOnPath = path.intersectionPointPath(head, false);
        VoxelInt upper = null;
        Measure2Distance md = new Measure2Distance(head, dendriteObj);
    }
    
    
    public void adjustNeck(Object3DInt head){
        path.createPath();
        path.neckPath(head);
//        if(path.getSize()>0){
        //getHPointDist();
//        }
    }
    
    public void deleteNeck(){
        path = null;
    }
    
    public boolean isTouchingDendrite(Object3DInt head, Object3DInt dendriteObj){
        Measure2Distance bb = new Measure2Distance(head, dendriteObj);
//        IJ.log("distance:"+bb.getValue("DistBorderBorderPix"));
        return bb.getValue("DistBorderBorderPix") == 0;
    }
    
    public void shortenPathDendrite(Object3DInt dendrite) {
        path.shortenPath(dendrite);
    }
    
    public void updatePath(AutoTrace2 autoTrace, VoxelInt border, Object3DInt dendrite, double calibXY, double calibZ) {
        this.path = autoTrace.getPath(border.getX(), border.getY(), (border.getZ()+1), closestDendrite.getX(), closestDendrite.getY(), (closestDendrite.getZ()+1));
        this.path.shortenPath(head);
        this.path.shortenPath(dendrite);
        path.setCalibXY(calibXY);
        path.setCalibZ(calibZ);
        computeDistances(dendrite);
    }
    
    public boolean isPointOnPath(Point3D pt){
        return path.isPointOnPath(pt);
    }

    /**
     * 
     * @param creator3D
     * @param val 
     */
    public void drawLineDendrite(ObjectCreator3D creator3D, int val) {
        if (closestDendrite != null) {
            Point3D closest = new Point3D(closestDendrite.getX(), closestDendrite.getY(), closestDendrite.getZ());
            creator3D.createLine(this.getPoint(), closest, val, 0);
        }
    }

    /**
     * Draw in this image with val value
     * @param plus
     * @param val
     */
    public void drawPathDendrite(ImagePlus plus, int val){
        if (this.path.getSize()>0){
            this.path.draw(plus, val);
        }
    }
    
    public void drawPathDendriteCalib(ImagePlus plus, int val){
        if (this.path.getSize()>0){
            this.path.drawwCalib(plus, val);
        }
    }
    
    public void erasePathInImage(ImagePlus plus){
        if (this.path != null && this.path.getSize() > 0){
            this.path.erasePathInImage(plus);
        }
    }
    
}
