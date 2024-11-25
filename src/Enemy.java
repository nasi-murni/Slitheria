import java.util.Random;

abstract class Entity{
    protected int x, y;
    protected char symbol;

    public Entity(int x, int y, char symbol){
        this.x = x;
        this.y = y;
        this.symbol = symbol;
    }

    public abstract void update(VisualizedMap map);
}

public class Enemy extends Entity{
    private Random random = new Random();
    private int moveDelay = 0;

    public Enemy(int x, int y){
        super(x, y, 'E');
    }

    @Override
    public void update(VisualizedMap map){
        if(moveDelay++ >= 3){
            moveDelay = 0;
        }
    }
}
