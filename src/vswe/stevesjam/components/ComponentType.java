package vswe.stevesjam.components;


public enum ComponentType {
    INPUT(ComponentMenuInventory.class, ComponentMenuInventory.class, ComponentMenuInventory.class, ComponentMenuInventory.class, ComponentMenuInventory.class);

    private Class<? extends ComponentMenu>[] classes;

    private ComponentType(Class<? extends ComponentMenu> ... classes) {
        this.classes = classes;
    }

    public Class<? extends ComponentMenu>[] getClasses() {
        return classes;
    }
}