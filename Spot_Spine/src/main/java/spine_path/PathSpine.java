package spine_path;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
//import mcib3d.geom.Object3D;
import mcib3d.geom.Point3D;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.VoxelInt;
import mcib3d.image3d.ImageHandler;
import sc.fiji.snt.Path;
import sc.fiji.snt.util.PointInImage;

/**
 * Path is Z 1-based
 * @author jeffgilles
 */
public class PathSpine extends Path {
    Path pathSpine = null;
    double shortestNeck = 0;
    double calXY = 1;
    double calZ = 1;

    public PathSpine(Path pathSpine, double calXY, double calZ) {
        this.pathSpine = pathSpine;
        this.calXY = calXY;
        this.calZ = calZ;
    }

//    public double getLengthRaw(){
//        return pathSpine.getLength();
//    }
    
    @Override
    public double getLength(){
        if(pathSpine.getLength() == calXY) return 0;
        else return pathSpine.getLength();
    }

    public int getSize(){
        if (pathSpine == null){
            return 0;
        }
        else{
            return pathSpine.getNodes().size();
        }
    }
    
    public double getCalibXY(){
        return this.calXY;
    }
    
    public double getCalibZ(){
        return this.calXY;
    }
    
    public void setCalibXY(double calibXY){
        this.calXY = calibXY;
    }
    
    public void setCalibZ(double calibZ){
        this.calZ = calibZ;
    }
    
    public List<PointInImage> getList(){
        return pathSpine.getNodes();
    }

    /**
     * Draw in a new Image with col value
     * @param plus based image
     * @param col
     * @return 
     */
    public ImagePlus drawImageROIplugin(ImagePlus plus, int col){
        // draw the nodes
        ImageHandler handler = ImageHandler.wrap(plus.duplicate());
        handler.fill(0);

        pathSpine.getNodes().stream().
                forEach(node -> handler.setPixel((int) node.getX(), (int) node.getY(), (int) node.getZ()-1, col));
        
        return handler.getImagePlus();
    }
    
    
    /**
     * Shorten Path by excluding points inside Object3DInt
     * @param dendriteObject
     */
    public void shortenPath(Object3DInt dendriteObject){
        Path newpath = pathSpine.createPath();
        pathSpine.getNodes().stream().
                forEach(node -> {
                    VoxelInt v = new VoxelInt((int) node.getX(), (int) node.getY(), (int) node.getZ()-1, dendriteObject.getLabel());
                    if(!isVoxelInObject(v, dendriteObject)){
                        newpath.addNode(node);
                    }
                });
//        IJ.log("obj="+dendriteObject.getLabel()+" avant path: "+pathSpine.getLength()+" apres: "+newpath.getLength());
        
        if (newpath != null){
            pathSpine = newpath;
        }
    }
    
    
    public void neckPath (Object3DInt obj){
//        IJ.log("test compute 0");
        int i1 = 0;
        PointInImage pt1 = this.getList().get(i1);
        VoxelInt vo1 = new VoxelInt((int) pt1.x, (int) pt1.y, (int) pt1.z-1, (int) pt1.v);

        Path newpath = pathSpine.createPath();
        long count = this.getList().stream().count();
        
        if (count > 0){
            for (int i = 0; i < count; i++){
                PointInImage pt = this.getList().get(i);
                VoxelInt vo = new VoxelInt((int) pt.x, (int) pt.y, (int) pt.z-1, (int) pt.v);
//                IJ.log(""+i+"_________x"+(int) Math.round(pt.x)+" x"+(int) Math.round(pt.y)+" z"+(int) Math.round(pt.z));//+" obj contains vox :"+obj.contains(vo));
//                if (!obj.contains(vo)){ // Eviter les vides??
//                    newpath.addNode(pt);
//                }
                if (!isVoxelInObject(vo, obj)){
                    newpath.addNode(pt);
                }
            }
        }
        if (newpath.getNodes().isEmpty()){
            newpath = null;
        }
        pathSpine = newpath;
    }
    
