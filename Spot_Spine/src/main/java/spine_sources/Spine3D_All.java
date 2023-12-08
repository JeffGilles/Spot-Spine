/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package spine_sources;

import ij.plugin.PlugIn;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * @author jean-francoisGilles
 */
public class Spine3D_All implements PlugIn {
    
    
    @Override
    public void run(String arg) {
        this.checkForClassInstalled();
        //this.checkForSNT();
        ComputeDendrite dendrite = new ComputeDendrite();
        dendrite.run(arg);
    }
    
    
    private void checkForClassInstalled() { //thanks to csbdresden
        try {
            Class.forName("mcib3d.image3d.ImageHandler");
            Class.forName("sc.fiji.snt.SNT");
        } catch (ClassNotFoundException var2) {
            JOptionPane.showMessageDialog((Component)null, "<html><p>Spine3D relies on <ul style='list-style-type: circle'><li>The MCIB-Core plugin from <b>3D ImageJ Suite</b></li><li>SNT plugin from <b>NeuroAnatomy</b> as libraries.</li></ul>"
                   + "Please install them as below by enabling there update sites.<br>Go to <i>Help > Update...</i>, then click on <i>Manage update sites</i></p>"
                   + "<br><br><img src='" + this.getClass().getResource("/3dsuite.png") + "' width='440' height='40'>"
                   + "<br><br><p><img src='" + this.getClass().getResource("/neuroanat.png") + "' width='440' height='40'></p>"
                   + "</html>", "Required MCIB-Core and/or SNT plugin missing", 0);
            
            throw new RuntimeException("3DSuite or NeuroAnatomy not installed");
        }
    }
    
}
