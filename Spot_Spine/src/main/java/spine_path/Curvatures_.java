//package spine_sources;
//
//import spine_path.AutoTrace2;//OLD;
//import ij.ImagePlus;
//import ij.WindowManager;
//import ij.gui.GenericDialog;
//import ij.plugin.PlugIn;
//
//public class Curvatures_ implements PlugIn {
//    double sigma = 0.998;
//    double max = 63.5;
//
//    @Override
//    public void run(String arg) {
//        ImagePlus plus = WindowManager.getCurrentImage();
//        if (dialog()) {
//            AutoTrace2 traceTest = new AutoTrace2(plus);
//            traceTest.computeCurvatures(sigma);
//        }
//    }
//
//    private boolean dialog() {
//        GenericDialog dialogPlus = new GenericDialog("Spinej");
//        dialogPlus.addNumericField("Sigma", sigma);
//        dialogPlus.addNumericField("Maximum", max);
//
//        dialogPlus.showDialog();
//        sigma = dialogPlus.getNextNumber();
//        max = dialogPlus.getNextNumber();
//
//        return dialogPlus.wasOKed();
//    }
//}
