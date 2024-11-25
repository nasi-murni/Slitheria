import java.util.Objects;

public class Portal{
    private final int x, y; // Portal's position
    private char id; // Unique identifier to map distinct portal pairs

    public Portal(int x, int y, char id){
        this.x = x;
        this.y = y;
        this.id = id;
    }

    // Getters
    public int getX(){ return x; }
    public int getY(){ return y; }
    public char getID() { return id; }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Portal)) return false;
        Portal portal = (Portal) o;
        return x == portal.x && y == portal.y && id == portal.id;
    }

    @Override
    public int hashCode(){
        return Objects.hash(x, y, id);
    }
}