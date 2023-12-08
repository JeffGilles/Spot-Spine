package spine_path;

import features.ComputeCurvatures;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageStatistics;
import mcib3d.geom.Voxel3D;
import org.scijava.Context;
import sc.fiji.snt.Path;
import sc.fiji.snt.SNT;
import sc.fiji.snt.SNTService;
import sc.fiji.snt.SNTUtils;
import sc.fiji.snt.util.PointInImage;

public class AutoTrace2OLD {
    ImagePlus imp;
    private final double stackMax;
    private final double pixelWidth;
    private final double pixelHeight;
    private final double pixelDepth;
    private final boolean singleSlice; // Is the image 2D?
    private ComputeCurvatures curvatures;
    double maximum = 63.5;

    public AutoTrace2OLD(ImagePlus imp) {
        this.imp = imp;
        final ImageStatistics stats = imp.getStatistics();
        stackMax = stats.max;
        singleSlice = imp.getNSlices() == 1;
        final Calibration calibration = imp.getCalibration();
        pixelWidth = calibration.pixelWidth;
        pixelHeight = calibration.pixelHeight;
        pixelDepth = calibration.pixelDepth;
    }

    public void computeCurvatures(double sigma) {
        IJ.log("Computing curvatures...");
        curvatures = new ComputeCurvatures(imp, sigma, null, true);
        // curvatures.run();
        if(imp.getNSlices()==1) {
            ComputeCurvatures.FloatArray data = curvatures.ImageToFloatArray(this.imp.getProcessor());
            data = curvatures.computeGaussianFastMirror((ComputeCurvatures.FloatArray2D) data, (float) sigma, null, null);
            curvatures.FloatArrayToImagePlus((ComputeCurvatures.FloatArray2D) data, "curvatures", 0, 255).show();
        }
        else {
            ComputeCurvatures.FloatArray data = curvatures.StackToFloatArray(this.imp.getStack());
            data = curvatures.computeGaussianFastMirror((ComputeCurvatures.FloatArray3D) data, (float) sigma, null, null);
            curvatures.FloatArrayToStack((ComputeCurvatures.FloatArray3D) data, "curvatures", 0, 255).show();
        }
    }

    // pixel coordinates base Z 1
    public PathSpine getPath(double x0, double y0, double z0, double x1, double y1, double z1) {
        // Start and end point to find a path between
        PointInImage source = new PointInImage(x0, y0, z0);
        PointInImage target = new PointInImage(x1, y1, z1);
        
//        IJ.log("0 in getPathBase1: x:"+x0+" y:"+y0+" z:"+z0+" v:"+imp.getStack().getVoxel((int)x0, (int)y0, (int)z0-1)+" closestx:"+x1+" closesty:"+y1+" closestz:"+z1+" v:"+imp.getStack().getVoxel((int)x1, (int)y1, (int)z1-1));
        Path result = traceWithoutPlugin(source, target);
        
        // System.out.println("Path length = " + result.getLength() + " " + result.getNodes().size());
        
        return new PathSpine(result, 1, 1); // calibration
    }
    
    /**
     * Compute path from Voxel3D with Zbase 0
     * @param v0 initial point (Head mostly)
     * @param v1 end point
     * @return Path without calibration
     */
    public PathSpine getPath(Voxel3D v0, Voxel3D v1){
        PointInImage source = new PointInImage(v0.x, v0.y, v0.z+1);
        PointInImage target = new PointInImage(v1.x, v1.y, v1.z+1);
        Path result = traceWithoutPlugin(source, target);
        return new PathSpine(result, 1, 1); // no calibration
    }

    private Path traceWithoutPlugin(PointInImage source, PointInImage target) {
        // With some extra work, we can bypass using the plugin
        System.out.println("Tracing with SNT");
        
        Context context =  SNTUtils.getContext();
        SNTService sntService = context.getService(SNTService.class);
        SNT snt = sntService.initialize(imp, false);  // do not startUI
        snt.setUseSubVolumeStats(true);
        
        return snt.autoTrace(source, target, null, true);
    }
}
