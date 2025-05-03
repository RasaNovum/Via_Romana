package net.rasanovum.viaromana.variables;

public class VariableAccess {
    public static PlayerVariableAccessor playerVariables = new PlayerVariableAccessor();
    public static MapVariableAccessor mapVariables = new MapVariableAccessor();
    public static WorldVariableAccessor worldVariables = new WorldVariableAccessor();

    public static void initialize() {
        System.out.println("VariableAccess initialized with Fabric accessors.");
    }
}
