package cppc.compiler.transforms.syntactic.stub.c;

import cetus.hir.Program;
import cppc.compiler.transforms.syntactic.skel.modules.CppcCheckpointTranslator;
import cppc.compiler.transforms.syntactic.skel.modules.CppcExecuteTranslator;
import cppc.compiler.transforms.syntactic.skel.modules.CppcShutdownTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.CppcNonportableFunctionMarkTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.CppcNonportableMarkTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.CppcRegisterTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.CppcUnregisterTranslator;

public class PragmaDetection
  extends cppc.compiler.transforms.syntactic.skel.PragmaDetection
{
  private PragmaDetection(Program program)
  {
    super(program);
  }
  
  protected void registerAllModules()
  {
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcCheckpointTranslator());
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcExecuteTranslator());
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcNonportableMarkTranslator());
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcNonportableFunctionMarkTranslator());
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcRegisterTranslator());
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcShutdownTranslator());
    cppc.compiler.transforms.syntactic.skel.PragmaDetection.registerModule(new CppcUnregisterTranslator());
  }
  
  public static final PragmaDetection getTransformInstance(Program program)
  {
    return new PragmaDetection(program);
  }
}
