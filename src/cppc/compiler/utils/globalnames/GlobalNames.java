package cppc.compiler.utils.globalnames;

public abstract interface GlobalNames
{
  public static final String CLASS_OPTION = "CPPC/Utils/GlobalNames/ClassName";
  public static final String ADD_CPPC_INIT_PRAGMA_CLASS_OPTION = "CPPC/Transforms/AddCppcInitPragma/ClassName";
  public static final String ADD_LOOP_CONTEXT_MANAGEMENT_CLASS_OPTION = "CPPC/Transforms/AddLoopContextManagement/ClassName";
  public static final String ADD_OPEN_FILES_CONTROL_CLASS_OPTION = "CPPC/Transforms/AddOpenFilesControl/ClassName";
  public static final String ADD_RESTART_JUMPS_CLASS_OPTION = "CPPC/Transforms/AddRestartJumps/ClassName";
  public static final String CODE_OPTIMIZATIONS_CLASS_OPTION = "CPPC/Transforms/CodeOptimizations/ClassName";
  public static final String COMMUNICATION_ANALYZER_CLASS_OPTION = "CPPC/Analysis/CommunicationAnalyzer/ClassName";
  public static final String EXPRESSION_ANALYZER_CLASS_OPTION = "CPPC/Analysis/ExpressionAnalyzer/ClassName";
  public static final String LANGUAGE_ANALYZER_CLASS_OPTION = "CPPC/Utils/LanguageAnalyzer/ClassName";
  public static final String LANGUAGE_TRANSFORMS_CLASS_OPTION = "CPPC/Utils/LanguageTransforms/ClassName";
  public static final String PRAGMA_DETECTION_CLASS_OPTION = "CPPC/Transforms/PragmaDetection/ClassName";
  public static final String SPECIFIER_ANALYZER_CLASS_OPTION = "CPPC/Analysis/SpecifierAnalyzer/ClassName";
  public static final String STATEMENT_ANALYZER_CLASS_OPTION = "CPPC/Analysis/StatementAnalyzer/ClassName";
  public static final String SYMBOLIC_ANALYZER_CLASS_OPTION = "CPPC/Analysis/SymbolicAnalyzer/ClassName";
  public static final String SYMBOLIC_EXPRESSION_ANALYZER_CLASS_OPTION = "CPPC/Analysis/SymbolicExpressionAnalyzer/ClassName";
  public static final String VARIABLE_SIZE_ANALYZER_CLASS_OPTION = "CPPC/Utils/VariableSizeAnalyzer/ClassName";
  
  public abstract String[] BEGIN_EXECUTE_PRAGMA();
  
  public abstract String[] CHECKPOINT_PRAGMA();
  
  public abstract String[] CHECKPOINT_LOOP_PRAGMA();
  
  public abstract String[] END_EXECUTE_PRAGMA();
  
  public abstract String[] REGISTER_PRAGMA();
  
  public abstract String[] SHUTDOWN_PRAGMA();
  
  public abstract String[] UNREGISTER_PRAGMA();
  
  public abstract String CHECKPOINT_LABEL();
  
  public abstract int CURRENT_CHKPT_CODE();
  
  public abstract String ENTER_FUNCTION_LABEL();
  
  public abstract String EXECUTE_LABEL();
  
  public abstract String EXIT_FUNCTION_LABEL();
  
  public abstract String LOOP_UNROLLING_LABEL();
  
  public abstract String REGISTER_LABEL();
  
  public abstract String UNREGISTER_LABEL();
  
  public abstract int NEXT_FILE_CODE();
  
  public abstract String ADD_LOOP_INDEX_FUNCTION();
  
  public abstract String CHECKPOINT_FUNCTION();
  
  public abstract String COMMIT_CALL_IMAGE_FUNCTION();
  
  public abstract String CONTEXT_POP_FUNCTION();
  
  public abstract String CONTEXT_PUSH_FUNCTION();
  
  public abstract String CREATE_CALL_IMAGE_FUNCTION();
  
  public abstract String INIT_CONFIGURATION_FUNCTION();
  
  public abstract String INIT_STATE_FUNCTION();
  
  public abstract String JUMP_NEXT_FUNCTION();
  
  public abstract String REGISTER_DESCRIPTOR_FUNCTION();
  
  public abstract String REGISTER_FOR_CALL_IMAGE_FUNCTION();
  
  public abstract String REGISTER_FUNCTION();
  
  public abstract String REMOVE_LOOP_INDEX_FUNCTION();
  
  public abstract String SET_LOOP_INDEX_FUNCTION();
  
  public abstract String SHUTDOWN_FUNCTION();
  
  public abstract String UNREGISTER_DESCRIPTOR_FUNCTION();
  
  public abstract String UNREGISTER_FUNCTION();
  
  public abstract String INCLUDE_FILE();
}
