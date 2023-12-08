
package spine_sources;

import mcib3d.geom2.VoxelInt;

/**
 *
 * @author jeff
 */
public class Peak3D {
    
    double x = 0;
    double y = 0;
    double z = 0;
    double v = 0;
    double d = 0;

    /**
     * 
     * @param x
     * @param y
     * @param z
     */
    public Peak3D ( double x,  double y,  double z, double v){
        this.x = x;
        this.y = y;
        this.z = z;
        this.v = v;
    }
    
    public  Peak3D ( double [] position){
        this.x = position[0];
        this.y = position[1];
        this.z = position[2];
        if(position.length>3){
            this.v = position[3];
        }
    }
    
    public double getX() {
        return this.x;
    }
    
    public double getY() {
        return this.y;
    }
    
    public double getZ() {
        return this.z;
    }
    
    public void setValue(double value) {
        this.v = value;
    }
    
    public double getValue() {
        return this.v;
    }
    
    public void setDistToDendrite(double distDendrite) {
        this.d = distDendrite;
    }
    
    public double getDistToDendrite() {
        return this.d;
    }
    
    /**
     * Compute distance from Peak to this point
     * @param x point coord
     * @param y point coord
     * @param z point coord
     * @return 
     */
    public double computeDistanceToPt(int x, int y, int z){
        int deltaZ = (int) Math.abs(this.z-z);
        if (deltaZ > 1){ //3D
            return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2) + Math.pow(this.z - z, 2));
        }
        else { //2D
            return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
        }
    }
    
    /**
     * Compute distance from Peak to this point
     * @param x point coord
     * @param y point coord
     * @param z point coord
     * @return 
     */
    public double computeDistanceToPt2D(int x, int y){
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
    }
    
    public VoxelInt getVoxel3DInt(){
        return new VoxelInt((int) x, (int) y, (int) z, (float) v);
    }
    
    /**
     * not value in
     * @return 
     */
    public double[] toArray(){
        double[] res = {x, y, z, v};
        return res;
    }
    
    public boolean isSamePeak(double[] point){
        boolean res = false;
        if (this.x == point[0] && this.y== point[1] && this.z == point[2]){
            return res = true;
        }
        return res;
    }
    
    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Peak3D other = (Peak3D) obj;
        return Double.compare(this.x, other.x) == 0 &&
               Double.compare(this.y, other.y) == 0 &&
               Double.compare(this.z, other.z) == 0;
    }
}
