package spine_path;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageStatistics;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import sc.fiji.snt.Path;
import sc.fiji.snt.SNT;
import sc.fiji.snt.analysis.RoiConverter;
import sc.fiji.snt.tracing.TracerThread;
import sc.fiji.snt.tracing.cost.Reciprocal;
import sc.fiji.snt.tracing.heuristic.Euclidean;
import sc.fiji.snt.util.ImgUtils;

public class TracerWithoutSNT_NOTUSED implements PlugIn {

    public static void main(String[] args) {
    }

    @Override
    public void run(String s) {
        ImagePlus imp = IJ.openImage("/home/thomas/ownCloud/PROG/SPINEJ/LineGraph.tif");
        int channel = imp.getC();
        int frame = imp.getFrame();
        Calibration calibration = imp.getCalibration();
        ImageStatistics imgStats = imp.getStatistics(ImageStatistics.MIN_MAX);
        double min = imgStats.min;
        double max = imgStats.max;
        // convert ImagePlus to RAI
        Context context = (Context) IJ.runPlugIn("org.scijava.Context", "");
        ConvertService convertService = context.service(ConvertService.class);
        Dataset dataset = convertService.convert(imp, Dataset.class);
        RandomAccessibleInterval<UnsignedByteType> ctSlice3d = ImgUtils.getCtSlice3d(dataset, channel - 1, frame - 1);

        TracerThread tt = new TracerThread(
                (RandomAccessibleInterval<? extends RealType<?>>) ctSlice3d, calibration, // RAI, image calibration
                217, 265, 1, 331, 41, 1, //start and goal coordinates
                -1, 100, // timeout and report time
                SNT.SearchImageType.ARRAY, new Reciprocal(min, max), new Euclidean(calibration) // Search type, cost function and  heuristic
        );
        tt.run();
        Path path = tt.getResult();
        new RoiConverter(path, imp).convertPaths();
        imp = imp.duplicate();
        imp.show();
    }


}