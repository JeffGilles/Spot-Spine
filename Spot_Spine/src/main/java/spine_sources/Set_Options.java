/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package spine_sources;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;

/**
 *
 * @author jeffgilles
 */
public class Set_Options implements PlugIn {
    
    public void run(String arg) {
        String[] label1 = new String[10];
        boolean[] state1 = new boolean[label1.length];
        String[] label2 = new String[6];
        boolean[] state2 = new boolean[label2.length];
        int h = 0;
        int g = 0;
        
        //Heads
        label1[h] = "Volume (unit)";
        state1[h++] = Prefs.get("Spine3D-Options_volume.boolean", true);
        label1[h] = "Surface (unit)";
        state1[h++] = Prefs.get("Spine3D-Options_surface.boolean", true);
        label1[h] = "Compactness";
        state1[h++] = Prefs.get("Spine3D-Options_compacity.boolean", true);
        label1[h] = "Fit_Ellipse";
        state1[h++] = Prefs.get("Spine3D-Options_ellipse.boolean", true);
        label1[h] = "Intensities";
        state1[h++] = Prefs.get("Spine3D-Options_intensity.boolean", false);
//        label1[h] = "Mean_Grey_Value";
//        state1[h++] = Prefs.get("Spine3D-Options_mean.boolean", true);
//        label1[h] = "Std_Dev_Grey_Value";
//        state1[h++] = Prefs.get("Spine3D-Options_stdDev.boolean", true);
        label1[h] = "Mode_Grey_Value";
        state1[h++] = Prefs.get("Spine3D-Options_Mode.boolean", false);
        label1[h] = "Centroid";
        state1[h++] = Prefs.get("Spine3D-Options_centroid.boolean", true);
        label1[h] = "Centre_of_mass";
        state1[h++] = Prefs.get("Spine3D-Options_COM.boolean", true);
//        label1[h] = "Objects numbering";
//        state1[h++] = Prefs.get("Spine3D-Options_Numbering.boolean", false);

        //Distances
        label2[g] = "Feret (unit)";
        state2[g++] = Prefs.get("Spine3D-Options_feret.boolean", false);
        label2[g] = "Distance to surface";
        state2[g++] = Prefs.get("Spine3D-Options_dist2Surf.boolean", false);
        label2[g] = "Furthest pt from neck (unit)";
        state2[g++] = Prefs.get("Spine3D-Options_NtoH.boolean", false);
        label2[g] = "Neck length";
        state2[g++] = Prefs.get("Spine3D-Options_neckLen.boolean", false);
        label2[g] = "Furthest_Pt from dendrite (unit)";
        state2[g++] = Prefs.get("Spine3D-Options_DtoF.boolean", false);
        label2[g] = "Closest Head";
        state2[g++] = Prefs.get("Spine3D-Options_Closest.boolean", false);
        
        GenericDialog gd = new GenericDialog("Spine Set Measurements");
        gd.addMessage("Head Measurements:", Font.decode("dialog bold 14"));
//        gd.addCheckboxGroup(4, 2, label1, state1);
        gd.addCheckboxGroup(4, 2, label1, state1);
        gd.addMessage("Distance Measurements:", Font.decode("dialog bold 14"));
        gd.getMessage().setSize(60, 17);
//        gd.getMessage().setBounds(21, 201, 250, 17);
        gd.getMessage().setPreferredSize(new Dimension(130, 17));
//        IJ.log("1 x:"+gd.getMessage().getBounds().x+" y:"+gd.getMessage().getBounds().y+" w:"+gd.getMessage().getBounds().width+" h:"+gd.getMessage().getBounds().height);
        gd.addToSameRow();
        
        
//        gd.addButton(" ? ", (e) -> {
//            jButtonActionPerformed(e);
//        });
        
        gd.addCheckboxGroup(6, 1, label2, state2);
        gd.addButton("about measures", (e) -> {
            aboutActionReleased(e);
        });
        
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
//        IJ.log("2 x:"+gd.getMessage().getBounds().x+" y:"+gd.getMessage().getBounds().y+" w:"+gd.getMessage().getBounds().width+" h:"+gd.getMessage().getBounds().height);

        // analyse Head
        Prefs.set("Spine3D-Options_volume.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_surface.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_compacity.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_ellipse.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_intensity.boolean", gd.getNextBoolean());
//        Prefs.set("Spine3D-Options_mean.boolean", gd.getNextBoolean());
//        Prefs.set("Spine3D-Options_stdDev.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_Mode.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_centroid.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_COM.boolean", gd.getNextBoolean());
//        Prefs.set("Spine3D-Options_Numbering.boolean", gd.getNextBoolean());
        
        // analyse Distances
        Prefs.set("Spine3D-Options_feret.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_dist2Surf.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_NtoH.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_neckLen.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_DtoF.boolean", gd.getNextBoolean());
        Prefs.set("Spine3D-Options_Closest.boolean", gd.getNextBoolean());

    }
    
//    private void jButtonActionPerformed(java.awt.event.ActionEvent evt) {  
//        Frame_Options fram = new Frame_Options();
//        fram.setVisible(true);
//    }
    
    private void aboutActionReleased(ActionEvent evt) {                                             
        try{
            URI uri = URI.create("https://imagej.net/plugins/distance-analysis");
            Desktop.getDesktop().browse(uri);
        }
        catch (IOException ex){
            IJ.error("Open failed");
        }
    }
}
