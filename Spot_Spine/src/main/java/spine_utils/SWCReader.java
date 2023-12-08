package spine_utils;

import spine_utils.Swc;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.geom.Point3D;
import mcib3d.geom.Vector3D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SWCReader {
    private final List<Swc> listSwc = new ArrayList<>();

    public void SWCFileReader(String path) {
        File fileSWC;
        fileSWC = new File(path);
        try {
            BufferedReader inSWC = new BufferedReader(new FileReader(fileSWC));
            String lineSWC;
            String[] dataSWC;

            lineSWC = inSWC.readLine();
            while (lineSWC != null) {
                //IJ.log("reading " + lineSWC);
                if ((!lineSWC.startsWith("#")) && (lineSWC.length() != 0)) {
                    lineSWC = lineSWC.replaceAll("\t", " ");
                    dataSWC = lineSWC.split(" ");
                    int type = Integer.parseInt(dataSWC[1]);
                    double x = Double.parseDouble(dataSWC[2]);
                    double y = Double.parseDouble(dataSWC[3]);
                    double z = Double.parseDouble(dataSWC[4]);
                    double r = Double.parseDouble(dataSWC[5]);
                    listSwc.add(new Swc(x, y, z, r, type));
                    //IJ.log("adding " + x + " " + y + " " + z + " " + r);
                }
                lineSWC = inSWC.readLine();
            }
            inSWC.close();
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }
    }
    
    public boolean fileExists(String path){
        File fileSWC;
        fileSWC = new File(path);
        return fileSWC.exists();
    }

    /**
     * Draw SWC on image with val value
     * @param plus ImagePlus used for dimensions and calibration
     * @param val fill value
     * @return 
     */
    public ImagePlus drawSWC(ImagePlus plus, int val) {
        Calibration calibration = plus.getCalibration();
        double calXY = calibration.getX(1);
        double calZ = calibration.getZ(1);
        ObjectCreator3D creator3D = new ObjectCreator3D(plus.getWidth(), plus.getHeight(), plus.getStackSize());
        for (int i = 0; i < listSwc.size() - 1; i++) {
            fillSWCSphere1(creator3D, listSwc.get(i), null, calXY, calZ, val);
            fillSWCcone(creator3D, listSwc.get(i), listSwc.get(i + 1), calXY, calZ, val);
        }
        fillSWCSphere1(creator3D, listSwc.get(listSwc.size()-1), null, calXY, calZ, val);
        
        ImagePlus result = creator3D.getPlus();
        result.setCalibration(plus.getCalibration());
        return result;
    }

    private void fillSWCSphere1(ObjectCreator3D creator3D, Swc swc1, Swc swc2, double calXY, double calZ, int val) {
        // sphere 1
        creator3D.createSphere(swc1.getX() / calXY, swc1.getY() / calXY, swc1.getZ() / calZ, swc1.getR() / calXY, val, false);
        // sphere 2
        //creator3D.createSphere(swc2.getX() / calXY, swc2.getY() / calXY, swc2.getZ() / calZ, swc2.getR() / calXY, val, false);
    }

    private void fillSWCline(ObjectCreator3D creator3D, Swc swc1, Swc swc2, double calXY, double calZ, int val) {
        Point3D centre1 = new Point3D(swc1.getX() / calXY, swc1.getY() / calXY, swc1.getZ() / calZ);
        Point3D centre2 = new Point3D(swc2.getX() / calXY, swc2.getY() / calXY, swc2.getZ() / calZ);
        double radius = 0.5 * swc1.getR() / calXY + 0.5 * swc2.getR() / calXY;

        creator3D.createLine(centre1.getRoundX(), centre1.getRoundX(), centre1.getRoundZ(), centre2.getRoundX(), centre2.getRoundY(), centre2.getRoundZ(), val, (int) radius);
    }


    private void fillSWCcone(ObjectCreator3D creator3D, Swc swc1, Swc swc2, double calXY, double calZ, int val) {
        Point3D centre1 = new Point3D(swc1.getX() / calXY, swc1.getY() / calXY, swc1.getZ() / calZ);
        Point3D centre2 = new Point3D(swc2.getX() / calXY, swc2.getY() / calXY, swc2.getZ() / calZ);
        Point3D centre = new Point3D(0.5 * centre1.getX() + 0.5 * centre2.getX(), 0.5 * centre1.getY() + 0.5 * centre2.getY(), 0.5 * centre1.getZ() + 0.5 * centre2.getZ());
        Vector3D vector3D = new Vector3D(centre1, centre2);
        Vector3D perpendicular1 = vector3D.getRandomPerpendicularVector().getNormalizedVector();
        Vector3D perpendicular2 = vector3D.crossProduct(perpendicular1);
        double radius1 = swc1.getR() / calXY;
        double radius2 = swc2.getR() / calXY;
        double height = vector3D.getLength();

        creator3D.createConeAxes(centre.getRoundX(), centre.getRoundY(), centre.getRoundZ(), radius1, radius1, radius2, radius2, height, val, perpendicular1, perpendicular2);
    }
    
    public double getLength(){
        double l = 0;
        for (int i = 1; i < listSwc.size(); i++) {
            Swc swc1 = listSwc.get(i-1);
            Swc swc2 = listSwc.get(i);
            l = l + computeDistance(swc1, swc2);
        }
        return l;
    }
    
    private double computeDistance(Swc swc1, Swc swc2){
            return Math.sqrt(Math.pow(swc1.getX() - swc2.getX(), 2) + Math.pow(swc1.getY() - swc2.getY(), 2) + Math.pow(swc1.getZ() - swc2.getZ(), 2));
    }


}
