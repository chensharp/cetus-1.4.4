package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;
import cetus.hir.PointerSpecifier;
import cetus.hir.Specifier;

//import cppc.compiler.fortran.ComplexSpecifier;
//import cppc.compiler.fortran.DoubleComplexSpecifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class TypeManager
{
  private static HashMap<List<Specifier>, DataType> types = new HashMap();
  
  static
  {
    List<Specifier> specs = new ArrayList(1);
    specs.add(Specifier.CHAR);
    types.put(specs, new BasicDataType(new Identifier("CPPC_CHAR")));
    
    specs = new ArrayList(2);
    specs.add(Specifier.UNSIGNED);
    specs.add(Specifier.CHAR);
    types.put(specs, new BasicDataType(new Identifier("CPPC_UCHAR")));
    
    specs = new ArrayList(1);
    specs.add(Specifier.SHORT);
    types.put(specs, new BasicDataType(new Identifier("CPPC_SHORT")));
    
    specs = new ArrayList(2);
    specs.add(Specifier.UNSIGNED);
    specs.add(Specifier.SHORT);
    types.put(specs, new BasicDataType(new Identifier("CPPC_USHORT")));
    
    specs = new ArrayList(1);
    specs.add(Specifier.INT);
    types.put(specs, new BasicDataType(new Identifier("CPPC_INT")));
    
    specs = new ArrayList(2);
    specs.add(Specifier.UNSIGNED);
    specs.add(Specifier.INT);
    types.put(specs, new BasicDataType(new Identifier("CPPC_UINT")));
    
    specs = new ArrayList(1);
    specs.add(Specifier.LONG);
    types.put(specs, new BasicDataType(new Identifier("CPPC_LONG")));
    
    specs = new ArrayList(2);
    specs.add(Specifier.UNSIGNED);
    specs.add(Specifier.LONG);
    types.put(specs, new BasicDataType(new Identifier("CPPC_ULONG")));
    
    specs = new ArrayList(1);
    specs.add(Specifier.FLOAT);
    types.put(specs, new BasicDataType(new Identifier("CPPC_FLOAT")));
    
    specs = new ArrayList(1);
    specs.add(Specifier.DOUBLE);
    types.put(specs, new BasicDataType(new Identifier("CPPC_DOUBLE")));
    
    specs = new ArrayList(1);
    specs.add(Specifier.BOOL);
    types.put(specs, new BasicDataType(new Identifier("CPPC_UCHAR")));
    
    //specs = new ArrayList(1);
    //specs.add(ComplexSpecifier.instance());
    //types.put(specs, new ComplexDataType(new Identifier("CPPC_FLOAT")));
    
    //specs = new ArrayList(1);
    //specs.add(DoubleComplexSpecifier.instance());
    //types.put(specs, new ComplexDataType(new Identifier("CPPC_DOUBLE")));
  }
  
  public static final boolean addType(List<Specifier> specs, DataType type)
  {
    if (types.containsKey(specs)) {
      return false;
    }
    types.put(specs, type);
    
    return true;
  }
  
  public static final boolean isBasicType(List<Specifier> specs)
  {
    if (!isRegistered(specs)) {
      return false;
    }
    DataType type = (DataType)types.get(specs);
    return type instanceof BasicDataType;
  }
  
  public static final boolean isRegistered(List<Specifier> specs)
  {
    List<Specifier> copy = new ArrayList(specs.size());
    for (Specifier s : specs) {
      if (!(s instanceof PointerSpecifier)) {
        copy.add(s);
      }
    }
    return types.containsKey(copy);
  }
  
  public static final DataType getType(List<Specifier> specs)
  {
    List<Specifier> copy = new ArrayList(specs.size());
    for (Specifier s : specs) {
      if ((!(s instanceof PointerSpecifier)) && 
        (s != Specifier.CONST)) {
        copy.add(s);
      }
    }
    return (DataType)types.get(copy);
  }
}
