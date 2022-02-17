package videoRangeLegacy;

import java.util.ArrayList;

public class VRHorzMethods {
	
	private VRControl vrControl;
	
	private ArrayList<VRHorzCalcMethod> methods = new ArrayList<VRHorzCalcMethod>();
	
	private ArrayList<String> names = new ArrayList<String>();
	
	private int currentMethodId = METHOD_ROUND;
	
	private VRHorzCalcMethod currentMethod;
	
	public static final int METHOD_ROUND = 0;
	public static final int METHOD_REFRACTION = 1;

	public VRHorzMethods(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
		addMethod(new RoundEarthMethod(vrControl), "Round Earth Method");
		addMethod(new RefractionMethod(vrControl), "Refraction Method");
		setCurrentMethodId(METHOD_ROUND);
	}
	
	private void addMethod(VRHorzCalcMethod method, String name) {
		
		methods.add(method);
		names.add(name);
	}
	
	public VRHorzCalcMethod getCurrentMethod() {
		return currentMethod;
	}
	
	public void setCurrentMethodId(int methodId) {
		currentMethodId = methodId;
		currentMethod = methods.get(methodId);
	}

	public ArrayList<String> getNames() {
		return names;
	}
	
	public VRHorzCalcMethod getMethod(int index) {
		return methods.get(index);
	}
}
