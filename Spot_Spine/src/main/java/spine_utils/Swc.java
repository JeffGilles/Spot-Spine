package spine_utils;

public class Swc {
    private double x, y, z, r;
    private int id;

    public Swc(double x, double y, double z, double r, int id) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getR() {
        return r;
    }

    public int getId() {
        return id;
    }

}
