package spine_utils;

import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

public class swc_draw implements PlugIn {
    @Override
    public void run(String arg) {
        // Image
        ImagePlus plus = WindowManager.getCurrentImage();

        // SWC
        SWCReader reader = new SWCReader();
        FileInfo fileInfo = plus.getOriginalFileInfo();
        String fileSWC = fileInfo.directory + fileInfo.fileName.replace(".tif", ".swc");
        reader.SWCFileReader(fileSWC);
        ImagePlus dendrite = reader.drawSWC(plus.duplicate(), 255);
        dendrite.show();
    }
}
