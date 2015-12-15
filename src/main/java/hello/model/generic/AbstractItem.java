package hello.model.generic;

public abstract class AbstractItem
extends               AbstractContent {
    
    private final String name;

    public AbstractItem(Long id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[name=%s]", 
                this.getClass().getSimpleName(), name);
    }

}