package vswe.superfactory.components;


public class ComponentMenuContainerTypesVariable extends ComponentMenuContainerTypes {
	public ComponentMenuContainerTypesVariable(FlowComponent parent) {
		super(parent);
	}

	@Override
	public boolean isVisible() {
		return getParent().getConnectionSet() == ConnectionSet.EMPTY;
	}
}