    /**
     * Get intersection VoxelInt between Path and Object3DInt
     * @param dendriteObject
     * @param first or last
     * @return 
     */
    public VoxelInt intersectionPointPath(Object3DInt dendriteObject, boolean first){
        Path newpath = pathSpine.createPath();
        VoxelInt vox = null;
        //select path within object
        pathSpine.getNodes().stream().
                forEach(node -> {
                    VoxelInt v = new VoxelInt((int) node.getX(), (int) node.getY(), (int) node.getZ()-1, (int) node.v);
                    if (dendriteObject.contains(v)) {
                        newpath.addNode(node);
                    }
                });
        if (first) {
            PointInImage pii = newpath.getNodes().stream().findFirst().get();
            vox = new VoxelInt((int) pii.getX(), (int) pii.getY(), (int) pii.getZ()-1, (int) pii.v);
        }
        else {
            long count = newpath.getNodes().stream().count();
            PointInImage pii = newpath.getNodes().stream().skip(count - 1).findFirst().get();
            vox = new VoxelInt((int) pii.getX(), (int) pii.getY(), (int) pii.getZ()-1, (int) pii.v);
        }
        
        return vox;
    }
    
    public boolean isPointOnPath(Point3D pt){
        PointInImage p = new PointInImage(pt.x, pt.y, pt.z);
        return pathSpine.getNodes().stream().anyMatch(n -> n==p);
    }
    
    /**
     * true if on the path
     * @param vox Voxel with Z 0-based
     * @return 
     */
    public boolean isVoxelIntOnPath(VoxelInt vox){
//        PointInImage p = new PointInImage(vox.getX()*calXY, vox.getY()*calXY, vox.getZ()*calZ);
        PointInImage p = new PointInImage(vox.getX(), vox.getY(), vox.getZ()+1);
        return pathSpine.getNodes().stream().anyMatch(n -> n==p);
    }
    
    public double computeDistanceVoxelsPxl(VoxelInt v1, VoxelInt v2){
            return Math.sqrt(Math.pow((v1.getX() - v2.getX()), 2) + Math.pow((v1.getY() - v2.getY()), 2) + Math.pow((v1.getZ() - v2.getZ()), 2));

    }
    
    public double computeDistanceVoxels(VoxelInt v1, VoxelInt v2){
            return Math.sqrt(Math.pow((v1.getX() - v2.getX())*calXY, 2) + Math.pow((v1.getY() - v2.getY())*calXY, 2) + Math.pow((v1.getZ() - v2.getZ())*calZ, 2));
//2D          return Math.sqrt(Math.pow((v1.getX() - v2.getX())*calXY, 2) + Math.pow((v1.getY() - v2.getY())*calXY, 2));
    }
    
    /**
     * Compute the max distance from vox with the contours of obj
     * @param vox begin from
     * @param obj object for contours
     * @return 
     */
    public VoxelInt maxDist (VoxelInt vox, Object3DInt obj){
        Object3DComputation oc = new Object3DComputation(obj);
        List<VoxelInt> contours = oc.getContour();
        
        contours.stream().forEach(v -> v.setExtra(computeDistanceVoxels(v, vox)));
        VoxelInt vresult = contours.stream().max(Comparator.comparing(VoxelInt::getExtra)).get();
        
        return vresult;
    }
    
    /**
     * Get path length with squared dist between each pixels on the path with calibration if pixel==false
     * @param pixel true == length in pxl (1,1,1)
     * @return 
     */
    public double MeasureLength(boolean pixel){
        double sum = 0;
        if (pathSpine!=null && pathSpine.getNodes().size() > 0){
            for (int i = 0; i < pathSpine.getNodes().size()-1; i++) {
                PointInImage p0 = pathSpine.getNodes().get(i);
                VoxelInt v0 = new VoxelInt((int) p0.getX(), (int) p0.getY(), (int) p0.getZ()-1, (int) p0.v);
                PointInImage p1 = pathSpine.getNodes().get(i+1);
                VoxelInt v1 = new VoxelInt((int) p1.getX(), (int) p1.getY(), (int) p1.getZ()-1, (int) p1.v);
                double dist;
                if (pixel){
                    dist = computeDistanceVoxelsPxl(v0, v1);
                }
                else{
                    dist = computeDistanceVoxels(v0, v1);
                }
                sum = sum + dist;
            }
        }
        if (pixel && sum == 1) sum = 0;
        else if(!pixel && sum == calXY) sum = 0;
        return sum;
    }
    
