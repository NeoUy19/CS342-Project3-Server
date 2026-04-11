package Checkers;

public class Pieces {

    public enum Color{BLACK,RED}
    private Color color;

    public Pieces(Color color) {
        this.color = color;
    }
    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

}
