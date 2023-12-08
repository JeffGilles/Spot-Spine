package spine_sources;

import spine_path.PathSpine;
import spine_path.AutoTrace2;//OLD;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.CalibrationBar;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import mcib3d.geom.Voxel3D;

public class SpineJ_ROI implements PlugIn {
    @Override
    public void run(String arg) {
        // Image
        ImagePlus plus = WindowManager.getCurrentImage();
        // Calibration
        Calibration cal0 = plus.getCalibration();
        Calibration cal = new Calibration();
        cal.pixelWidth = 1;
        cal.pixelHeight = 1;
        cal.pixelDepth = 1;
        plus.setCalibration(cal);

        // ROI
        RoiManager manager = RoiManager.getRoiManager();
        Roi roi1 = manager.getRoi(0);
        Roi roi2 = manager.getRoi(1);
        // coordinates
        double x0 = roi1.getBounds().getCenterX();
        double y0 = roi1.getBounds().getCenterY();
        double z0 = roi1.getZPosition();
        double x1 = roi2.getBounds().getCenterX();
        double y1 = roi2.getBounds().getCenterY();
        double z1 = roi2.getZPosition();

        // Path test
        AutoTrace2 autoTrace = new AutoTrace2(plus);
        IJ.log(""+x0+" "+y0+" "+z0);
        IJ.log("to "+x1+" "+y1+" "+z1);
        PathSpine path1 = autoTrace.getPath(x0, y0, z0, x1, y1, z1);
        IJ.log("roi x:"+x0+" y:"+y0+" z:"+z0+" closestx:"+x1+" closesty:"+y1+" closestz:"+z1);
        IJ.log("Path1 length : " + path1.getLength() + " (" + path1.getSize() + " points)");
        ImagePlus draw = path1.drawImageROIplugin(plus, plus.getBitDepth() == 8 ? 255 : 65535);
        plus.setCalibration(cal0);
        draw.setCalibration(cal0);
        draw.show();
        
        
        
//        //mes tests
//        Spine spi = new Spine((int) x0, (int) y0, (int) z0, 42);
//        Voxel3D end = new Voxel3D((int) x1, (int) y1, (int) z1, 6);
//        spi.closestDendrite = end;
//        AutoTrace2 autoTrace2 = new AutoTrace2(plus.duplicate());
//        spi.computePathDendrite(autoTrace2);
//        spi.path.drawImage(plus, plus.getBitDepth() == 8 ? 255 : 65535);
//        plus.updateAndDraw();
//        plus.setCalibration(cal0);
////        draw.setCalibration(cal0);
////        draw.show();
//        plus.show();
//        //ça c'est ok
    }
}