    public double getShortestNeckDistance(){
        PointInImage p0 = this.getList().get(0);
        VoxelInt v0 = new VoxelInt((int) Math.round(p0.getX()), (int) Math.round(p0.getY()), (int) Math.round(p0.getZ()-1), (int)p0.v);
        PointInImage p1 = this.getList().get(this.getList().size()-1);
        VoxelInt v1 = new VoxelInt((int) Math.round(p1.getX()), (int) Math.round(p1.getY()), (int) Math.round(p1.getZ()-1), (int)p1.v);
        return v0.distance(v1);
    }
    
    
    /**
     * Draw the nodes in this image with col value
     * @param plus
     * @param col 
     */
    public void draw(ImagePlus plus, int col){
        // draw the nodes
        ImageHandler handler = ImageHandler.wrap(plus);
        if (pathSpine.size()>0){
//            IJ.log("_"+pathSpine.size());
            pathSpine.getNodes().stream().
//                forEach(node -> handler.setPixel((int) Math.round(node.getX()/calXY), (int) Math.round(node.getY()/calXY), (int) Math.round(node.getZ()/calZ), col));
                forEach(node -> 
                        handler.setPixel( (int) Math.round(node.getX()), (int) Math.round(node.getY()), (int) Math.round(node.getZ()-1), col));
        }
    }

    public void drawwCalib(ImagePlus plus, int col){
        // draw the nodes
        ImageHandler handler = ImageHandler.wrap(plus);
        if (pathSpine.size()>0){
            pathSpine.getNodes().stream().
                forEach(node -> handler.setPixel( (int) Math.round(node.getX()), (int) Math.round(node.getY()), (int) Math.round(node.getZ()-1), col));
        }
    }
    
    /**
     * For pixel positions
     * @param plus 
     */
    public void erasePathInImage(ImagePlus plus){
        ImageHandler handler = ImageHandler.wrap(plus);
        pathSpine.getNodes().stream().
//            forEach(node -> handler.setPixel((int) Math.round(node.getX()/calXY), (int) Math.round(node.getY()/calXY), (int) Math.round((node.getZ()/calZ)-1), 0));
            forEach(node -> {
                handler.setPixel((int) Math.round(node.getX()), (int) Math.round(node.getY()), (int) Math.round((node.getZ())-1), 0);
            });

    }
    
    /**
     * use it only if path uses calibrated nodes
     * @param plus 
     */
    public void erasePathInImage2(ImagePlus plus){
        
        pathSpine.getNodes().stream().
            forEach(node -> {
                    plus.setSliceWithoutUpdate((int) Math.round(node.getZ()/calZ));
                    plus.getProcessor().set((int) Math.round(node.getX()/calXY), (int) Math.round(node.getY()/calXY), 0);
            });
        
    }
    
    private Boolean isVoxelInObject(VoxelInt vox, Object3DInt obj){
        Boolean res = false;
        if (!obj.getBoundingBox().contains(vox)) return false;
        else{
            List<VoxelInt> list = new ArrayList<>();
            obj.getObject3DPlanes().forEach(pl ->{
                list.addAll(pl.getVoxels().subList(0, pl.getVoxels().size()));
                });
            for (VoxelInt v : list) {
                if (vox.getX()==v.getX() && vox.getY()==v.getY() && vox.getZ()==v.getZ()) return true;
                
            }
        }
        return res;
    }
}
